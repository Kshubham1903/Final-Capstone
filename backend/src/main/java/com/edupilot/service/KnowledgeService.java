package com.edupilot.service;

import com.edupilot.dto.ConceptMasteryResponse;
import com.edupilot.dto.KnowledgeProfileResponse;
import com.edupilot.model.AssessmentResult;
import com.edupilot.model.ConceptMastery;
import com.edupilot.model.KnowledgeProfile;
import com.edupilot.model.StudentProfile;
import com.edupilot.repository.ConceptMasteryRepository;
import com.edupilot.repository.KnowledgeProfileRepository;
import com.edupilot.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    @Autowired
    private ConceptMasteryRepository conceptRepository;

    @Autowired
    private KnowledgeProfileRepository profileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private RecommendationService recommendationService;

    /**
     * Process diagnostic assessment results & update persistent Knowledge Profile.
     */
    public KnowledgeProfile processAssessmentResult(AssessmentResult result) {
        if (result == null || result.getUserId() == null) {
            return null;
        }

        String userId = result.getUserId();
        String subjectCode = result.getSubjectCode();
        String subjectName = result.getSubjectName();

        if (result.getUserAnswers() != null) {
            for (AssessmentResult.UserAnswer ans : result.getUserAnswers()) {
                String topic = ans.getTopic() != null ? ans.getTopic() : "General";
                updateSingleConceptMastery(userId, result.getStudentProfileId(), subjectCode, subjectName, topic, ans.isCorrect());
            }
        } else {
            syncKnowledgeProfileSummary(userId, subjectName);
        }

        return profileRepository.findByUserId(userId).orElse(null);
    }

    /**
     * Standardized single concept mastery update (Authoritative Source of Truth).
     */
    public ConceptMastery updateSingleConceptMastery(String userId, String studentProfileId, String subjectCode, String subjectName, String topic, boolean isCorrect) {
        if (userId == null) return null;
        String conceptName = (topic != null && !topic.isBlank()) ? topic : "General";
        String sCode = (subjectCode != null && !subjectCode.isBlank()) ? subjectCode : "CS301";
        String sName = (subjectName != null && !subjectName.isBlank()) ? subjectName : "General";

        Optional<ConceptMastery> opt = conceptRepository.findByUserIdAndSubjectCodeAndTopicAndConceptName(
                userId, sCode, conceptName, conceptName
        );

        ConceptMastery cm = opt.orElseGet(() -> {
            ConceptMastery c = new ConceptMastery();
            c.setUserId(userId);
            c.setStudentProfileId(studentProfileId);
            c.setSubjectCode(sCode);
            c.setSubjectName(sName);
            c.setTopic(conceptName);
            c.setConceptName(conceptName);
            return c;
        });

        int attempts = cm.getAttemptCount() + 1;
        int corrects = cm.getCorrectCount() + (isCorrect ? 1 : 0);
        int wrongs = attempts - corrects;

        int recentWrongs = cm.getRecentWrongAnswerCount();
        if (isCorrect) {
            recentWrongs = Math.max(0, recentWrongs - 1);
        } else {
            recentWrongs = recentWrongs + 1;
        }

        double accuracy = Math.round((corrects * 100.0 / attempts) * 10.0) / 10.0;
        
        // V1 Sample-size evidence-confidence heuristic: min(attempts / 4.0 * 100.0, 100.0)
        double confidence = Math.min(Math.round((attempts / 4.0 * 100.0) * 10.0) / 10.0, 100.0);

        // 1. MasteryLevel based STRICTLY on accuracy (observed performance):
        ConceptMastery.MasteryLevel level;
        if (accuracy >= 85.0) {
            level = ConceptMastery.MasteryLevel.MASTER;
        } else if (accuracy >= 70.0) {
            level = ConceptMastery.MasteryLevel.PROFICIENT;
        } else if (accuracy >= 50.0) {
            level = ConceptMastery.MasteryLevel.INTERMEDIATE;
        } else {
            level = ConceptMastery.MasteryLevel.BEGINNER;
        }

        // 2. ConceptStatus based on evidence/reliability state:
        ConceptMastery.ConceptStatus status;
        if (attempts == 0) {
            status = ConceptMastery.ConceptStatus.UNASSESSED;
        } else if (confidence < 50.0) { // attempts < 2 (insufficient evidence)
            status = ConceptMastery.ConceptStatus.UNCERTAIN;
        } else if (accuracy >= 70.0) {
            status = ConceptMastery.ConceptStatus.STRONG; // confidence >= 50%, accuracy >= 70%
        } else {
            status = ConceptMastery.ConceptStatus.WEAK; // confidence >= 50%, accuracy < 70%
        }

        String action;
        if (status == ConceptMastery.ConceptStatus.UNCERTAIN) {
            action = "Targeted adaptive testing required for " + conceptName + " to gather sufficient evidence.";
        } else if (status == ConceptMastery.ConceptStatus.WEAK) {
            action = "Review core foundational concepts & practice EASY problems for " + conceptName;
        } else {
            action = "Mastery achieved! Maintain velocity with periodic spaced retention.";
        }

        cm.setAttemptCount(attempts);
        cm.setCorrectCount(corrects);
        cm.setAccuracy(accuracy);
        cm.setWrongCount(wrongs);
        cm.setRecentWrongAnswerCount(recentWrongs);
        cm.setMasteryScore(accuracy);
        cm.setConfidenceScore(confidence);
        cm.setMasteryLevel(level);
        cm.setStatus(status);
        cm.setRecommendedAction(action);
        cm.setLastAssessedAt(LocalDateTime.now());

        System.out.println("[MASTERY DEBUG CALCULATION] concept=" + conceptName + ", attempts=" + attempts + ", corrects=" + corrects + ", accuracy=" + accuracy + "%, status=" + status);

        ConceptMastery savedCm = conceptRepository.save(cm);

        syncKnowledgeProfileSummary(userId, sName);

        return savedCm;
    }

    public void syncKnowledgeProfileSummary(String userId, String subjectName) {
        if (userId == null) return;

        List<ConceptMastery> allConcepts = conceptRepository.findByUserId(userId);

        int mastered = 0;
        int proficient = 0;
        int intermediate = 0;
        int beginner = 0;
        List<String> strongList = new ArrayList<>();
        List<String> weakList = new ArrayList<>();

        for (ConceptMastery cm : allConcepts) {
            if (cm.getStatus() == ConceptMastery.ConceptStatus.STRONG) {
                strongList.add(cm.getConceptName());
            } else if (cm.getStatus() == ConceptMastery.ConceptStatus.WEAK) {
                weakList.add(cm.getConceptName());
            }

            if (cm.getMasteryLevel() == ConceptMastery.MasteryLevel.MASTER) {
                mastered++;
            } else if (cm.getMasteryLevel() == ConceptMastery.MasteryLevel.PROFICIENT) {
                proficient++;
            } else if (cm.getMasteryLevel() == ConceptMastery.MasteryLevel.INTERMEDIATE) {
                intermediate++;
            } else {
                beginner++;
            }
        }

        double healthScore = 0.0;
        if (!allConcepts.isEmpty()) {
            double totalAcc = allConcepts.stream().mapToDouble(ConceptMastery::getAccuracy).sum();
            healthScore = Math.round((totalAcc / allConcepts.size()) * 10.0) / 10.0;
        }

        KnowledgeProfile kp = profileRepository.findByUserId(userId).orElseGet(() -> {
            KnowledgeProfile k = new KnowledgeProfile();
            k.setUserId(userId);
            return k;
        });

        kp.setTotalConceptsTracked(allConcepts.size());
        kp.setMasteredCount(mastered);
        kp.setProficientCount(proficient);
        kp.setIntermediateCount(intermediate);
        kp.setBeginnerCount(beginner);
        kp.setLearningHealthScore(healthScore);
        kp.setStrongConcepts(strongList);
        kp.setWeakConcepts(weakList);
        kp.setUpdatedAt(LocalDateTime.now());

        profileRepository.save(kp);

        // Sync with StudentProfile (defensive lookup: findByUserId with findById fallback)
        Optional<StudentProfile> profOpt = studentProfileRepository.findByUserId(userId);
        if (profOpt.isEmpty()) {
            profOpt = studentProfileRepository.findById(userId);
        }

        if (profOpt.isPresent()) {
            StudentProfile prof = profOpt.get();
            
            String sName = (subjectName != null && !subjectName.isBlank()) ? subjectName : "General";

            // Calculate subject-specific health score
            double subjectHealthScore = healthScore;
            List<ConceptMastery> subjectConcepts = allConcepts.stream()
                    .filter(c -> c.getSubjectName() != null && c.getSubjectName().equalsIgnoreCase(sName))
                    .collect(Collectors.toList());
            if (!subjectConcepts.isEmpty()) {
                double subjectAccSum = subjectConcepts.stream().mapToDouble(ConceptMastery::getAccuracy).sum();
                subjectHealthScore = Math.round((subjectAccSum / subjectConcepts.size()) * 10.0) / 10.0;
            }

            Map<String, List<String>> weakMap = prof.getWeakConcepts() != null ? prof.getWeakConcepts() : new HashMap<>();
            weakMap.put(sName, weakList);
            prof.setWeakConcepts(weakMap);

            Map<String, List<String>> strongMap = prof.getStrongConcepts() != null ? prof.getStrongConcepts() : new HashMap<>();
            strongMap.put(sName, strongList);
            prof.setStrongConcepts(strongMap);

            Map<String, Double> masteryMap = prof.getConceptMastery() != null ? prof.getConceptMastery() : new HashMap<>();
            masteryMap.put(sName, subjectHealthScore);
            prof.setConceptMastery(masteryMap);

            studentProfileRepository.save(prof);

            System.out.println("[PROFILE DEBUG AFTER] userId=" + userId + ", subject=" + sName + ", updatedMastery=" + subjectHealthScore + "%, strongCount=" + strongList.size() + ", weakCount=" + weakList.size());
        } else {
            System.err.println("[PROFILE DEBUG WARNING] Could not find StudentProfile for userId=" + userId + " to persist mastery summary.");
        }

        // Trigger real-time recommendation engine generation
        try {
            recommendationService.generateRecommendations(userId);
        } catch (Exception ex) {
            System.err.println("Failed to trigger recommendation generation: " + ex.getMessage());
        }
    }

    public KnowledgeProfileResponse getKnowledgeProfile(String userId) {
        KnowledgeProfile kp = profileRepository.findByUserId(userId).orElseGet(() -> {
            KnowledgeProfile k = new KnowledgeProfile();
            k.setUserId(userId);
            return k;
        });

        List<ConceptMasteryResponse> entries = conceptRepository.findByUserId(userId)
                .stream()
                .map(ConceptMasteryResponse::new)
                .collect(Collectors.toList());

        return new KnowledgeProfileResponse(kp, entries);
    }

    public List<ConceptMasteryResponse> getWeakConcepts(String userId) {
        return conceptRepository.findByUserId(userId)
                .stream()
                .filter(cm -> cm.getStatus() == ConceptMastery.ConceptStatus.WEAK || cm.getStatus() == ConceptMastery.ConceptStatus.UNCERTAIN)
                .map(ConceptMasteryResponse::new)
                .collect(Collectors.toList());
    }

    public List<ConceptMasteryResponse> getStrongConcepts(String userId) {
        return conceptRepository.findByUserId(userId)
                .stream()
                .filter(cm -> cm.getStatus() == ConceptMastery.ConceptStatus.STRONG)
                .map(ConceptMasteryResponse::new)
                .collect(Collectors.toList());
    }

    public List<ConceptMasteryResponse> getConceptMastery(String userId) {
        return conceptRepository.findByUserId(userId)
                .stream()
                .map(ConceptMasteryResponse::new)
                .collect(Collectors.toList());
    }
}
