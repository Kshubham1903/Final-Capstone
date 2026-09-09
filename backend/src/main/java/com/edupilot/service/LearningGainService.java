package com.edupilot.service;

import com.edupilot.dto.LearningGainResponse;
import com.edupilot.model.AssessmentResult;
import com.edupilot.model.ConceptMastery;
import com.edupilot.model.StudentProfile;
import com.edupilot.repository.AssessmentResultRepository;
import com.edupilot.repository.ConceptMasteryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class LearningGainService {

    @Autowired
    private StudentService studentService;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    /**
     * Computes normalized Hake's Learning Gain:
     * Learning Gain = (POST - PRE) / (1 - PRE)
     * Clamped to [0.0, 1.0]. Handles PRE = 1.0 safely without division by zero.
     */
    public static double computeGain(double pre, double post) {
        pre = Math.max(0.0, Math.min(1.0, pre));
        post = Math.max(0.0, Math.min(1.0, post));

        if (Math.abs(1.0 - pre) < 1e-6) {
            return post >= pre ? 0.0 : 0.0;
        }

        double gain = (post - pre) / (1.0 - pre);
        return Math.max(0.0, Math.min(1.0, round2(gain)));
    }

    public LearningGainResponse calculateStudentLearningGain(String userId) {
        String resolvedUserId = studentService.resolveUserId(userId);
        StudentProfile profile = studentService.findOrCreateProfile(resolvedUserId);

        Map<String, Double> preScores = extractBaselinePreScores(resolvedUserId, profile.getId());
        Map<String, Double> postScores = extractCurrentPostScores(resolvedUserId, profile);

        List<LearningGainResponse.TopicGainDTO> topicGains = new ArrayList<>();
        double sumGain = 0.0;
        int topicCount = 0;

        for (Map.Entry<String, Double> entry : preScores.entrySet()) {
            String topic = entry.getKey();
            double pre = entry.getValue();
            double post = postScores.getOrDefault(topic, pre);

            double gain = computeGain(pre, post);
            topicGains.add(new LearningGainResponse.TopicGainDTO(
                    topic,
                    round2(pre),
                    round2(post),
                    round2(gain)
            ));

            sumGain += gain;
            topicCount++;
        }

        // Aggregate overall gain as average normalized gain across baseline-assessed topics
        double overallGain = topicCount > 0 ? round2(sumGain / topicCount) : 0.0;

        return new LearningGainResponse(resolvedUserId, overallGain, topicGains);
    }

    private Map<String, Double> extractBaselinePreScores(String canonicalUserId, String profileId) {
        Map<String, Double> preMap = new LinkedHashMap<>();

        // 1. Check AssessmentResult baseline attempts
        List<AssessmentResult> results = assessmentResultRepository.findByUserId(canonicalUserId);
        if (results.isEmpty() && profileId != null) {
            results = assessmentResultRepository.findByUserId(profileId);
        }

        if (!results.isEmpty()) {
            // Sort by earliest creation date for baseline diagnostic
            results.sort(Comparator.comparing(AssessmentResult::getCreatedAt));
            for (AssessmentResult res : results) {
                if (res.getTopicBreakdown() != null && !res.getTopicBreakdown().isEmpty()) {
                    for (Map.Entry<String, Map<String, Object>> entry : res.getTopicBreakdown().entrySet()) {
                        String topic = entry.getKey();
                        Map<String, Object> data = entry.getValue();
                        double score = parseScore(data);
                        preMap.putIfAbsent(topic, score);
                    }
                } else if (res.getUserAnswers() != null && !res.getUserAnswers().isEmpty()) {
                    Map<String, int[]> counts = new HashMap<>();
                    for (AssessmentResult.UserAnswer ans : res.getUserAnswers()) {
                        if (ans.getTopic() != null) {
                            int[] c = counts.computeIfAbsent(ans.getTopic(), k -> new int[2]);
                            c[1]++; // total
                            if (ans.isCorrect()) c[0]++; // correct
                        }
                    }
                    for (Map.Entry<String, int[]> e : counts.entrySet()) {
                        double score = e.getValue()[1] > 0 ? (double) e.getValue()[0] / e.getValue()[1] : 0.5;
                        preMap.putIfAbsent(e.getKey(), score);
                    }
                }
            }
        }

        return preMap;
    }

    private Map<String, Double> extractCurrentPostScores(String canonicalUserId, StudentProfile profile) {
        Map<String, Double> postMap = new LinkedHashMap<>();

        // 1. ConceptMastery collection
        List<ConceptMastery> cmList = conceptMasteryRepository.findByUserId(canonicalUserId);
        if (cmList.isEmpty() && profile.getId() != null) {
            cmList = conceptMasteryRepository.findByUserId(profile.getId());
        }

        for (ConceptMastery cm : cmList) {
            String topic = cm.getTopic() != null && !cm.getTopic().isEmpty() ? cm.getTopic() : cm.getConceptName();
            if (topic != null) {
                double score = cm.getAccuracy() > 0 ? cm.getAccuracy() : (cm.getMasteryScore() / 100.0);
                if (score > 1.0) score /= 100.0;
                postMap.put(topic, score);
            }
        }

        // 2. Fallback to profile conceptMastery map
        if (profile.getConceptMastery() != null) {
            for (Map.Entry<String, Double> e : profile.getConceptMastery().entrySet()) {
                double score = e.getValue() > 1.0 ? e.getValue() / 100.0 : e.getValue();
                postMap.putIfAbsent(e.getKey(), score);
            }
        }

        return postMap;
    }

    private double parseScore(Map<String, Object> data) {
        if (data == null) return 0.5;
        if (data.containsKey("percentage")) {
            double p = ((Number) data.get("percentage")).doubleValue();
            return p > 1.0 ? p / 100.0 : p;
        }
        if (data.containsKey("accuracy")) {
            double a = ((Number) data.get("accuracy")).doubleValue();
            return a > 1.0 ? a / 100.0 : a;
        }
        if (data.containsKey("correct") && data.containsKey("total")) {
            double c = ((Number) data.get("correct")).doubleValue();
            double t = ((Number) data.get("total")).doubleValue();
            return t > 0 ? c / t : 0.5;
        }
        return 0.5;
    }

    private static double round2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
