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
    private LearningPlannerService plannerService;

    /**
     * Generate dynamic, explainable recommendations derived from Knowledge Engine profile & concept mastery.
     */
    public List<RecommendationResponse> generateRecommendations(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Optional<StudentProfile> profOpt = studentProfileRepository.findByUserId(userId);
        if (profOpt.isEmpty()) {
            profOpt = studentProfileRepository.findById(userId);
        }
        if (profOpt.isEmpty()) {
            return Collections.emptyList();
        }
        StudentProfile profile = profOpt.get();
        List<String> subjects = profile.getSubjects();
        if (subjects == null || subjects.isEmpty()) {
            return Collections.emptyList();
        }

        List<Recommendation> generatedList = new ArrayList<>();

        for (String subjectName : subjects) {
            Optional<Subject> catOpt = subjectRepository.findBySubjectName(subjectName);
            final String finalSubjectCode = catOpt.isPresent() ? catOpt.get().getSubjectCode() : "CS301";
            final String finalSubjectName = subjectName;

            // 1. Fetch user ConceptMastery records for this subject
            List<ConceptMastery> userConceptMasteries = conceptRepository.findByUserIdAndSubjectCode(userId, finalSubjectCode);
            boolean hasAnyRealPerformanceData = false;
            if (userConceptMasteries != null) {
                for (ConceptMastery cm : userConceptMasteries) {
                    if (cm.getAttemptCount() > 0 && cm.getTopic() != null && 
                        !cm.getTopic().equalsIgnoreCase("Initial Diagnostic") && 
                        !cm.getTopic().equalsIgnoreCase("Initial Diagnostic Foundations")) {
                        hasAnyRealPerformanceData = true;
                        break;
                    }
                }
            }

            // 2. Lookup existing active recommendations for this user and subject
            List<Recommendation> activeRecs = recommendationRepository.findByUserIdAndSubjectCodeAndStatus(
                userId, finalSubjectCode, Recommendation.Status.ACTIVE
            );

            Recommendation rec;
            if (activeRecs.isEmpty()) {
                rec = new Recommendation();
                rec.setUserId(userId);
                rec.setStudentProfileId(profile.getId());
                rec.setSubjectCode(finalSubjectCode);
                rec.setSubjectName(finalSubjectName);
                rec.setStatus(Recommendation.Status.ACTIVE);
            } else {
                rec = activeRecs.get(0);
                // Deactivate any duplicate active recommendations for this subject
                for (int i = 1; i < activeRecs.size(); i++) {
                    Recommendation extra = activeRecs.get(i);
                    extra.setStatus(Recommendation.Status.COMPLETED);
                    recommendationRepository.save(extra);
                }
            }

            String prevTopic = rec.getTopic();

            if (!hasAnyRealPerformanceData) {
                // If there's no actual performance data, we MUST recommend Initial Diagnostic
                rec.setTopic("Initial Diagnostic");
                rec.setConceptName(finalSubjectName + " Foundations");
                rec.setReason("Baseline diagnostic evaluation required to map conceptual mastery for " + finalSubjectName + ".");
                rec.setRecommendedAction("Take your first 5-minute diagnostic assessment for " + finalSubjectName + ".");
                rec.setRecommendationType(Recommendation.RecommendationType.DIAGNOSTIC_RETEST);
                rec.setPriority(Recommendation.Priority.HIGH);
                rec.setDifficulty("MEDIUM");
                rec.setConfidenceScore(50.0);
                rec.setMasteryScore(50.0);
                rec.setAccuracy(50.0);
                rec.setPrevTopic(prevTopic);
                rec.setCreatedAt(LocalDateTime.now());
                rec.setExpiresAt(LocalDateTime.now().plusDays(7));
            } else {
                // We have real performance data. Exclude "Initial Diagnostic" and find highest-priority weak topic
                List<String> topics = getTopicsForSubject(finalSubjectName, finalSubjectCode, userId);
                topics.remove("Initial Diagnostic");
                topics.remove("Initial Diagnostic Foundations");

                String highestPriorityTopic = null;
                double maxScore = -1.0;
                ConceptMastery bestCm = null;

                for (String topic : topics) {
                    Optional<ConceptMastery> cmOpt = conceptRepository.findByUserIdAndSubjectCodeAndTopicAndConceptName(
                        userId, finalSubjectCode, topic, topic
                    );
                    double accuracy = 50.0;
                    int attempts = 0;
                    int recentWrongs = 0;
                    boolean hasData = false;
                    ConceptMastery cm = null;

                    if (cmOpt.isPresent()) {
                        cm = cmOpt.get();
                        accuracy = cm.getAccuracy();
                        attempts = cm.getAttemptCount();
                        recentWrongs = cm.getRecentWrongAnswerCount();
                        hasData = (attempts > 0);
                    }

                    double priorityScore;
                    if (!hasData) {
                        priorityScore = 45.0; // Moderate priority for unattempted topics
                    } else {
                        priorityScore = (100.0 - accuracy) + (recentWrongs * 15.0);
                    }

                    if (priorityScore > maxScore) {
                        maxScore = priorityScore;
                        highestPriorityTopic = topic;
                        bestCm = cm;
                    }
                }

                if (highestPriorityTopic == null) {
                    highestPriorityTopic = topics.isEmpty() ? "General Concepts" : topics.get(0);
                }

                rec.setTopic(highestPriorityTopic);
                rec.setConceptName(highestPriorityTopic);

                double acc = bestCm != null ? bestCm.getAccuracy() : 50.0;
                int recentWrongs = bestCm != null ? bestCm.getRecentWrongAnswerCount() : 0;
                int totalAttempts = bestCm != null ? bestCm.getAttemptCount() : 0;

                String reason = "";
                if (prevTopic != null && !prevTopic.equalsIgnoreCase(highestPriorityTopic) && !prevTopic.equalsIgnoreCase("Initial Diagnostic")) {
                    Optional<ConceptMastery> prevCmOpt = conceptRepository.findByUserIdAndSubjectCodeAndTopicAndConceptName(
                        userId, finalSubjectCode, prevTopic, prevTopic
                    );
                    if (prevCmOpt.isPresent()) {
                        double prevAcc = prevCmOpt.get().getAccuracy();
                        reason = "Your " + prevTopic + " accuracy improved to " + Math.round(prevAcc) + "%; " + highestPriorityTopic + " is now your highest-priority topic.";
                    } else {
                        reason = "You completed " + prevTopic + "; " + highestPriorityTopic + " is now your highest-priority topic.";
                    }
                }

                if (reason.isEmpty()) {
                    if (recentWrongs > 0) {
                        reason = "You answered " + recentWrongs + " of recent questions on " + highestPriorityTopic + " incorrectly.";
                    } else if (totalAttempts == 0) {
                        reason = "Establish foundational understanding of " + highestPriorityTopic + " to boost your mastery.";
                    } else {
                        reason = "Your " + highestPriorityTopic + " accuracy is " + Math.round(acc) + "%. Practice to build consistency.";
                    }
                }

                rec.setReason(reason);

                Recommendation.Priority priorityLevel;
                if (maxScore >= 75.0) {
                    priorityLevel = Recommendation.Priority.CRITICAL;
                } else if (maxScore >= 50.0) {
                    priorityLevel = Recommendation.Priority.HIGH;
                } else if (maxScore >= 30.0) {
                    priorityLevel = Recommendation.Priority.MEDIUM;
                } else {
                    priorityLevel = Recommendation.Priority.LOW;
                }
                rec.setPriority(priorityLevel);

                String difficulty = "MEDIUM";
                if (acc < 50.0) difficulty = "EASY";
                else if (acc >= 75.0) difficulty = "HARD";
                rec.setDifficulty(difficulty);

                rec.setRecommendationType(Recommendation.RecommendationType.CONCEPT_REVISION);
                rec.setRecommendedAction("Solve practice questions on " + highestPriorityTopic + ".");
                rec.setEstimatedStudyTimeMinutes(20);
                rec.setConfidenceScore(acc);
                rec.setMasteryScore(bestCm != null ? bestCm.getMasteryScore() : 50.0);
                rec.setAccuracy(acc);
                rec.setPrevTopic(prevTopic);
                rec.setCreatedAt(LocalDateTime.now());
                rec.setExpiresAt(LocalDateTime.now().plusDays(7));
            }

            generatedList.add(recommendationRepository.save(rec));
        }

        try {
            plannerService.generateLearningPlan(userId);
        } catch (Exception ex) {
            System.err.println("Failed to trigger planner generation: " + ex.getMessage());
        }

        return generatedList.stream().map(RecommendationResponse::new).collect(Collectors.toList());
    }

    private List<String> getTopicsForSubject(String subjectName, String subjectCode, String userId) {
        Set<String> topics = new LinkedHashSet<>();
        
        String lower = subjectName.toLowerCase();
        if (lower.contains("data structure") || lower.contains("algorithm") || lower.contains("dsa")) {
            topics.addAll(Arrays.asList("Recursion", "Trees", "Graphs", "Sorting", "Binary Search Trees", "Sorting Algorithms", "Graph Theory", "Dynamic Programming", "Hash Tables"));
        } else if (lower.contains("database") || lower.contains("dbms")) {
            topics.addAll(Arrays.asList("Normalization", "Indexing", "SQL Queries", "Transactions", "Relational Algebra"));
        } else if (lower.contains("discrete") || lower.contains("math")) {
            topics.addAll(Arrays.asList("Set Theory", "Graph Theory", "Combinatorics", "Propositional Logic"));
        } else if (lower.contains("blockchain")) {
            topics.addAll(Arrays.asList("Consensus Mechanisms", "Smart Contracts", "Proof of Stake", "Cryptographic Linking"));
        } else if (lower.contains("cloud")) {
            topics.addAll(Arrays.asList("Identity & Access Management", "Shared Responsibility Model", "Virtual Firewalls", "Zero Trust Security"));
        } else if (lower.contains("artificial intelligence") || lower.contains("machine learning") || lower.contains("ai")) {
            topics.addAll(Arrays.asList("Neural Networks", "Supervised Learning", "Core Concepts", "Advanced Principles", "Optimization"));
        } else {
            topics.addAll(Arrays.asList("Core Concepts", "Advanced Principles", "Optimization"));
        }

        List<ConceptMastery> cmList = conceptRepository.findByUserId(userId);
        if (cmList != null) {
            for (ConceptMastery cm : cmList) {
                if (cm.getSubjectName().equalsIgnoreCase(subjectName) || cm.getSubjectCode().equalsIgnoreCase(subjectCode)) {
                    if (cm.getTopic() != null && !cm.getTopic().isBlank()) {
                        topics.add(cm.getTopic());
                    }
                }
            }
        }

        return new ArrayList<>(topics);
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
