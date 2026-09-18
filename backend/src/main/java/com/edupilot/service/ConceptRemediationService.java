package com.edupilot.service;

import com.edupilot.dto.DashboardTestQuestionDTO;
import com.edupilot.dto.DashboardTestSubmissionDTO;
import com.edupilot.model.*;
import com.edupilot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ConceptRemediationService {

    @Autowired
    private QuizGenerationService quizGenerationService;

    @Autowired
    private RemediationSessionRepository remediationSessionRepository;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private QuizQuestionRepository questionRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    @Autowired
    private LearningPlannerService plannerService;

    @Autowired
    private StudentService studentService;


    public Map<String, Object> startRemediationTest(String studentId, String subject, String concept) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("studentId is required");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("subject is required");
        }
        if (concept == null || concept.trim().isEmpty()) {
            throw new IllegalArgumentException("concept is required");
        }

        StudentProfile profile = studentService.findOrCreateProfile(studentId);

        // 1. Generate 5 concept-targeted questions via QuizGenerationService (tagged as REMEDIATION)
        List<QuizQuestion> questions = quizGenerationService.generateForConcept(
                subject.trim(), 
                concept.trim(), 
                QuizQuestion.Difficulty.MEDIUM, 
                5
        );

        List<String> questionIds = new ArrayList<>();
        List<DashboardTestQuestionDTO> questionDTOs = new ArrayList<>();

        for (QuizQuestion q : questions) {
            if (q.getId() != null) {
                questionIds.add(q.getId());
            }
            questionDTOs.add(new DashboardTestQuestionDTO(
                    q.getId(),
                    q.getSubject(),
                    q.getConcept(),
                    q.getQuestionText(),
                    q.getOptions()
            ));
        }

        // 2. Persist remediation session in dedicated remediation_sessions MongoDB collection
        RemediationSession session = new RemediationSession();
        session.setStudentId(studentId);
        session.setSubject(subject.trim());
        session.setConcept(concept.trim());
        session.setQuestionIds(questionIds);
        session.setCreatedAt(LocalDateTime.now());
        session.setCompleted(false);
        session.setModuleType(ModuleType.REMEDIATION);

        RemediationSession savedSession = remediationSessionRepository.save(session);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", savedSession.getId());
        response.put("studentId", studentId);
        response.put("subject", subject);
        response.put("concept", concept);
        response.put("totalQuestions", questionDTOs.size());
        response.put("questions", questionDTOs);

        return response;
    }

    public Map<String, Object> startVerificationTest(String studentId, String subject, String concept) {
        Map<String, Object> res = startRemediationTest(studentId, subject, concept);
        String sessionId = (String) res.get("sessionId");
        if (sessionId != null) {
            remediationSessionRepository.findById(sessionId).ifPresent(s -> {
                s.setModuleType(ModuleType.VERIFICATION);
                remediationSessionRepository.save(s);
            });
        }
        return res;
    }

    public Map<String, Object> submitRemediationTest(String studentId, String sessionId, List<DashboardTestSubmissionDTO.AnswerEntry> answers) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        List<String> questionIds;
        String subject = "General";
        String concept = "Core Concept";
        String effectiveStudentId = studentId;

        RemediationSession session = remediationSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Remediation session not found: " + sessionId));
        questionIds = session.getQuestionIds();
        subject = session.getSubject() != null ? session.getSubject() : "General";
        concept = session.getConcept() != null ? session.getConcept() : "Core Concept";
        if (effectiveStudentId == null || effectiveStudentId.isBlank()) {
            effectiveStudentId = session.getStudentId();
        }

        List<QuizQuestion> questions = (questionIds != null && !questionIds.isEmpty()) 
                ? questionRepository.findAllById(questionIds) 
                : new ArrayList<>();

        Map<String, QuizQuestion> questionMap = new HashMap<>();
        for (QuizQuestion q : questions) {
            questionMap.put(q.getId(), q);
        }

        int totalQuestions = questions.size();
        int correctCount = 0;

        if (answers != null) {
            for (DashboardTestSubmissionDTO.AnswerEntry ans : answers) {
                QuizQuestion q = questionMap.get(ans.getQuestionId());
                if (q != null) {
                    if (q.getConcept() != null) {
                        concept = q.getConcept();
                    }
                    if (ans.getSelectedOptionIndex() == q.getCorrectOptionIndex()) {
                        correctCount++;
                    }
                }
            }
        }

        double percentage = totalQuestions > 0 ? ((double) correctCount / totalQuestions) * 100.0 : 0.0;
        boolean passed = (correctCount >= 4); // >= 80% required for remediation pass

        session.setCompleted(true);
        session.setCreatedAt(LocalDateTime.now());
        remediationSessionRepository.save(session);

        StudentProfile profile = studentService.findOrCreateProfile(effectiveStudentId);
        String canonicalUserId = profile.getUserId() != null ? profile.getUserId() : profile.getId();

        // Track previous knowledge for observational growth computation
        double previousKnowledge = 50.0;
        Optional<ConceptMastery> existingCmOpt = conceptMasteryRepository.findByUserIdAndConceptName(canonicalUserId, concept);
        if (existingCmOpt.isPresent()) {
            previousKnowledge = existingCmOpt.get().getAccuracy() > 0 ? existingCmOpt.get().getAccuracy() : existingCmOpt.get().getMasteryScore();
        }

        // 1. Persist QuizSession ONLY if this session is a Knowledge Check verification assessment
        if (session.getModuleType() == ModuleType.VERIFICATION) {
            QuizSession quizSession = new QuizSession();
            quizSession.setUserId(canonicalUserId);
            quizSession.setStudentProfileId(profile.getId());
            quizSession.setSubjectName(subject);
            quizSession.setSubjectCode("CS301");
            quizSession.setTotalQuestions(totalQuestions > 0 ? totalQuestions : 5);
            quizSession.setCorrectCount(correctCount);
            quizSession.setIncorrectCount(Math.max(0, totalQuestions - correctCount));
            quizSession.setStatus(QuizSession.Status.COMPLETED);
            quizSession.setVerificationQuiz(true);
            quizSession.setTargetConcept(concept);
            quizSession.setLastAnswerTime(LocalDateTime.now());
            quizSessionRepository.save(quizSession);
        }


        if (passed) {
            // Mark corresponding active/verification-pending recommendations as COMPLETED
            List<Recommendation> recs = recommendationRepository.findByUserIdAndStatus(canonicalUserId, Recommendation.Status.ACTIVE);
            recs.addAll(recommendationRepository.findByUserIdAndStatus(canonicalUserId, Recommendation.Status.VERIFICATION_PENDING));
            
            for (Recommendation r : recs) {
                if (r.getConceptName() != null && isConceptMatch(r.getConceptName(), concept)) {
                    r.setStatus(Recommendation.Status.COMPLETED);
                    recommendationRepository.save(r);
                }
            }

            // Remove concept from StudentProfile.weakConcepts map
            if (profile.getWeakConcepts() != null && profile.getWeakConcepts().containsKey(subject)) {
                List<String> weaks = new ArrayList<>(profile.getWeakConcepts().get(subject));
                final String targetConcept = concept;
                weaks.removeIf(w -> isConceptMatch(w, targetConcept));
                profile.getWeakConcepts().put(subject, weaks);
                profileRepository.save(profile);
            }
        }

        // Update ConceptMastery document
        final String finalSubject = subject;
        final String finalConcept = targetConcept(concept);
        ConceptMastery cm = existingCmOpt.orElseGet(() -> {
            ConceptMastery c = new ConceptMastery();
            c.setUserId(canonicalUserId);
            c.setStudentProfileId(profile.getId());
            c.setSubjectName(finalSubject);
            c.setConceptName(finalConcept);
            c.setTopic(finalConcept);
            return c;
        });

        double newAccuracy = Math.max(percentage, passed ? 85.0 : percentage);
        cm.setMasteryLevel(passed ? ConceptMastery.MasteryLevel.MASTER : ConceptMastery.MasteryLevel.INTERMEDIATE);
        cm.setAccuracy(newAccuracy);
        cm.setConfidenceScore(passed ? 100.0 : 70.0);
        cm.setLastAssessedAt(LocalDateTime.now());
        cm.setRecommendedAction(passed ? "Remediated successfully! Concept cleared." : "Observed progress recorded.");
        conceptMasteryRepository.save(cm);

        // Synchronize StudentProfile summary
        try {
            studentService.syncConceptMasteryWithProfile(canonicalUserId, finalSubject);
        } catch (Exception e) {
            System.err.println("[ConceptRemediationService] Profile mastery sync error: " + e.getMessage());
        }

        // Refresh adaptive planner
        try {
            plannerService.generateLearningPlan(canonicalUserId);
        } catch (Exception e) {
            System.err.println("[ConceptRemediationService] Learning plan refresh note: " + e.getMessage());
        }

        double observedGain = BigDecimal.valueOf(newAccuracy - previousKnowledge).setScale(1, RoundingMode.HALF_UP).doubleValue();

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("subject", subject);
        response.put("concept", concept);
        response.put("correctCount", correctCount);
        response.put("totalQuestions", totalQuestions);
        response.put("percentage", Math.round(percentage * 10.0) / 10.0);
        response.put("passed", passed);
        response.put("previousKnowledge", Math.round(previousKnowledge * 10.0) / 10.0);
        response.put("currentKnowledge", Math.round(newAccuracy * 10.0) / 10.0);
        response.put("observedGain", observedGain);
        response.put("message", "Observed knowledge change: " + (observedGain >= 0 ? "+" : "") + observedGain + " pp");

        return response;
    }

    /**
     * Dynamic Reassessment Eligibility Engine.
     * Evaluates if student is eligible for a 5-question Knowledge Check based on genuine completed learning activities.
     * Lifecycle Rule:
     * 1. Candidates are identified by completed learning activities (remediation sessions) or weak concepts (<70%).
     * 2. Eligibility REQUIRES a completed learning activity AFTER the previous completed Knowledge Check for that concept.
     * 3. A completed Knowledge Check consumes current eligibility.
     * 4. A new learning activity restores eligibility.
     * 5. Abandoned Knowledge Checks do NOT consume eligibility.
     * 6. Subject & concept isolation are strictly enforced.
     */
    public Map<String, Object> getPendingReassessment(String studentId, String subject) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasPendingCheck", false);

        if (studentId == null || studentId.trim().isEmpty()) {
            return result;
        }

        StudentProfile profile = studentService.findOrCreateProfile(studentId);
        String canonicalUserId = profile.getUserId() != null ? profile.getUserId() : profile.getId();

        String targetSubject = (subject != null && !subject.isBlank()) ? subject : "Data Structures & Algorithms";

        // 1. Gather candidate concepts in priority order
        List<RemediationSession> remSessions = remediationSessionRepository.findByStudentIdOrderByCreatedAtDesc(canonicalUserId);
        if (remSessions.isEmpty() && !studentId.equals(canonicalUserId)) {
            remSessions = remediationSessionRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        }

        List<String> candidateConcepts = new ArrayList<>();

        // Priority A: Concepts from completed remediation sessions for this subject
        for (RemediationSession rs : remSessions) {
            if (rs.isCompleted() && rs.getConcept() != null && !rs.getConcept().isBlank()) {
                if (rs.getSubject() == null || isConceptMatch(rs.getSubject(), targetSubject)) {
                    if (!candidateConcepts.contains(rs.getConcept())) {
                        candidateConcepts.add(rs.getConcept());
                    }
                }
            }
        }

        // Priority B: Weak concepts (<70% accuracy) for this subject
        List<ConceptMastery> cms = conceptMasteryRepository.findByUserId(canonicalUserId);
        for (ConceptMastery cm : cms) {
            String cmSubject = cm.getSubjectName() != null ? cm.getSubjectName() : "Data Structures & Algorithms";
            if (isConceptMatch(cmSubject, targetSubject)) {
                if (cm.getAttemptCount() > 0 && (cm.getStatus() == ConceptMastery.ConceptStatus.WEAK || cm.getAccuracy() < 70.0)) {
                    String conceptName = cm.getTopic() != null && !cm.getTopic().isBlank() ? cm.getTopic() : cm.getConceptName();
                    if (conceptName != null && !candidateConcepts.contains(conceptName)) {
                        candidateConcepts.add(conceptName);
                    }
                }
            }
        }

        if (candidateConcepts.isEmpty()) {
            return result;
        }

        // 2. Fetch completed verification quizzes for this student
        List<QuizSession> allUserQuizzes = quizSessionRepository.findByUserIdOrderByLastAnswerTimeAsc(canonicalUserId);

        // 3. Evaluate each candidate concept for valid unconsumed eligibility
        for (String candidateConcept : candidateConcepts) {
            // Find timestamp of the LATEST completed verification quiz for this exact concept
            LocalDateTime latestQuizTime = null;
            for (QuizSession qs : allUserQuizzes) {
                if (qs.isVerificationQuiz() && qs.getStatus() == QuizSession.Status.COMPLETED) {
                    if (qs.getTargetConcept() != null && isConceptMatch(qs.getTargetConcept(), candidateConcept)) {
                        if (qs.getLastAnswerTime() != null) {
                            if (latestQuizTime == null || qs.getLastAnswerTime().isAfter(latestQuizTime)) {
                                latestQuizTime = qs.getLastAnswerTime();
                            }
                        }
                    }
                }
            }

            // Find timestamp of the LATEST completed learning activity for this concept (excluding verification quizzes)
            LocalDateTime latestActivityTime = null;
            for (RemediationSession rs : remSessions) {
                if (rs.getModuleType() != ModuleType.VERIFICATION && rs.isCompleted() && rs.getConcept() != null && isConceptMatch(rs.getConcept(), candidateConcept)) {
                    if (rs.getSubject() == null || isConceptMatch(rs.getSubject(), targetSubject)) {
                        LocalDateTime actTime = rs.getCreatedAt() != null ? rs.getCreatedAt() : LocalDateTime.now().minusHours(1);
                        if (latestActivityTime == null || actTime.isAfter(latestActivityTime)) {
                            latestActivityTime = actTime;
                        }
                    }
                }
            }

            // Eligibility Condition:
            // A. If NO verification quiz has ever been completed for this concept AND a qualifying learning activity or concept mastery attempt exists -> ELIGIBLE
            // B. If a verification quiz WAS completed at latestQuizTime AND a new completed learning activity occurred AFTER latestQuizTime -> ELIGIBLE
            boolean isEligible = false;
            if (latestQuizTime == null) {
                // First-time eligibility: requires completed learning activity or concept mastery record
                if (latestActivityTime != null) {
                    isEligible = true;
                } else {
                    // Check if student has initial diagnostic / concept mastery evidence
                    for (ConceptMastery cm : cms) {
                        String cmConcept = cm.getTopic() != null ? cm.getTopic() : cm.getConceptName();
                        if (isConceptMatch(cmConcept, candidateConcept) && cm.getAttemptCount() > 0) {
                            isEligible = true;
                            break;
                        }
                    }
                }
            } else {
                // Secondary Knowledge Check: REQUIRES a new completed learning activity after latestQuizTime
                if (latestActivityTime != null && latestActivityTime.isAfter(latestQuizTime)) {
                    isEligible = true;
                }
            }

            if (isEligible) {
                result.put("hasPendingCheck", true);
                result.put("studentId", canonicalUserId);
                result.put("subject", targetSubject);
                result.put("concept", candidateConcept);
                result.put("questionCount", 5);
                result.put("description", "You've completed learning on " + candidateConcept + ". Take a quick 5-question check to measure your current understanding.");
                return result;
            }
        }

        return result;
    }

    private String targetConcept(String concept) {
        return concept != null ? concept : "Core Concept";
    }

    private boolean isConceptMatch(String c1, String c2) {
        if (c1 == null || c2 == null) return false;
        String clean1 = c1.trim().toLowerCase();
        String clean2 = c2.trim().toLowerCase();
        return clean1.equals(clean2) || clean1.contains(clean2) || clean2.contains(clean1);
    }

    public Map<String, Object> abandonRemediationSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId is required");
        }
        Optional<RemediationSession> remSessionOpt = remediationSessionRepository.findById(sessionId.trim());
        if (remSessionOpt.isPresent()) {
            RemediationSession session = remSessionOpt.get();
            session.setCompleted(false);
            remediationSessionRepository.save(session);
            return Map.of("status", "SESSION_ABANDONED", "sessionId", sessionId);
        }
        return Map.of("status", "SESSION_NOT_FOUND", "sessionId", sessionId);
    }
}
