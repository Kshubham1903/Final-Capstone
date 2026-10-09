package com.edupilot.service;

import com.edupilot.model.StudentProfile;
import com.edupilot.repository.StudentProfileRepository;
import com.edupilot.security.CryptoUtils;
import com.edupilot.service.llm.GeminiProvider;
import com.edupilot.service.llm.GroqProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "EDUPILOT_CREDENTIAL_ENCRYPTION_KEY=TestSecretEncryptionKeyForJUnitTesting32B!",
    "llm.gemini.api-key=global_fallback_gemini_key_never_used_for_students",
    "llm.groq.api-key=gsk_global_fallback_test_key_12345"
})
public class StudentGeminiKeyIntegrationTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private CryptoUtils cryptoUtils;

    @Autowired
    private GeminiProvider geminiProvider;

    @Autowired
    private GroqProvider groqProvider;

    @Autowired
    private com.edupilot.controller.StudentController studentController;

    private String studentAId;
    private String studentBId;

    @BeforeEach
    public void setup() {
        studentAId = "test_gemini_student_a_" + UUID.randomUUID().toString().substring(0, 8);
        studentBId = "test_gemini_student_b_" + UUID.randomUUID().toString().substring(0, 8);

        studentService.findOrCreateProfile(studentAId);
        studentService.findOrCreateProfile(studentBId);
    }

    @Test
    public void testSavingGeminiKeyEncryptsAndIsDecrypted() {
        String keyA = "AIzaSy_student_A_custom_gemini_key_999";

        Map<String, Object> payload = new HashMap<>();
        payload.put("geminiApiKey", keyA);

        StudentProfile updatedProfile = studentService.updateProfileAndRecalculate(studentAId, payload);
        assertNotNull(updatedProfile.getGeminiApiKey());
        assertNotEquals(keyA, updatedProfile.getGeminiApiKey());
        assertTrue(updatedProfile.isGeminiApiKeyConfigured());

        String decryptedKey = studentService.getDecryptedGeminiApiKey(studentAId);
        assertEquals(keyA, decryptedKey);
    }

    @Test
    public void testGeminiKeyIsNotExposedInJsonSerialization() throws Exception {
        String keyA = "AIzaSy_student_A_secret_key_123";
        studentService.updateProfileAndRecalculate(studentAId, Map.of("geminiApiKey", keyA));

        StudentProfile profile = studentService.getProfileByUserId(studentAId);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(profile);

        assertFalse(json.contains(keyA), "Plaintext Gemini key must NEVER be serialized into JSON REST response!");
        assertFalse(json.contains(profile.getGeminiApiKey()), "Encrypted Gemini key must NEVER be serialized into JSON REST response!");
        assertTrue(json.contains("geminiApiKeyConfigured"), "Boolean status geminiApiKeyConfigured should be present in JSON!");
    }

    @Test
    public void testRemovingGeminiKeyReturnsNullAndBlocksGemini() {
        String keyA = "AIzaSy_student_A_custom_key_888";
        Map<String, Object> setPayload = Map.of("geminiApiKey", keyA);
        studentService.updateProfileAndRecalculate(studentAId, setPayload);

        assertEquals(keyA, studentService.getDecryptedGeminiApiKey(studentAId));

        Map<String, Object> removePayload = Map.of("geminiApiKey", "");
        StudentProfile clearedProfile = studentService.updateProfileAndRecalculate(studentAId, removePayload);

        assertNull(clearedProfile.getGeminiApiKey());
        assertFalse(clearedProfile.isGeminiApiKeyConfigured());
        assertNull(studentService.getDecryptedGeminiApiKey(studentAId));
    }

    @Test
    public void testOmittedGeminiApiKeyFieldDoesNotClearExistingKey() {
        String keyA = "AIzaSy_student_A_custom_key_777";
        Map<String, Object> setPayload = Map.of("geminiApiKey", keyA);
        studentService.updateProfileAndRecalculate(studentAId, setPayload);

        Map<String, Object> updateOtherFieldPayload = Map.of("fullName", "Updated Gemini Student A");
        StudentProfile updatedProfile = studentService.updateProfileAndRecalculate(studentAId, updateOtherFieldPayload);

        assertEquals("Updated Gemini Student A", updatedProfile.getFullName());
        assertTrue(updatedProfile.isGeminiApiKeyConfigured());
        assertEquals(keyA, studentService.getDecryptedGeminiApiKey(studentAId));
    }

    @Test
    public void testCrossUserGeminiKeyIsolation() throws Exception {
        String keyA = "AIzaSy_student_A_key_AAA";
        String keyB = "AIzaSy_student_B_key_BBB";

        studentService.updateProfileAndRecalculate(studentAId, Map.of("geminiApiKey", keyA));
        studentService.updateProfileAndRecalculate(studentBId, Map.of("geminiApiKey", keyB));

        String resolvedKeyA = (String) ReflectionTestUtils.invokeMethod(geminiProvider, "resolveEffectiveApiKey", Map.of("userId", studentAId));
        String resolvedKeyB = (String) ReflectionTestUtils.invokeMethod(geminiProvider, "resolveEffectiveApiKey", Map.of("userId", studentBId));
        String resolvedKeyNoUser = (String) ReflectionTestUtils.invokeMethod(geminiProvider, "resolveEffectiveApiKey", Map.of());

        assertEquals(keyA, resolvedKeyA, "Student A must resolve Student A's Gemini key");
        assertEquals(keyB, resolvedKeyB, "Student B must resolve Student B's Gemini key");
        assertNotEquals(resolvedKeyA, resolvedKeyB, "Student A and Student B must have isolated keys");
        assertNull(resolvedKeyNoUser, "Global Gemini API key must NOT be used as a student fallback!");
    }

    @Test
    public void testUnconfiguredStudentGeminiRequestReturnsUnauthenticatedError() {
        String response = geminiProvider.generateResponse("System", "Prompt", Map.of("userId", studentBId));
        assertTrue(response.contains("UNAUTHENTICATED"), "Missing student key must produce UNAAUTHENTICATED error");
        assertTrue(response.contains("Personal Gemini API key is required"), "Missing key error message must clearly state key is required");
    }

    @Test
    public void testGeminiProviderDoesNotFallbackToGlobalKey() throws Exception {
        String resolvedKey = (String) ReflectionTestUtils.invokeMethod(geminiProvider, "resolveEffectiveApiKey", Map.of("userId", studentBId));
        assertNull(resolvedKey, "Student without Gemini key must resolve null key (NO global key fallback)");
    }

    @Test
    public void testCandidateModelsListIncludesFallbackModels() throws Exception {
        java.util.List<String> models = (java.util.List<String>) ReflectionTestUtils.invokeMethod(geminiProvider, "getCandidateModels", "gemini-flash-latest");
        assertNotNull(models);
        assertTrue(models.contains("gemini-flash-latest"));
        assertTrue(models.contains("gemini-2.5-flash"));
        assertTrue(models.contains("gemini-2.5-flash-lite"));
        assertTrue(models.contains("gemini-3.5-flash"));
    }

    @Test
    public void testProfileUpdateSecurityOwnershipEnforcementForGeminiKey() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        // Cross-user update attempt (Student B trying to modify Student A's key) -> 403
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(studentBId, null, java.util.List.of())
        );
        org.springframework.http.ResponseEntity<?> respB = studentController.updateProfileAndRecalculate(studentAId, Map.of("geminiApiKey", "AIzaSy_hacker_key"));
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, respB.getStatusCode(), "Cross-student key update must return 403 FORBIDDEN");

        // Own profile update (Student A modifying Student A's key) -> 200 OK
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(studentAId, null, java.util.List.of())
        );
        org.springframework.http.ResponseEntity<?> respA = studentController.updateProfileAndRecalculate(studentAId, Map.of("geminiApiKey", "AIzaSy_valid_own_key"));
        assertEquals(org.springframework.http.HttpStatus.OK, respA.getStatusCode(), "Own profile key update must return 200 OK");

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    public void testExistingGroqBehaviorIsUnaffected() {
        String groqKey = "gsk_student_A_groq_key_test_123";
        studentService.updateProfileAndRecalculate(studentAId, Map.of("groqApiKey", groqKey));

        String resolvedGroqKey = (String) ReflectionTestUtils.invokeMethod(groqProvider, "resolveEffectiveApiKey", Map.of("userId", studentAId));
        assertEquals(groqKey, resolvedGroqKey, "Existing Groq key resolution must remain completely unaffected");
    }

    @Test
    public void testModelFallbackSuccessAfter404WithSameStudentKey() throws Exception {
        String studentKey = "AIzaSy_student_A_custom_key_404_test";
        studentService.updateProfileAndRecalculate(studentAId, Map.of("geminiApiKey", studentKey));

        org.springframework.web.client.RestTemplate restTemplate =
                (org.springframework.web.client.RestTemplate) ReflectionTestUtils.getField(geminiProvider, "restTemplate");

        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.createServer(restTemplate);

        String initialUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + studentKey;
        String fallbackUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + studentKey;

        // 1. Initial request returns 404 NOT_FOUND
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(initialUrl))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(org.springframework.http.HttpMethod.POST))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.NOT_FOUND)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"code\": 404, \"message\": \"models/gemini-flash-latest is not found\"}}"));

        // 2. Fallback request uses SAME student key to gemini-2.5-flash -> 200 OK
        String successResponseBody = "{\n" +
                "  \"candidates\": [\n" +
                "    {\n" +
                "      \"content\": {\n" +
                "        \"parts\": [\n" +
                "          {\"text\": \"Hello! Response from fallback model gemini-2.5-flash.\"}\n" +
                "        ]\n" +
                "      },\n" +
                "      \"finishReason\": \"STOP\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(fallbackUrl))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(org.springframework.http.HttpMethod.POST))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(successResponseBody, org.springframework.http.MediaType.APPLICATION_JSON));

        String response = geminiProvider.generateResponse("System Prompt", "User Question", Map.of("userId", studentAId));

        mockServer.verify();
        assertNotNull(response);
        assertTrue(response.contains("fallback model gemini-2.5-flash"));
        assertFalse(response.contains("UNAUTHENTICATED"));
    }

    @Test
    public void testHttp401DoesNotTriggerModelFallback() throws Exception {
        String studentKey = "AIzaSy_invalid_student_key_401_test";
        studentService.updateProfileAndRecalculate(studentAId, Map.of("geminiApiKey", studentKey));

        org.springframework.web.client.RestTemplate restTemplate =
                (org.springframework.web.client.RestTemplate) ReflectionTestUtils.getField(geminiProvider, "restTemplate");

        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.createServer(restTemplate);

        String initialUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + studentKey;

        // Expect ONLY 1 request to initial model -> 401 UNAUTHORIZED
        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo(initialUrl))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(org.springframework.http.HttpMethod.POST))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"code\": 401, \"message\": \"API key not valid\"}}"));

        String response = geminiProvider.generateResponse("System Prompt", "User Question", Map.of("userId", studentAId));

        mockServer.verify();
        assertNotNull(response);
        assertTrue(response.contains("UNAUTHENTICATED"));
        assertTrue(response.contains("invalid or unauthorized"));
    }
}
