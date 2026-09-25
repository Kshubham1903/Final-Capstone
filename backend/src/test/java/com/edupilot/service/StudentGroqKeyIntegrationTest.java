package com.edupilot.service;

import com.edupilot.model.StudentProfile;
import com.edupilot.repository.StudentProfileRepository;
import com.edupilot.security.CryptoUtils;
import com.edupilot.service.llm.GroqProvider;
import com.edupilot.service.llm.RoadmapGroqProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "EDUPILOT_CREDENTIAL_ENCRYPTION_KEY=TestSecretEncryptionKeyForJUnitTesting32B!",
    "llm.groq.api-key=gsk_global_fallback_test_key_12345"
})
public class StudentGroqKeyIntegrationTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private CryptoUtils cryptoUtils;

    @Autowired
    private GroqProvider groqProvider;

    @Autowired
    private RoadmapGroqProvider roadmapGroqProvider;

    private String studentAId;
    private String studentBId;

    @BeforeEach
    public void setup() {
        studentAId = "test_student_a_" + UUID.randomUUID().toString().substring(0, 8);
        studentBId = "test_student_b_" + UUID.randomUUID().toString().substring(0, 8);

        studentService.findOrCreateProfile(studentAId);
        studentService.findOrCreateProfile(studentBId);
    }

    @Test
    public void testCryptoUtilsEncryptionAndDecryption() {
        String rawKey = "gsk_personal_key_test_123456789";
        String encrypted = cryptoUtils.encrypt(rawKey);

        assertNotNull(encrypted);
        assertNotEquals(rawKey, encrypted);

        String decrypted = cryptoUtils.decrypt(encrypted);
        assertEquals(rawKey, decrypted);
    }

    @Test
    public void testSavingPersonalKeyEncryptsAndIsDecrypted() {
        String keyA = "gsk_student_A_custom_key_999";

        Map<String, Object> payload = new HashMap<>();
        payload.put("groqApiKey", keyA);

        StudentProfile updatedProfile = studentService.updateProfileAndRecalculate(studentAId, payload);
        assertNotNull(updatedProfile.getGroqApiKey());
        assertNotEquals(keyA, updatedProfile.getGroqApiKey());
        assertTrue(updatedProfile.isGroqApiKeyConfigured());

        String decryptedKey = studentService.getDecryptedGroqApiKey(studentAId);
        assertEquals(keyA, decryptedKey);
    }

    @Test
    public void testRemovingPersonalKeyReturnsNullAndBlocksGroq() {
        String keyA = "gsk_student_A_custom_key_888";
        Map<String, Object> setPayload = Map.of("groqApiKey", keyA);
        studentService.updateProfileAndRecalculate(studentAId, setPayload);

        assertEquals(keyA, studentService.getDecryptedGroqApiKey(studentAId));

        Map<String, Object> removePayload = Map.of("groqApiKey", "");
        StudentProfile clearedProfile = studentService.updateProfileAndRecalculate(studentAId, removePayload);

        assertNull(clearedProfile.getGroqApiKey());
        assertFalse(clearedProfile.isGroqApiKeyConfigured());
        assertNull(studentService.getDecryptedGroqApiKey(studentAId));
    }

    @Test
    public void testOmittedGroqApiKeyFieldDoesNotClearExistingKey() {
        String keyA = "gsk_student_A_custom_key_777";
        Map<String, Object> setPayload = Map.of("groqApiKey", keyA);
        studentService.updateProfileAndRecalculate(studentAId, setPayload);

        Map<String, Object> updateOtherFieldPayload = Map.of("fullName", "Updated Student A Name");
        StudentProfile updatedProfile = studentService.updateProfileAndRecalculate(studentAId, updateOtherFieldPayload);

        assertEquals("Updated Student A Name", updatedProfile.getFullName());
        assertTrue(updatedProfile.isGroqApiKeyConfigured());
        assertEquals(keyA, studentService.getDecryptedGroqApiKey(studentAId));
    }

    @Test
    public void testCrossUserIsolationBetweenStudentAAndStudentB() throws Exception {
        String keyA = "gsk_student_A_key_AAA";
        String keyB = "gsk_student_B_key_BBB";

        studentService.updateProfileAndRecalculate(studentAId, Map.of("groqApiKey", keyA));
        studentService.updateProfileAndRecalculate(studentBId, Map.of("groqApiKey", keyB));

        String resolvedKeyA = (String) ReflectionTestUtils.invokeMethod(groqProvider, "resolveEffectiveApiKey", Map.of("userId", studentAId));
        String resolvedKeyB = (String) ReflectionTestUtils.invokeMethod(groqProvider, "resolveEffectiveApiKey", Map.of("userId", studentBId));
        String resolvedKeyNoUser = (String) ReflectionTestUtils.invokeMethod(groqProvider, "resolveEffectiveApiKey", Map.of());

        assertEquals(keyA, resolvedKeyA);
        assertEquals(keyB, resolvedKeyB);
        assertNull(resolvedKeyNoUser, "Global GROQ_API_KEY must NOT be used when user identity/personal key is missing!");
    }

    @Test
    public void testRoadmapGroqProviderResolvesStudentKeyAndBlocksMissingKey() throws Exception {
        String keyA = "gsk_student_A_roadmap_key_111";
        studentService.updateProfileAndRecalculate(studentAId, Map.of("groqApiKey", keyA));

        String resolvedKeyA = (String) ReflectionTestUtils.invokeMethod(roadmapGroqProvider, "resolveEffectiveApiKey", Map.of("userId", studentAId));
        String resolvedKeyB = (String) ReflectionTestUtils.invokeMethod(roadmapGroqProvider, "resolveEffectiveApiKey", Map.of("userId", studentBId));

        assertEquals(keyA, resolvedKeyA);
        assertNull(resolvedKeyB, "Student B without key must NOT fall back to global key in RoadmapGroqProvider!");
    }

    @Test
    public void testUnconfiguredStudentGroqRequestReturnsUnauthenticatedError() {
        String response = groqProvider.generateResponse("System", "Prompt", Map.of("userId", studentBId));
        assertTrue(response.contains("UNAUTHENTICATED"));
        assertTrue(response.contains("Personal Groq API key is required"));
    }

    @Test
    public void testMissingEncryptionSecretThrowsControlledException() {
        CryptoUtils isolatedCryptoUtils = new CryptoUtils();
        ReflectionTestUtils.setField(isolatedCryptoUtils, "secretKeySource", "");

        IllegalStateException encryptEx = assertThrows(IllegalStateException.class, () -> {
            isolatedCryptoUtils.encrypt("gsk_test_key_xyz");
        });
        assertTrue(encryptEx.getMessage().contains("EDUPILOT_CREDENTIAL_ENCRYPTION_KEY configuration is missing"));

        IllegalStateException decryptEx = assertThrows(IllegalStateException.class, () -> {
            isolatedCryptoUtils.decrypt("QUFBQUFBQUFBQUFBQUFBQQ==");
        });
        assertTrue(decryptEx.getMessage().contains("EDUPILOT_CREDENTIAL_ENCRYPTION_KEY configuration is missing"));
    }

    @Autowired
    private com.edupilot.controller.StudentController studentController;

    @Test
    public void testGroqApiKeyCannotActAsEncryptionFallback() {
        CryptoUtils isolatedCryptoUtils = new CryptoUtils();
        ReflectionTestUtils.setField(isolatedCryptoUtils, "secretKeySource", null);

        assertThrows(IllegalStateException.class, () -> {
            isolatedCryptoUtils.encrypt("gsk_test_key_xyz");
        });
    }

    @Test
    public void testProfileUpdateSecurityOwnershipEnforcement() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        // Case C: Unauthenticated (null auth) -> 403
        org.springframework.http.ResponseEntity<?> respC = studentController.updateProfileAndRecalculate(studentAId, Map.of("fullName", "Attacker Payload"));
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, respC.getStatusCode(), "Unauthenticated request MUST return 403 FORBIDDEN");

        // Case D: Anonymous student -> 403
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("anonymous_student", null, java.util.List.of())
        );
        org.springframework.http.ResponseEntity<?> respD = studentController.updateProfileAndRecalculate(studentAId, Map.of("fullName", "Attacker Payload"));
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, respD.getStatusCode(), "Anonymous request MUST return 403 FORBIDDEN");

        // Case B: Different authenticated user (Student B attempting to update Student A) -> 403
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(studentBId, null, java.util.List.of())
        );
        org.springframework.http.ResponseEntity<?> respB = studentController.updateProfileAndRecalculate(studentAId, Map.of("fullName", "Attacker Payload"));
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, respB.getStatusCode(), "Cross-user update request MUST return 403 FORBIDDEN");

        // Case A: Own authenticated profile update (Student A updating Student A) -> 200 OK
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(studentAId, null, java.util.List.of())
        );
        org.springframework.http.ResponseEntity<?> respA = studentController.updateProfileAndRecalculate(studentAId, Map.of("fullName", "Updated Student A"));
        assertEquals(org.springframework.http.HttpStatus.OK, respA.getStatusCode(), "Own authenticated profile update MUST return 200 OK");

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
