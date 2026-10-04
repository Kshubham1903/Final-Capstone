package com.edupilot.service;

import com.edupilot.model.ModuleType;
import com.edupilot.model.QuizQuestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class VerificationOptionRandomizerTest {

    @Test
    @DisplayName("TEST 1: Generated question with correct index 0 gets randomized to target position")
    public void testRandomizeQuestionWithCorrectIndex0() {
        QuizQuestion q = QuizQuestion.builder()
                .questionText("What is the time complexity of array random access?")
                .options(List.of("O(1) direct offset", "O(n) linear search", "O(log n) binary search", "O(n^2) quadratic"))
                .correctOptionIndex(0)
                .build();

        for (int targetPos = 0; targetPos < 4; targetPos++) {
            VerificationOptionRandomizer.RandomizedQuestionResult res =
                    VerificationOptionRandomizer.randomizeVerificationOptions(q, targetPos);

            assertEquals(targetPos, res.getDisplayedCorrectIndex());
            assertEquals("O(1) direct offset", res.getDisplayedOptions().get(targetPos));
            assertEquals(0, res.getDisplayedToOriginalMapping().get(targetPos));
            assertEquals(4, res.getDisplayedOptions().size());
            assertEquals(4, new HashSet<>(res.getDisplayedOptions()).size(), "All 4 options must be distinct");
        }
    }

    @Test
    @DisplayName("TEST 2: Generated question with correct index 1 gets randomized to target position")
    public void testRandomizeQuestionWithCorrectIndex1() {
        QuizQuestion q = QuizQuestion.builder()
                .questionText("Which structure allows fast O(1) head insertion?")
                .options(List.of("Static array", "Singly linked list", "Contiguous buffer", "Fixed vector"))
                .correctOptionIndex(1)
                .build();

        for (int targetPos = 0; targetPos < 4; targetPos++) {
            VerificationOptionRandomizer.RandomizedQuestionResult res =
                    VerificationOptionRandomizer.randomizeVerificationOptions(q, targetPos);

            assertEquals(targetPos, res.getDisplayedCorrectIndex());
            assertEquals("Singly linked list", res.getDisplayedOptions().get(targetPos));
            assertEquals(1, res.getDisplayedToOriginalMapping().get(targetPos));
            assertEquals(4, res.getDisplayedOptions().size());
            assertEquals(4, new HashSet<>(res.getDisplayedOptions()).size());
        }
    }

    @Test
    @DisplayName("TEST 3: Generated question with correct index 2 gets randomized to target position")
    public void testRandomizeQuestionWithCorrectIndex2() {
        QuizQuestion q = QuizQuestion.builder()
                .questionText("What is true about cache locality?")
                .options(List.of("Pointers are always cache friendly", "Linked lists utilize hardware prefetching best", "Arrays have superior spatial locality", "Memory layout has no impact on speed"))
                .correctOptionIndex(2)
                .build();

        for (int targetPos = 0; targetPos < 4; targetPos++) {
            VerificationOptionRandomizer.RandomizedQuestionResult res =
                    VerificationOptionRandomizer.randomizeVerificationOptions(q, targetPos);

            assertEquals(targetPos, res.getDisplayedCorrectIndex());
            assertEquals("Arrays have superior spatial locality", res.getDisplayedOptions().get(targetPos));
            assertEquals(2, res.getDisplayedToOriginalMapping().get(targetPos));
            assertEquals(4, res.getDisplayedOptions().size());
            assertEquals(4, new HashSet<>(res.getDisplayedOptions()).size());
        }
    }

    @Test
    @DisplayName("TEST 4: Generated question with correct index 3 gets randomized to target position")
    public void testRandomizeQuestionWithCorrectIndex3() {
        QuizQuestion q = QuizQuestion.builder()
                .questionText("What overhead do linked list nodes incur?")
                .options(List.of("Zero pointer overhead", "Pre-allocated capacity waste", "Continuous stack allocation", "Extra pointer storage per node"))
                .correctOptionIndex(3)
                .build();

        for (int targetPos = 0; targetPos < 4; targetPos++) {
            VerificationOptionRandomizer.RandomizedQuestionResult res =
                    VerificationOptionRandomizer.randomizeVerificationOptions(q, targetPos);

            assertEquals(targetPos, res.getDisplayedCorrectIndex());
            assertEquals("Extra pointer storage per node", res.getDisplayedOptions().get(targetPos));
            assertEquals(3, res.getDisplayedToOriginalMapping().get(targetPos));
            assertEquals(4, res.getDisplayedOptions().size());
            assertEquals(4, new HashSet<>(res.getDisplayedOptions()).size());
        }
    }

    @Test
    @DisplayName("TEST 5: Displayed option mapping resolves correctly across all 4 positions")
    public void testDisplayedOptionMappingResolution() {
        QuizQuestion q = QuizQuestion.builder()
                .questionText("Sample Question")
                .options(List.of("Option 0", "Option 1", "Option 2", "Option 3"))
                .correctOptionIndex(2)
                .build();

        VerificationOptionRandomizer.RandomizedQuestionResult res =
                VerificationOptionRandomizer.randomizeVerificationOptions(q, 1); // target at position 1

        List<Integer> mapping = res.getDisplayedToOriginalMapping();
        assertEquals(4, mapping.size());

        // For each displayed position 0..3, resolving it returns the original index whose option text matches
        for (int displayedIdx = 0; displayedIdx < 4; displayedIdx++) {
            int originalIdx = VerificationOptionRandomizer.resolveDisplayedOptionIndex(displayedIdx, mapping);
            String displayedText = res.getDisplayedOptions().get(displayedIdx);
            String originalText = q.getOptions().get(originalIdx);
            assertEquals(originalText, displayedText, "Mapping from displayed index " + displayedIdx + " must resolve to original index " + originalIdx);
        }
    }

    @Test
    @DisplayName("TEST 6 & 7: Correct displayed option grades as correct, incorrect grades as incorrect")
    public void testGradingResolutionCorrectAndIncorrect() {
        QuizQuestion q = QuizQuestion.builder()
                .questionText("What is the Big-O lookup time in an array?")
                .options(List.of("O(n)", "O(log n)", "O(1)", "O(n log n)"))
                .correctOptionIndex(2) // "O(1)" is at canonical index 2
                .build();

        // Randomize so that O(1) appears at displayed position 0
        VerificationOptionRandomizer.RandomizedQuestionResult res =
                VerificationOptionRandomizer.randomizeVerificationOptions(q, 0);

        List<Integer> mapping = res.getDisplayedToOriginalMapping();

        // 1. Student selects displayed index 0 (which has O(1))
        int resolvedIdx0 = VerificationOptionRandomizer.resolveDisplayedOptionIndex(0, mapping);
        assertEquals(q.getCorrectOptionIndex(), resolvedIdx0, "Displayed index 0 must resolve to canonical correct index 2");
        boolean isCorrect0 = (resolvedIdx0 == q.getCorrectOptionIndex());
        assertTrue(isCorrect0, "Student answer at displayed index 0 must be graded as CORRECT");

        // 2. Student selects displayed index 1, 2, 3 (distractors)
        for (int distractorDisplayedIdx = 1; distractorDisplayedIdx < 4; distractorDisplayedIdx++) {
            int resolvedDistractorIdx = VerificationOptionRandomizer.resolveDisplayedOptionIndex(distractorDisplayedIdx, mapping);
            assertNotEquals(q.getCorrectOptionIndex(), resolvedDistractorIdx, "Distractor at displayed index " + distractorDisplayedIdx + " must not equal canonical correct index");
            boolean isDistractorCorrect = (resolvedDistractorIdx == q.getCorrectOptionIndex());
            assertFalse(isDistractorCorrect, "Student answer at displayed index " + distractorDisplayedIdx + " must be graded as INCORRECT");
        }
    }

    @Test
    @DisplayName("TEST 8: 10-question quiz has approximately balanced answer positions across A, B, C, D")
    public void testTenQuestionQuizBalancedDistribution() {
        List<Integer> plan = VerificationOptionRandomizer.buildBalancedAnswerPositionPlan(10);
        assertEquals(10, plan.size());

        Map<Integer, Integer> counts = new HashMap<>();
        for (int pos : plan) {
            assertTrue(pos >= 0 && pos <= 3, "Position must be in [0, 3]");
            counts.put(pos, counts.getOrDefault(pos, 0) + 1);
        }

        assertEquals(4, counts.size(), "All 4 options (0, 1, 2, 3) must appear in the 10-question plan");
        for (int opt = 0; opt < 4; opt++) {
            int count = counts.get(opt);
            assertTrue(count == 2 || count == 3, "For 10 questions, each option must appear 2 or 3 times. Option " + opt + " appeared " + count + " times.");
        }

        // Verify no run of >=3 consecutive identical positions
        for (int i = 2; i < plan.size(); i++) {
            boolean threeInARow = plan.get(i).equals(plan.get(i - 1)) && plan.get(i).equals(plan.get(i - 2));
            assertFalse(threeInARow, "No 3 consecutive identical answer positions allowed in plan: " + plan);
        }
    }

    @Test
    @DisplayName("TEST 9: Two verification sessions produce different answer-position sequences")
    public void testTwoSessionsProduceDifferentSequences() {
        boolean foundDifference = false;
        List<Integer> firstPlan = VerificationOptionRandomizer.buildBalancedAnswerPositionPlan(10);

        for (int attempt = 0; attempt < 10; attempt++) {
            List<Integer> secondPlan = VerificationOptionRandomizer.buildBalancedAnswerPositionPlan(10);
            if (!firstPlan.equals(secondPlan)) {
                foundDifference = true;
                break;
            }
        }
        assertTrue(foundDifference, "Multiple verification sessions must produce varying randomized answer position sequences");
    }

    @Test
    @DisplayName("Option prefix cleaning strips redundant 'Option A:', 'B.', 'Choice C:' tags")
    public void testPrefixCleaning() {
        assertEquals("Direct offset indexing", VerificationOptionRandomizer.cleanOptionText("Option A: Direct offset indexing"));
        assertEquals("Contiguous memory allocation", VerificationOptionRandomizer.cleanOptionText("A. Contiguous memory allocation"));
        assertEquals("Dynamic resizing overhead", VerificationOptionRandomizer.cleanOptionText("Choice B: Dynamic resizing overhead"));
        assertEquals("Doubly linked list", VerificationOptionRandomizer.cleanOptionText("Doubly linked list"));
        assertEquals("O(1) head insertion", VerificationOptionRandomizer.cleanOptionText("C) O(1) head insertion"));
    }
}
