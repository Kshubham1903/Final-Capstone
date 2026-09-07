package com.edupilot.service;

import com.edupilot.dto.StudentStateResponse;
import com.edupilot.model.*;
import com.edupilot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StudentStateService {

    @Autowired
    private StudentService studentService;

    @Autowired
    private LearningPreferenceQuestionnaireRepository preferenceRepository;

    @Autowired
    private WellBeingLearningContextRepository wellBeingRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    @Autowired
    private KnowledgeProfileRepository knowledgeProfileRepository;

    /**
     * Compute and assemble Student State Vector [K, P, E, W].
     */
    public StudentStateResponse getStudentState(String userId) {
        String resolvedUserId = studentService.resolveUserId(userId);
        StudentProfile profile = studentService.findOrCreateProfile(resolvedUserId);

        Map<String, Object> knowledgeMap = computeKnowledgeModel(resolvedUserId, profile);
        Map<String, Double> preferenceMap = computePreferenceScores(resolvedUserId, profile);
        Double engagementScore = computeEngagementScore(profile);
        Map<String, Object> wellbeingMap = computeWellbeingReadiness(resolvedUserId);

        return new StudentStateResponse(
                resolvedUserId,
                knowledgeMap,
                preferenceMap,
                engagementScore,
                wellbeingMap
        );
    }

    /**
     * Task 1: Preference Score Calculation (Form B)
     * Maps 20 Likert items (B1.1–B5.4) to 5 normalized dimensions [0.0 - 1.0].
     */
    public Map<String, Double> computePreferenceScores(String userId, StudentProfile profile) {
        Map<String, Double> pScores = new LinkedHashMap<>();

        Optional<LearningPreferenceQuestionnaire> prefOpt = preferenceRepository.findByUserId(userId);

        if (prefOpt.isPresent() && prefOpt.get().getResponses() != null && !prefOpt.get().getResponses().isEmpty()) {
            Map<String, Integer> res = prefOpt.get().getResponses();

            double visual = calculateSectionScore(res, "B1", 4);
            double readingVerbal = calculateSectionScore(res, "B2", 4);
            double practicalKinesthetic = calculateSectionScore(res, "B3", 4);
            double sequentialGlobal = calculateSectionScore(res, "B4", 4);
            double feedbackPractice = calculateSectionScore(res, "B5", 4);

            pScores.put("visual", round2(visual));
            pScores.put("readingVerbal", round2(readingVerbal));
            pScores.put("practicalKinesthetic", round2(practicalKinesthetic));
            pScores.put("sequentialGlobal", round2(sequentialGlobal));
            pScores.put("feedbackPractice", round2(feedbackPractice));
        } else {
            String style = profile != null && profile.getLearningStyle() != null ? profile.getLearningStyle().toLowerCase() : "visual";
            pScores.put("visual", style.contains("visual") ? 0.85 : 0.60);
            pScores.put("readingVerbal", style.contains("reading") || style.contains("verbal") ? 0.85 : 0.50);
            pScores.put("practicalKinesthetic", style.contains("practical") || style.contains("kinesthetic") ? 0.85 : 0.65);
            pScores.put("sequentialGlobal", 0.75);
            pScores.put("feedbackPractice", 0.70);
        }

        return pScores;
    }

    private double calculateSectionScore(Map<String, Integer> responses, String prefix, int count) {
        int sum = 0;
        int validItems = 0;
        for (int i = 1; i <= count; i++) {
            String key = prefix + "." + i;
            if (responses.containsKey(key) && responses.get(key) != null) {
                sum += responses.get(key);
                validItems++;
            }
        }
        if (validItems == 0) return 0.50;
        double normalized = (sum - validItems) / (double) (validItems * 4);
        return Math.max(0.0, Math.min(1.0, normalized));
    }

    /**
     * Task 2: Well-being & Learning Readiness Score (Form C)
     * Combines motivation, focus, confidence, satisfaction, workload comfort minus stress & fatigue.
     */
    public Map<String, Object> computeWellbeingReadiness(String userId) {
        Map<String, Object> wMap = new LinkedHashMap<>();

        Optional<WellBeingLearningContext> wbOpt = wellBeingRepository.findLatestByUserId(userId);

        if (wbOpt.isPresent() && wbOpt.get().isConsent() && wbOpt.get().getMotivation() != null) {
            WellBeingLearningContext wb = wbOpt.get();

            int motivation = wb.getMotivation() != null ? wb.getMotivation() : 3;
            int focus = wb.getFocus() != null ? wb.getFocus() : 3;
            int confidence = wb.getCurrentLearningConfidence() != null ? wb.getCurrentLearningConfidence() : 3;
            int workloadComfort = wb.getWorkloadComfort() != null ? wb.getWorkloadComfort() : 3;
            int satisfaction = wb.getLearningSatisfaction() != null ? wb.getLearningSatisfaction() : 3;

            int fatigue = wb.getMentalFatigue() != null ? wb.getMentalFatigue() : 3;
            int stress = wb.getAcademicStress() != null ? wb.getAcademicStress() : 3;

            int invertedFatigue = 6 - fatigue;
            int invertedStress = 6 - stress;

            int totalPositivePoints = motivation + focus + confidence + workloadComfort + satisfaction + invertedFatigue + invertedStress;
            double readinessScore = (totalPositivePoints - 7.0) / 28.0;
            readinessScore = Math.max(0.0, Math.min(1.0, readinessScore));

            wMap.put("readiness", round2(readinessScore));
            wMap.put("motivation", round2((motivation - 1.0) / 4.0));
            wMap.put("focus", round2((focus - 1.0) / 4.0));
            wMap.put("confidence", round2((confidence - 1.0) / 4.0));
            wMap.put("workloadComfort", round2((workloadComfort - 1.0) / 4.0));
            wMap.put("satisfaction", round2((satisfaction - 1.0) / 4.0));
            wMap.put("stress", round2((stress - 1.0) / 4.0));
            wMap.put("fatigue", round2((fatigue - 1.0) / 4.0));
            wMap.put("consentGranted", true);
        } else {
            wMap.put("readiness", 0.70);
            wMap.put("motivation", 0.75);
            wMap.put("focus", 0.70);
            wMap.put("confidence", 0.75);
            wMap.put("workloadComfort", 0.65);
            wMap.put("satisfaction", 0.70);
            wMap.put("stress", 0.35);
            wMap.put("fatigue", 0.30);
            wMap.put("consentGranted", false);
        }

        return wMap;
    }

    /**
     * Task 3: Engagement Score Calculation (E)
     * Combines streak, completed quizzes, and preferred study hours into normalized 0.0 - 1.0 score.
     */
    public Double computeEngagementScore(StudentProfile profile) {
        if (profile == null) return 0.50;

        int streak = profile.getCurrentStreakCount();
        int completedQuizzes = profile.getCompletedQuizzesCount();
        double studyHours = profile.getPreferredStudyHoursPerDay();

        double streakScore = Math.min(streak / 7.0, 1.0);
        double quizScore = Math.min(completedQuizzes / 10.0, 1.0);
        double hoursScore = Math.min(studyHours / 4.0, 1.0);

        double compositeEngagement = (0.40 * streakScore) + (0.40 * quizScore) + (0.20 * hoursScore);
        return round2(Math.max(0.0, Math.min(1.0, compositeEngagement)));
    }

    /**
     * Task 4: Knowledge Model Extraction (K)
     */
    public Map<String, Object> computeKnowledgeModel(String userId, StudentProfile profile) {
        Map<String, Object> kMap = new LinkedHashMap<>();

        List<ConceptMastery> cmList = conceptMasteryRepository.findByUserId(userId);
        Map<String, Double> topicMasteryScores = new LinkedHashMap<>();
        Map<String, String> topicStatuses = new LinkedHashMap<>();

        if (cmList != null && !cmList.isEmpty()) {
            for (ConceptMastery cm : cmList) {
                if (cm.getTopic() != null && !cm.getTopic().isBlank()) {
                    double normAccuracy = round2(cm.getAccuracy() / 100.0);
                    topicMasteryScores.put(cm.getTopic(), normAccuracy);
                    topicStatuses.put(cm.getTopic(), cm.getStatus() != null ? cm.getStatus().name() : "UNCERTAIN");
                }
            }
        }

        if (topicMasteryScores.isEmpty() && profile != null && profile.getConceptMastery() != null) {
            for (Map.Entry<String, Double> entry : profile.getConceptMastery().entrySet()) {
                topicMasteryScores.put(entry.getKey(), round2(entry.getValue() / 100.0));
                topicStatuses.put(entry.getKey(), "UNCERTAIN");
            }
        }

        Optional<KnowledgeProfile> kpOpt = knowledgeProfileRepository.findByUserId(userId);

        kMap.put("topicMastery", topicMasteryScores);
        kMap.put("topicStatuses", topicStatuses);
        kMap.put("learningHealthScore", kpOpt.isPresent() ? round2(kpOpt.get().getLearningHealthScore() / 100.0) : 0.70);
        kMap.put("masteredCount", kpOpt.isPresent() ? kpOpt.get().getMasteredCount() : 0);
        kMap.put("weakConcepts", profile != null && profile.getWeakConcepts() != null ? profile.getWeakConcepts() : Collections.emptyMap());

        return kMap;
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
