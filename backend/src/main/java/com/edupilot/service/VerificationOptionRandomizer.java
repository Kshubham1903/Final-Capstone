package com.edupilot.service;

import com.edupilot.model.QuizQuestion;

import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

public class VerificationOptionRandomizer {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern OPTION_PREFIX_PATTERN = Pattern.compile("^(?:(?:Option|Choice)\\s+[A-D][:.]?\\s*|[A-D][.:)]\\s*)", Pattern.CASE_INSENSITIVE);

    public static class RandomizedQuestionResult {
        private final List<String> displayedOptions;
        private final int displayedCorrectIndex;
        private final List<Integer> displayedToOriginalMapping; // Index d -> original index

        public RandomizedQuestionResult(List<String> displayedOptions, int displayedCorrectIndex, List<Integer> displayedToOriginalMapping) {
            this.displayedOptions = Collections.unmodifiableList(displayedOptions);
            this.displayedCorrectIndex = displayedCorrectIndex;
            this.displayedToOriginalMapping = Collections.unmodifiableList(displayedToOriginalMapping);
        }

        public List<String> getDisplayedOptions() {
            return displayedOptions;
        }

        public int getDisplayedCorrectIndex() {
            return displayedCorrectIndex;
        }

        public List<Integer> getDisplayedToOriginalMapping() {
            return displayedToOriginalMapping;
        }
    }

    /**
     * Builds a balanced answer-position plan across options A(0), B(1), C(2), D(3).
     * For 10 questions: each option appears 2 or 3 times (e.g., [2, 3, 2, 3]).
     * Sequence is shuffled and post-processed to avoid >=3 consecutive runs or deterministic cycling.
     */
    public static List<Integer> buildBalancedAnswerPositionPlan(int totalQuestions) {
        if (totalQuestions <= 0) {
            return Collections.emptyList();
        }

        List<Integer> plan = new ArrayList<>(totalQuestions);
        int baseCount = totalQuestions / 4;
        int remainder = totalQuestions % 4;

        // Distribute remainder randomly across the 4 options
        List<Integer> optionBuckets = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(optionBuckets, RANDOM);
        Set<Integer> boostedOptions = new HashSet<>(optionBuckets.subList(0, remainder));

        for (int opt = 0; opt < 4; opt++) {
            int count = baseCount + (boostedOptions.contains(opt) ? 1 : 0);
            for (int k = 0; k < count; k++) {
                plan.add(opt);
            }
        }

        // Shuffle the distribution
        Collections.shuffle(plan, RANDOM);

        // Post-process to prevent runs of >=3 identical consecutive answer positions
        for (int i = 2; i < plan.size(); i++) {
            if (plan.get(i).equals(plan.get(i - 1)) && plan.get(i).equals(plan.get(i - 2))) {
                // Find a later or earlier position with a different value to swap
                boolean swapped = false;
                for (int j = i + 1; j < plan.size(); j++) {
                    if (!plan.get(j).equals(plan.get(i))) {
                        int temp = plan.get(i);
                        plan.set(i, plan.get(j));
                        plan.set(j, temp);
                        swapped = true;
                        break;
                    }
                }
                if (!swapped) {
                    for (int j = 0; j < i - 2; j++) {
                        if (!plan.get(j).equals(plan.get(i))) {
                            int temp = plan.get(i);
                            plan.set(i, plan.get(j));
                            plan.set(j, temp);
                            break;
                        }
                    }
                }
            }
        }

        return plan;
    }

    /**
     * Cleans option text by stripping redundant prefixes like "Option A: ", "B. ", "Choice C: ".
     * If the option text itself is just the label (e.g., "Option A"), it preserves it.
     */
    public static String cleanOptionText(String option) {
        if (option == null) return "";
        String trimmed = option.trim();
        String cleaned = OPTION_PREFIX_PATTERN.matcher(trimmed).replaceFirst("").trim();
        return cleaned.isEmpty() ? trimmed : cleaned;
    }

    /**
     * Randomizes the displayed option order for a question while placing the correct answer at targetDisplayedCorrectIndex.
     */
    public static RandomizedQuestionResult randomizeVerificationOptions(QuizQuestion question, int targetDisplayedCorrectIndex) {
        List<String> rawOptions = (question != null && question.getOptions() != null && !question.getOptions().isEmpty())
                ? question.getOptions()
                : List.of("Option A", "Option B", "Option C", "Option D");

        int numOptions = rawOptions.size();
        int rawCorrectIdx = (question != null) ? question.getCorrectOptionIndex() : 0;
        if (rawCorrectIdx < 0 || rawCorrectIdx >= numOptions) {
            rawCorrectIdx = 0;
        }

        int targetIndex = Math.floorMod(targetDisplayedCorrectIndex, numOptions);

        // Separate correct option from distractors
        int origCorrect = rawCorrectIdx;
        List<Integer> distractorOriginalIndices = new ArrayList<>();
        for (int i = 0; i < numOptions; i++) {
            if (i != origCorrect) {
                distractorOriginalIndices.add(i);
            }
        }

        // Shuffle distractors
        Collections.shuffle(distractorOriginalIndices, RANDOM);

        String[] displayedOptionsArr = new String[numOptions];
        Integer[] displayedToOriginalArr = new Integer[numOptions];

        // Place correct option at target index
        displayedOptionsArr[targetIndex] = cleanOptionText(rawOptions.get(origCorrect));
        displayedToOriginalArr[targetIndex] = origCorrect;

        // Place distractors at remaining indices
        int distractorPointer = 0;
        for (int d = 0; d < numOptions; d++) {
            if (d != targetIndex) {
                int origDistractorIdx = distractorOriginalIndices.get(distractorPointer++);
                displayedOptionsArr[d] = cleanOptionText(rawOptions.get(origDistractorIdx));
                displayedToOriginalArr[d] = origDistractorIdx;
            }
        }

        return new RandomizedQuestionResult(
                Arrays.asList(displayedOptionsArr),
                targetIndex,
                Arrays.asList(displayedToOriginalArr)
        );
    }

    /**
     * Resolves a student's selected displayed index (0..3) back to the canonical database option index (0..3).
     */
    public static int resolveDisplayedOptionIndex(int displayedSelectedIndex, List<Integer> displayedToOriginalMapping) {
        if (displayedToOriginalMapping != null && displayedSelectedIndex >= 0 && displayedSelectedIndex < displayedToOriginalMapping.size()) {
            Integer origIdx = displayedToOriginalMapping.get(displayedSelectedIndex);
            if (origIdx != null) {
                return origIdx;
            }
        }
        return displayedSelectedIndex;
    }
}
