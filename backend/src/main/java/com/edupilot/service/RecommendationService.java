package com.edupilot.service;

import com.edupilot.dto.RecommendationResponse;
import com.edupilot.model.ConceptMastery;
import com.edupilot.model.KnowledgeProfile;
import com.edupilot.model.Recommendation;
import com.edupilot.repository.ConceptMasteryRepository;
import com.edupilot.repository.KnowledgeProfileRepository;
import com.edupilot.repository.RecommendationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private ConceptMasteryRepository conceptRepository;

    @Autowired
    private KnowledgeProfileRepository knowledgeProfileRepository;

    @Autowired
    private LearningPlannerService plannerService;

    /**
     * Generate dynamic, explainable recommendations derived from Knowledge Engine profile & concept mastery.
     */
    public List<RecommendationResponse> generateRecommendations(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Optional<KnowledgeProfile> kpOpt = knowledgeProfileRepository.findByUserId(userId);
        List<ConceptMastery> concepts = conceptRepository.findByUserId(userId);

        List<Recommendation> generatedList = new ArrayList<>();

        // Rule 1 & Rule 2: Evaluate individual concept mastery entries
        for (ConceptMastery cm : concepts) {
            String subjectCode = cm.getSubjectCode();
            String subjectName = cm.getSubjectName();
            String topic = cm.getTopic();
            String conceptName = cm.getConceptName();
            double accuracy = cm.getAccuracy();
            int attempts = cm.getAttemptCount();

            Optional<Recommendation> existingOpt = recommendationRepository
                    .findByUserIdAndSubjectCodeAndConceptNameAndStatus(userId, subjectCode, conceptName, Recommendation.Status.ACTIVE);

            Recommendation rec = existingOpt.orElseGet(() -> {
                Recommendation r = new Recommendation();
                r.setUserId(userId);
                r.setStudentProfileId(cm.getStudentProfileId());
                r.setSubjectCode(subjectCode);
                r.setSubjectName(subjectName);
                r.setTopic(topic);
                r.setConceptName(conceptName);
                return r;
            });

            if (accuracy < 50.0) {
                // Rule 1: CRITICAL Concept Revision
                rec.setRecommendationType(Recommendation.RecommendationType.CONCEPT_REVISION);
                rec.setPriority(Recommendation.Priority.CRITICAL);
                rec.setReason("Your concept mastery for " + conceptName + " is " + accuracy + "%, which is below the 50% proficiency threshold after " + attempts + " attempt(s).");
                rec.setRecommendedAction("Review " + conceptName + " fundamental concepts and attempt 5 practice questions.");
                rec.setEstimatedStudyTimeMinutes(25);
                rec.setDifficulty("EASY");
                rec.setConfidenceScore(accuracy);
                rec.setStatus(Recommendation.Status.ACTIVE);
                rec.setCreatedAt(LocalDateTime.now());
                rec.setExpiresAt(LocalDateTime.now().plusDays(7));
                generatedList.add(recommendationRepository.save(rec));

            } else if (accuracy >= 50.0 && accuracy < 70.0) {
                // Rule 2: MEDIUM Practice Set
                rec.setRecommendationType(Recommendation.RecommendationType.PRACTICE_SET);
                rec.setPriority(Recommendation.Priority.MEDIUM);
                rec.setReason("Concept accuracy for " + conceptName + " is " + accuracy + "%. Practice intermediate diagnostic sets to reach proficiency.");
                rec.setRecommendedAction("Solve 4 medium-difficulty practice questions on " + conceptName + ".");
                rec.setEstimatedStudyTimeMinutes(20);
                rec.setDifficulty("MEDIUM");
                rec.setConfidenceScore(accuracy);
                rec.setStatus(Recommendation.Status.ACTIVE);
                rec.setCreatedAt(LocalDateTime.now());
                rec.setExpiresAt(LocalDateTime.now().plusDays(7));
                generatedList.add(recommendationRepository.save(rec));

            } else if (accuracy >= 85.0) {
                // Rule 4: LOW Advanced Practice
                rec.setRecommendationType(Recommendation.RecommendationType.ADVANCED_PRACTICE);
                rec.setPriority(Recommendation.Priority.LOW);
                rec.setReason("You demonstrated high mastery (" + accuracy + "%) in " + conceptName + ". Challenge yourself with advanced optimization problems.");
                rec.setRecommendedAction("Attempt hard-tier challenge items on " + conceptName + ".");
                rec.setEstimatedStudyTimeMinutes(30);
                rec.setDifficulty("HARD");
                rec.setConfidenceScore(accuracy);
                rec.setStatus(Recommendation.Status.ACTIVE);
                rec.setCreatedAt(LocalDateTime.now());
                rec.setExpiresAt(LocalDateTime.now().plusDays(7));
                generatedList.add(recommendationRepository.save(rec));
            }
        }

        // Rule 3: Check overall learning health for Retest Recommendation
        if (kpOpt.isPresent()) {
            KnowledgeProfile kp = kpOpt.get();
            if (kp.getLearningHealthScore() < 60.0 || kp.getBeginnerCount() > 2) {
                Optional<Recommendation> existingRetest = recommendationRepository
                        .findByUserIdAndSubjectCodeAndConceptNameAndStatus(userId, "CS301", "Overall Diagnostic", Recommendation.Status.ACTIVE);
                
                Recommendation retestRec = existingRetest.orElseGet(() -> {
                    Recommendation r = new Recommendation();
                    r.setUserId(userId);
                    r.setSubjectCode("CS301");
                    r.setSubjectName("Data Structures & Algorithms");
                    r.setTopic("Diagnostic Retest");
                    r.setConceptName("Overall Diagnostic");
                    return r;
                });

                retestRec.setRecommendationType(Recommendation.RecommendationType.DIAGNOSTIC_RETEST);
                retestRec.setPriority(Recommendation.Priority.HIGH);
                retestRec.setReason("Overall learning health score is " + kp.getLearningHealthScore() + "%. Taking an adaptive diagnostic test will update your SGI and recommendation map.");
                retestRec.setRecommendedAction("Attempt 10-minute diagnostic assessment to refresh learning health.");
                retestRec.setEstimatedStudyTimeMinutes(15);
                retestRec.setDifficulty("MEDIUM");
                retestRec.setConfidenceScore(kp.getLearningHealthScore());
                retestRec.setStatus(Recommendation.Status.ACTIVE);
                retestRec.setCreatedAt(LocalDateTime.now());
                retestRec.setExpiresAt(LocalDateTime.now().plusDays(7));
                generatedList.add(recommendationRepository.save(retestRec));
            }
        }

        // Fallback default recommendation if no custom recommendations generated
        if (generatedList.isEmpty()) {
            Recommendation defaultRec = new Recommendation();
            defaultRec.setUserId(userId);
            defaultRec.setRecommendationType(Recommendation.RecommendationType.DIAGNOSTIC_RETEST);
            defaultRec.setPriority(Recommendation.Priority.HIGH);
            defaultRec.setSubjectCode("CS301");
            defaultRec.setSubjectName("Data Structures & Algorithms");
            defaultRec.setTopic("Binary Search Trees");
            defaultRec.setConceptName("Binary Search Trees");
            defaultRec.setReason("Initial diagnostic evaluation recommended to generate your personalized SGI and knowledge map.");
            defaultRec.setRecommendedAction("Take your first 5-minute diagnostic assessment for Data Structures.");
            defaultRec.setEstimatedStudyTimeMinutes(15);
            defaultRec.setDifficulty("MEDIUM");
            defaultRec.setConfidenceScore(50.0);
            defaultRec.setStatus(Recommendation.Status.ACTIVE);
            defaultRec.setCreatedAt(LocalDateTime.now());
            defaultRec.setExpiresAt(LocalDateTime.now().plusDays(7));
            generatedList.add(recommendationRepository.save(defaultRec));
        }

        // Trigger Learning Planner regeneration automatically
        try {
            plannerService.generateLearningPlan(userId);
        } catch (Exception ex) {
            System.err.println("Failed to trigger planner generation: " + ex.getMessage());
        }

        return generatedList.stream().map(RecommendationResponse::new).collect(Collectors.toList());
    }

    public List<RecommendationResponse> getActiveRecommendations(String userId) {
        List<Recommendation> active = recommendationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Recommendation.Status.ACTIVE);
        if (active.isEmpty()) {
            return generateRecommendations(userId);
        }
        return active.stream().map(RecommendationResponse::new).collect(Collectors.toList());
    }

    public List<RecommendationResponse> getHighPriorityRecommendations(String userId) {
        List<Recommendation.Priority> priorities = List.of(Recommendation.Priority.CRITICAL, Recommendation.Priority.HIGH);
        List<Recommendation> highList = recommendationRepository.findByUserIdAndPriorityInAndStatus(userId, priorities, Recommendation.Status.ACTIVE);
        if (highList.isEmpty()) {
            return getActiveRecommendations(userId);
        }
        return highList.stream().map(RecommendationResponse::new).collect(Collectors.toList());
    }

    public RecommendationResponse completeRecommendation(String id) {
        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recommendation ID not found: " + id));
        
        rec.setStatus(Recommendation.Status.COMPLETED);
        Recommendation saved = recommendationRepository.save(rec);
        return new RecommendationResponse(saved);
    }
}
