package com.edupilot.service;

import com.edupilot.controller.QuizController;
import com.edupilot.model.QuizQuestion;
import com.edupilot.service.llm.GroqProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class QuizGenerationServiceTest {

    private QuizGenerationService quizGenerationService;
    private Method validateExplanationMethod;
    private Method parseBatchResultMethod;

    @BeforeEach
    public void setUp() throws Exception {
        quizGenerationService = new QuizGenerationService();
        validateExplanationMethod = QuizGenerationService.class.getDeclaredMethod("validateExplanationConsistency", List.class, int.class, String.class);
        validateExplanationMethod.setAccessible(true);

        parseBatchResultMethod = QuizGenerationService.class.getDeclaredMethod("parseBatchQuestionsResult", String.class, String.class, List.class);
        parseBatchResultMethod.setAccessible(true);
    }

    @Test
    public void testValidQuestionPassesValidation() {
        QuizQuestion q = QuizQuestion.builder()
                .subject("Physics")
                .concept("Refraction")
                .difficulty(QuizQuestion.Difficulty.MEDIUM)
                .questionText("What phenomenon occurs when light passes from water into air at an angle greater than the critical angle?")
                .options(List.of("Refraction", "Diffraction", "Total Internal Reflection", "Dispersion"))
                .correctOptionIndex(2)
                .conceptualExplanation("Total internal reflection occurs when light travels from a denser medium (water) to a rarer medium (air) at an angle exceeding the critical angle.")
                .build();

        String result = quizGenerationService.validateQuestionIntegrity(q, null);
        assertNull(result, "Valid question should pass validation cleanly without errors.");
    }

    @Test
    public void testDuplicateOptionsRejected() {
        QuizQuestion q = QuizQuestion.builder()
                .subject("Computer Science")
                .concept("Data Structures")
                .difficulty(QuizQuestion.Difficulty.EASY)
                .questionText("What is the time complexity of head insertion in a singly linked list?")
                .options(List.of("O(1)", "O(n)", "O(1)", "O(log n)"))
                .correctOptionIndex(0)
                .conceptualExplanation("Inserting at the head of a singly linked list is an O(1) operation because only head pointers are updated.")
                .build();

        String result = quizGenerationService.validateQuestionIntegrity(q, null);
        assertNotNull(result, "Should reject question with duplicate options.");
        assertTrue(result.contains("distinct choices") || result.contains("duplicates"), "Error message should mention duplicates.");
    }

    @Test
    public void testInvalidCorrectOptionIndexRejected() {
        QuizQuestion q = QuizQuestion.builder()
                .subject("Computer Science")
                .concept("Data Structures")
                .difficulty(QuizQuestion.Difficulty.EASY)
                .questionText("What is the time complexity of head insertion in a singly linked list?")
                .options(List.of("O(1)", "O(n)", "O(n^2)", "O(log n)"))
                .correctOptionIndex(5) // Invalid index
                .conceptualExplanation("Inserting at the head of a singly linked list is an O(1) operation because only head pointers are updated.")
                .build();

        String result = quizGenerationService.validateQuestionIntegrity(q, null);
        assertNotNull(result, "Should reject invalid correctOptionIndex.");
        assertTrue(result.contains("correctOptionIndex must explicitly be an integer between 0 and 3"), "Error message should mention correctOptionIndex bounds.");
    }

    @Test
    public void testMissingOrShortExplanationRejected() {
        QuizQuestion q = QuizQuestion.builder()
                .subject("Physics")
                .concept("Optics")
                .difficulty(QuizQuestion.Difficulty.EASY)
                .questionText("What is the speed of light in vacuum?")
                .options(List.of("3x10^8 m/s", "3x10^6 m/s", "1500 m/s", "300 m/s"))
                .correctOptionIndex(0)
                .conceptualExplanation("Too short") // Under 15 chars
                .build();

        String result = quizGenerationService.validateQuestionIntegrity(q, null);
        assertNotNull(result, "Should reject short conceptual explanation.");
        assertTrue(result.contains("too short"), "Error message should mention length requirement.");
    }

    @Test
    public void testMalformedQuestionMarkArtifactsRejected() {
        String result = quizGenerationService.validateTextIntegrity("What is the value of ? in the equation ? = m * a?", "questionText");
        assertNotNull(result, "Should reject question containing standalone '?' artifact in formula.");
        assertTrue(result.contains("malformed '?' character"), "Error message should mention malformed '?' character.");
    }

    @Test
    public void testTruncatedQuestionRejected() {
        String result = quizGenerationService.validateTextIntegrity("In a binary search tree, the maximum depth is determined by the", "questionText");
        assertNotNull(result, "Should reject incomplete/truncated sentence ending with 'the'.");
        assertTrue(result.contains("truncated or incomplete"), "Error message should mention truncation.");
    }

    @Test
    public void testObservedTotalInternalReflectionContradictionRejected() {
        QuizQuestion q = QuizQuestion.builder()
                .subject("Physics")
                .concept("Total Internal Reflection")
                .difficulty(QuizQuestion.Difficulty.MEDIUM)
                .questionText("When does total internal reflection occur?")
                .options(List.of(
                        "When light travels from an optically rarer to optically denser medium",
                        "When light travels from an optically denser to optically rarer medium",
                        "When angle of incidence is zero",
                        "When light enters vacuum"
                ))
                .correctOptionIndex(0) // Wrong choice claiming rarer to denser
                .conceptualExplanation("Total internal reflection occurs when light moves from a lower to higher refractive index medium.")
                .build();

        String result = quizGenerationService.validateQuestionIntegrity(q, null);
        assertNotNull(result, "Should reject TIR contradiction claiming lower to higher/rarer to denser.");
        assertTrue(result.contains("Total Internal Reflection requires light to travel from a denser"), "Error message should detail TIR rule.");
    }

    @Test
    public void testObservedLinkedListComplexityContradictionRejected() {
        QuizQuestion q = QuizQuestion.builder()
                .subject("Data Structures")
                .concept("Singly Linked List")
                .difficulty(QuizQuestion.Difficulty.EASY)
                .questionText("What is the time complexity of inserting a node at the head of a singly linked list?")
                .options(List.of("O(n)", "O(n log n)", "O(n^2)", "O(2^n)")) // O(1) missing, O(n) set as correct
                .correctOptionIndex(0)
                .conceptualExplanation("Inserting at the head requires traversing all n elements.")
                .build();

        String result = quizGenerationService.validateQuestionIntegrity(q, null);
        assertNotNull(result, "Should reject incorrect singly linked list head insertion complexity claim.");
        assertTrue(result.contains("Inserting at the head of a singly linked list is O(1) time complexity"), "Error message should detail linked list complexity rule.");
    }

    @Test
    public void testBatchParserDetectsDuplicateQuestions() throws Exception {
        String json = "{\n" +
                "  \"questions\": [\n" +
                "    {\n" +
                "      \"questionText\": \"What is the capital of France?\",\n" +
                "      \"options\": [\"Paris\", \"London\", \"Berlin\", \"Madrid\"],\n" +
                "      \"correctOptionIndex\": 0,\n" +
                "      \"conceptualExplanation\": \"Paris is the official capital city of France.\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"questionText\": \"What is the capital of France?\",\n" +
                "      \"options\": [\"Paris\", \"Rome\", \"Vienna\", \"Brussels\"],\n" +
                "      \"correctOptionIndex\": 0,\n" +
                "      \"conceptualExplanation\": \"Paris is the official capital city of France.\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        QuizGenerationService.QuestionBlueprintSpec spec1 = new QuizGenerationService.QuestionBlueprintSpec(1, "Geography", QuizQuestion.Difficulty.EASY);
        QuizGenerationService.QuestionBlueprintSpec spec2 = new QuizGenerationService.QuestionBlueprintSpec(2, "Geography", QuizQuestion.Difficulty.EASY);
        List<QuizGenerationService.QuestionBlueprintSpec> blueprint = List.of(spec1, spec2);

        Object batchResult = parseBatchResultMethod.invoke(quizGenerationService, json, "Geography", blueprint);
        Method isSuccessMethod = batchResult.getClass().getMethod("isSuccess");
        Method getErrorReasonMethod = batchResult.getClass().getMethod("getErrorReason");

        boolean success = (boolean) isSuccessMethod.invoke(batchResult);
        String errorReason = (String) getErrorReasonMethod.invoke(batchResult);

        assertFalse(success, "Batch parser should fail when duplicate question text is present.");
        assertNotNull(errorReason);
        assertTrue(errorReason.contains("Duplicate question text"), "Error reason should mention duplicate question text. Got: " + errorReason);
    }

    @Test
    public void testDetectsExplanationContradictingCorrectOption() throws Exception {
        List<String> options = List.of(
                "Inserting a new element at the head of the structure",
                "Deleting an element from the end of the structure",
                "Accessing an element by index",
                "Binary search"
        );
        int correctOptionIndex = 0; // Option A
        String explanation = "Deleting an element from the end of the structure takes O(1) time.";

        String result = (String) validateExplanationMethod.invoke(quizGenerationService, options, correctOptionIndex, explanation);

        assertNotNull(result, "Should detect contradiction when explanation describes option B while correctOptionIndex is 0");
        assertTrue(result.contains("describes option B"), "Error message should mention option B mismatch");
    }

    @Test
    public void testDetectsExplicitOptionLetterContradiction() throws Exception {
        List<String> options = List.of(
                "Option A Text",
                "Option B Text",
                "Option C Text",
                "Option D Text"
        );
        int correctOptionIndex = 0; // Option A
        String explanation = "Option B is correct because it provides constant time access.";

        String result = (String) validateExplanationMethod.invoke(quizGenerationService, options, correctOptionIndex, explanation);

        assertNotNull(result, "Should detect explicit Option B mention when correctOptionIndex is 0");
        assertTrue(result.contains("states Option B is correct"), "Error message should mention Option B");
    }

    @Test
    public void testPassesConsistentExplanation() throws Exception {
        List<String> options = List.of(
                "Inserting a new element at the head of the structure",
                "Deleting an element from the end of the structure",
                "Accessing an element by index",
                "Binary search"
        );
        int correctOptionIndex = 0; // Option A
        String explanation = "Inserting a new element at the head of a singly linked list takes constant time O(1).";

        String result = (String) validateExplanationMethod.invoke(quizGenerationService, options, correctOptionIndex, explanation);

        assertNull(result, "Consistent explanation should pass validation cleanly");
    }

    @Test
    public void testGroqFailureCapturesDescriptiveFallbackAndExceptionToString() throws Exception {
        GroqProvider mockGroq = mock(GroqProvider.class);
        when(mockGroq.generateResponse(anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException((String) null)); // Exception with null message

        Field groqField = QuizGenerationService.class.getDeclaredField("groqProvider");
        groqField.setAccessible(true);
        groqField.set(quizGenerationService, mockGroq);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            quizGenerationService.generate("Physics", QuizQuestion.Difficulty.MEDIUM, 5);
        });

        assertFalse(ex.getMessage().contains("Unknown error"), "Failure message should not contain unhelpful 'Unknown error'");
        assertTrue(ex.getMessage().contains("java.lang.RuntimeException") || ex.getMessage().contains("Groq API response validation failed"),
                "Failure message should contain descriptive exception representation or fallback");
    }

    @Test
    public void testQuizControllerGenerateAiQuizReturns502OnIllegalStateException() {
        QuizController controller = new QuizController();
        QuizGenerationService mockService = mock(QuizGenerationService.class);
        when(mockService.generateForStudent("s1", "Physics", 5))
                .thenThrow(new IllegalStateException("Groq API question generation failed for subject 'Physics' after 2 attempts. Last failure: Groq API response validation failed"));

        try {
            Field serviceField = QuizController.class.getDeclaredField("quizGenerationService");
            serviceField.setAccessible(true);
            serviceField.set(controller, mockService);
        } catch (Exception e) {
            fail("Failed to set mock service on controller: " + e.getMessage());
        }

        Map<String, Object> request = Map.of("studentId", "s1", "subject", "Physics", "count", 5);
        ResponseEntity<?> response = controller.generateAiQuiz(request);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Groq API question generation failed for subject 'Physics' after 2 attempts. Last failure: Groq API response validation failed", body.get("message"));
    }

    @Test
    public void testQuizControllerGenerateAiQuizSuccessUnchanged() {
        QuizController controller = new QuizController();
        QuizGenerationService mockService = mock(QuizGenerationService.class);
        QuizQuestion mockQ = QuizQuestion.builder().subject("Physics").concept("Optics").questionText("Sample Q").build();
        when(mockService.generateForStudent("s1", "Physics", 5))
                .thenReturn(List.of(mockQ));

        try {
            Field serviceField = QuizController.class.getDeclaredField("quizGenerationService");
            serviceField.setAccessible(true);
            serviceField.set(controller, mockService);
        } catch (Exception e) {
            fail("Failed to set mock service on controller: " + e.getMessage());
        }

        Map<String, Object> request = Map.of("studentId", "s1", "subject", "Physics", "count", 5);
        ResponseEntity<?> response = controller.generateAiQuiz(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("Physics", body.get("subject"));
        assertEquals(1, body.get("count"));
        assertEquals(List.of(mockQ), body.get("questions"));
    }
}
