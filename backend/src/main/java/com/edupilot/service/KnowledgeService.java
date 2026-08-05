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
                String conceptName = topic;

                Optional<ConceptMastery> opt = conceptRepository.findByUserIdAndSubjectCodeAndTopicAndConceptName(
                        userId, subjectCode, topic, conceptName
                );

                ConceptMastery cm = opt.orElseGet(() -> {
                    ConceptMastery c = new ConceptMastery();
                    c.setUserId(userId);
                    c.setStudentProfileId(result.getStudentProfileId());
                    c.setSubjectCode(subjectCode);
                    c.setSubjectName(subjectName);
                    c.setTopic(topic);
                    c.setConceptName(conceptName);
                    return c;
                });

                int attempts = cm.getAttemptCount() + 1;
                int corrects = cm.getCorrectCount() + (ans.isCorrect() ? 1 : 0);
                double accuracy = Math.round((corrects * 100.0 / attempts) * 10.0) / 10.0;
                double confidence = Math.min(Math.round((attempts * 20.0 + accuracy * 0.8) * 10.0) / 10.0, 100.0);

                ConceptMastery.MasteryLevel level;
                if (accuracy >= 85.0 && attempts >= 3) {
                    level = ConceptMastery.MasteryLevel.MASTER;
                } else if (accuracy >= 70.0) {
                    level = ConceptMastery.MasteryLevel.PROFICIENT;
                } else if (accuracy >= 50.0) {
                    level = ConceptMastery.MasteryLevel.INTERMEDIATE;
                } else {
                    level = ConceptMastery.MasteryLevel.BEGINNER;
                }

                String action;
                if (level == ConceptMastery.MasteryLevel.BEGINNER) {
                    action = "Review core foundational concepts & practice EASY problems for " + conceptName;
                } else if (level == ConceptMastery.MasteryLevel.INTERMEDIATE) {
                    action = "Practice MEDIUM difficulty diagnostic sets on " + conceptName;
                } else if (level == ConceptMastery.MasteryLevel.PROFICIENT) {
                    action = "Attempt HARD challenge sets to reach MASTER tier in " + conceptName;
                } else {
                    action = "Mastery achieved! Maintain velocity with periodic spaced retention.";
                }

                cm.setAttemptCount(attempts);
                cm.setCorrectCount(corrects);
                cm.setAccuracy(accuracy);
                cm.setConfidenceScore(confidence);
                cm.setMasteryLevel(level);
                cm.setRecommendedAction(action);
                cm.setLastAssessedAt(LocalDateTime.now());

                conceptRepository.save(cm);
            }
        }

        // Recalculate Knowledge Profile summary
        List<ConceptMastery> allConcepts = conceptRepository.findByUserId(userId);

        int mastered = 0;
        int proficient = 0;
        int intermediate = 0;
        int beginner = 0;
        List<String> strongList = new ArrayList<>();
        List<String> weakList = new ArrayList<>();

        for (ConceptMastery cm : allConcepts) {
            if (cm.getMasteryLevel() == ConceptMastery.MasteryLevel.MASTER) {
                mastered++;
                strongList.add(cm.getConceptName());
            } else if (cm.getMasteryLevel() == ConceptMastery.MasteryLevel.PROFICIENT) {
                proficient++;
                strongList.add(cm.getConceptName());
            } else if (cm.getMasteryLevel() == ConceptMastery.MasteryLevel.INTERMEDIATE) {
                intermediate++;
            } else {
                beginner++;
                weakList.add(cm.getConceptName());
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
            k.setStudentProfileId(result.getStudentProfileId());
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

        KnowledgeProfile savedKp = profileRepository.save(kp);

        // Sync with StudentProfile
        Optional<StudentProfile> profOpt = studentProfileRepository.findByUserId(userId);
        if (profOpt.isPresent()) {
            StudentProfile prof = profOpt.get();
            
            Map<String, List<String>> weakMap = prof.getWeakConcepts() != null ? prof.getWeakConcepts() : new HashMap<>();
            weakMap.put(subjectName, weakList);
            prof.setWeakConcepts(weakMap);

            Map<String, List<String>> strongMap = prof.getStrongConcepts() != null ? prof.getStrongConcepts() : new HashMap<>();
            strongMap.put(subjectName, strongList);
            prof.setStrongConcepts(strongMap);

            studentProfileRepository.save(prof);
        }

        // Trigger real-time recommendation engine generation
        try {
            recommendationService.generateRecommendations(userId);
        } catch (Exception ex) {
            System.err.println("Failed to trigger recommendation generation: " + ex.getMessage());
        }

        return savedKp;
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
                .filter(cm -> cm.getAccuracy() < 60.0 || cm.getMasteryLevel() == ConceptMastery.MasteryLevel.BEGINNER)
                .map(ConceptMasteryResponse::new)
                .collect(Collectors.toList());
    }

    public List<ConceptMasteryResponse> getStrongConcepts(String userId) {
        return conceptRepository.findByUserId(userId)
                .stream()
                .filter(cm -> cm.getAccuracy() >= 75.0 || cm.getMasteryLevel() == ConceptMastery.MasteryLevel.MASTER || cm.getMasteryLevel() == ConceptMastery.MasteryLevel.PROFICIENT)
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
