package com.edupilot.service;

import com.edupilot.dto.*;
import com.edupilot.model.*;
import com.edupilot.repository.*;
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
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private com.edupilot.repository.QuizSessionRepository quizSessionRepository;

    @Autowired
    private LearningPlannerService plannerService;

    public static String normalizeConceptName(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "General Concept";
        }
        String c = raw.trim();

        // 1. Remove standard question preamble prefixes
        c = c.replaceAll("(?i)^(Regarding fundamental principles of|In an operational engineering context for|Under high-scale production constraints evaluating|In foundational study of|When implementing practical workflows for|Advanced application of|Foundational principles of)\\s+", "");

        // 2. Remove difficulty/tier & index suffixes like " (EASY #16)", " Implementation (MEDIUM #16)", " Architecture (HARD #16)", " - EASY #16", " [HARD #16]"
        c = c.replaceAll("(?i)\\s+(Implementation|Architecture|Foundations|Concepts|Mechanics|Principles)?\\s*[-|\\[\\(]?\\s*(EASY|MEDIUM|HARD|Tier\\s*\\d+)?\\s*#?\\d+[\\]\\)]?", "");

        // 3. Remove standalone trailing difficulty/tier/index tags e.g. "(EASY)", "(MEDIUM)", "(HARD)", "(Tier 1)", "#16", etc.
        c = c.replaceAll("(?i)\\s*[-|\\[\\(]?\\s*(EASY|MEDIUM|HARD|Tier\\s*\\d+)\\s*[\\]\\)]?", "");
        c = c.replaceAll("(?i)\\s*#\\d+$", "");

        // 4. Remove trailing template qualifier words if remaining at end (e.g. " Implementation", " Architecture", " Foundations", " Mechanics")
        if (c.matches("(?i).+\\s+(Implementation|Architecture|Foundations|Mechanics|Concepts|Principles)$") 
                && !c.equalsIgnoreCase("Software Architecture") 
                && !c.equalsIgnoreCase("System Architecture")) {
            c = c.replaceAll("(?i)\\s+(Implementation|Architecture|Foundations|Mechanics|Concepts|Principles)$", "");
        }

        // Clean up trailing/leading punctuation
        c = c.replaceAll("^[\\s:-]+|[\\s:-]+$", "");

        return c.trim();
    }

    /**
     * Generate dynamic, explainable recommendations derived from the student's latest quiz session or concept mastery.
     */
    public List<RecommendationResponse> generateRecommendations(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Recommendation> generatedList = new ArrayList<>();
        Optional<QuizSession> latestSessionOpt = quizSessionRepository.findFirstByUserIdOrderByLastAnswerTimeDesc(userId);

        if (latestSessionOpt.isPresent()) {
            QuizSession latestSession = latestSessionOpt.get();
            String subjectCode = latestSession.getSubjectCode() != null ? latestSession.getSubjectCode() : "CS301";
            String subjectName = latestSession.getSubjectName() != null ? latestSession.getSubjectName() : "Data Structures & Algorithms";

            // Deactivate old active recommendations for previous sessions/subjects to prevent cross-subject contamination
            List<Recommendation> existingActive = recommendationRepository.findByUserIdAndStatus(userId, Recommendation.Status.ACTIVE);
            for (Recommendation r : existingActive) {
                r.setStatus(Recommendation.Status.COMPLETED);
                recommendationRepository.save(r);
            }

            // Extract incorrect concepts from the latest quiz session grouped by normalized core concept
            Map<String, Integer> mistakeCounts = new LinkedHashMap<>();
            Set<String> correctConcepts = new HashSet<>();

            if (latestSession.getAnswers() != null) {
                for (QuizSession.QuizAnswerRecord ans : latestSession.getAnswers()) {
                    String rawConcept = ans.getConcept();
                    if (rawConcept == null || rawConcept.isBlank()) continue;

                    String concept = normalizeConceptName(rawConcept);

                    if (ans.isCorrect()) {
                        correctConcepts.add(concept);
                    } else {
                        mistakeCounts.put(concept, mistakeCounts.getOrDefault(concept, 0) + 1);
                    }
                }
            }

            // Generate recommendations ONLY for concepts missed in this latest session
            for (Map.Entry<String, Integer> entry : mistakeCounts.entrySet()) {
                String conceptName = entry.getKey();
                int mistakes = entry.getValue();

                // If concept was answered correctly, skip
                if (correctConcepts.contains(conceptName) && mistakes == 0) {
                    continue;
                }

                Recommendation rec = new Recommendation();
                rec.setUserId(userId);
                rec.setStudentProfileId(latestSession.getStudentProfileId());
                rec.setSubjectCode(subjectCode);
                rec.setSubjectName(subjectName);
                rec.setTopic(conceptName);
                rec.setConceptName(conceptName);
                rec.setRecommendationType(Recommendation.RecommendationType.CONCEPT_REVISION);
                rec.setPriority(mistakes >= 2 ? Recommendation.Priority.CRITICAL : Recommendation.Priority.HIGH);
                rec.setReason("In your latest " + subjectName + " quiz, you missed " + conceptName + (mistakes > 1 ? (" " + mistakes + " times.") : "."));
                rec.setRecommendedAction("Review " + conceptName + " fundamental concepts and attempt practice questions.");
                rec.setEstimatedStudyTimeMinutes(20);
                rec.setDifficulty(mistakes >= 2 ? "EASY" : "MEDIUM");
                rec.setConfidenceScore(Math.max(0.0, 100.0 - mistakes * 25.0));
                rec.setStatus(Recommendation.Status.ACTIVE);
                rec.setCreatedAt(LocalDateTime.now());
                rec.setExpiresAt(LocalDateTime.now().plusDays(7));

                generatedList.add(recommendationRepository.save(rec));
            }
        } else {
            // Fallback for user without quiz sessions: Evaluate individual concept mastery entries
            Optional<KnowledgeProfile> kpOpt = knowledgeProfileRepository.findByUserId(userId);
            List<ConceptMastery> concepts = conceptRepository.findByUserId(userId);

            for (ConceptMastery cm : concepts) {
                String subjectCode = cm.getSubjectCode();
                String subjectName = cm.getSubjectName();
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
                    r.setTopic(conceptName);
                    r.setConceptName(conceptName);
                    return r;
                });

                if (accuracy < 50.0) {
                    rec.setRecommendationType(Recommendation.RecommendationType.CONCEPT_REVISION);
                    rec.setPriority(Recommendation.Priority.CRITICAL);
                    rec.setReason("Your concept mastery for " + conceptName + " is " + accuracy + "%, which is below the 50% threshold after " + attempts + " attempt(s).");
                    rec.setRecommendedAction("Review " + conceptName + " fundamental concepts and attempt practice questions.");
                    rec.setEstimatedStudyTimeMinutes(25);
                    rec.setDifficulty("EASY");
                    rec.setConfidenceScore(accuracy);
                    rec.setStatus(Recommendation.Status.ACTIVE);
                    rec.setCreatedAt(LocalDateTime.now());
                    rec.setExpiresAt(LocalDateTime.now().plusDays(7));
                    generatedList.add(recommendationRepository.save(rec));
                } else if (accuracy >= 50.0 && accuracy < 70.0) {
                    rec.setRecommendationType(Recommendation.RecommendationType.PRACTICE_SET);
                    rec.setPriority(Recommendation.Priority.HIGH);
                    rec.setReason("Concept accuracy for " + conceptName + " is " + accuracy + "%. Practice intermediate diagnostic sets.");
                    rec.setRecommendedAction("Solve 4 medium-difficulty practice questions on " + conceptName + ".");
                    rec.setEstimatedStudyTimeMinutes(20);
                    rec.setDifficulty("MEDIUM");
                    rec.setConfidenceScore(accuracy);
                    rec.setStatus(Recommendation.Status.ACTIVE);
                    rec.setCreatedAt(LocalDateTime.now());
                    rec.setExpiresAt(LocalDateTime.now().plusDays(7));
                    generatedList.add(recommendationRepository.save(rec));
                }
            }
        }

        // Rule 3: Check overall learning health for Retest Recommendation
        Optional<KnowledgeProfile> kpOpt = knowledgeProfileRepository.findByUserId(userId);
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

        // Fallback: Initial diagnostic recommendation ONLY for new students without any quiz sessions
        if (generatedList.isEmpty() && latestSessionOpt.isEmpty()) {
            String targetSubjCode = "CS301";
            String targetSubjName = "Data Structures & Algorithms";

            Optional<StudentProfile> profOpt = studentProfileRepository.findByUserId(userId);
            if (profOpt.isEmpty()) {
                profOpt = studentProfileRepository.findById(userId);
            }
            if (profOpt.isPresent()) {
                StudentProfile prof = profOpt.get();
                if (prof.getSubjects() != null && !prof.getSubjects().isEmpty()) {
                    targetSubjName = prof.getSubjects().get(0);
                    // Match code in subject catalog
                    Optional<Subject> catOpt = subjectRepository.findBySubjectName(targetSubjName);
                    if (catOpt.isPresent()) {
                        targetSubjCode = catOpt.get().getSubjectCode();
                    }
                } else if (prof.getBranch() != null) {
                    List<Subject> catalogSubjs = subjectRepository.findByBranchAndIsActiveTrue(prof.getBranch());
                    if (!catalogSubjs.isEmpty()) {
                        targetSubjCode = catalogSubjs.get(0).getSubjectCode();
                        targetSubjName = catalogSubjs.get(0).getSubjectName();
                    }
                }
            }

            Recommendation defaultRec = new Recommendation();
            defaultRec.setUserId(userId);
            defaultRec.setRecommendationType(Recommendation.RecommendationType.DIAGNOSTIC_RETEST);
            defaultRec.setPriority(Recommendation.Priority.HIGH);
            defaultRec.setSubjectCode(targetSubjCode);
            defaultRec.setSubjectName(targetSubjName);
            defaultRec.setTopic("Initial Diagnostic");
            defaultRec.setConceptName(targetSubjName + " Foundations");
            defaultRec.setReason("Initial diagnostic evaluation recommended for " + targetSubjName + " to map your conceptual mastery.");
            defaultRec.setRecommendedAction("Take your first 5-minute diagnostic assessment for " + targetSubjName + ".");
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
