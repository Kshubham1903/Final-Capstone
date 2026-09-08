package com.edupilot.service;

import com.edupilot.model.QuizQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QuizGenerationServiceTest {

    private QuizGenerationService quizGenerationService;
    private Method validateMethod;

    @BeforeEach
    public void setUp() throws Exception {
        quizGenerationService = new QuizGenerationService();
        validateMethod = QuizGenerationService.class.getDeclaredMethod("validateExplanationConsistency", List.class, int.class, String.class);
        validateMethod.setAccessible(true);
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
        // Contradictory explanation describing option B ("Deleting an element from the end of the structure")
        String explanation = "Deleting an element from the end of the structure takes O(1) time.";

        String result = (String) validateMethod.invoke(quizGenerationService, options, correctOptionIndex, explanation);

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

        String result = (String) validateMethod.invoke(quizGenerationService, options, correctOptionIndex, explanation);

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

        String result = (String) validateMethod.invoke(quizGenerationService, options, correctOptionIndex, explanation);

        assertNull(result, "Consistent explanation should pass validation cleanly");
    }
}
