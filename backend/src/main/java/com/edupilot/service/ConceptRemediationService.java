package com.edupilot.service;

import com.edupilot.dto.DashboardTestQuestionDTO;
import com.edupilot.dto.DashboardTestSubmissionDTO;
import com.edupilot.model.*;
import com.edupilot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ConceptRemediationService {

    @Autowired
    private QuizGenerationService quizGenerationService;

    @Autowired
    private DashboardTestSessionRepository sessionRepository;

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
        String canonicalUserId = profile.getUserId() != null ? profile.getUserId() : profile.getId();

        // 1. Generate 5 concept-targeted questions via QuizGenerationService
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

        // 2. Persist remediation session in MongoDB (reusing DashboardTestSession document)
        DashboardTestSession session = new DashboardTestSession();
        session.setStudentId(studentId);
        session.setSubjects(List.of(subject));
        session.setQuestionIds(questionIds);
        session.setCreatedAt(LocalDateTime.now());
        session.setCompleted(false);

        DashboardTestSession savedSession = sessionRepository.save(session);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", savedSession.getId());
        response.put("studentId", studentId);
        response.put("subject", subject);
        response.put("concept", concept);
        response.put("totalQuestions", questionDTOs.size());
        response.put("questions", questionDTOs);

        return response;
    }

    public Map<String, Object> submitRemediationTest(String studentId, String sessionId, List<DashboardTestSubmissionDTO.AnswerEntry> answers) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        DashboardTestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Remediation session not found: " + sessionId));

        List<String> questionIds = session.getQuestionIds();
        List<QuizQuestion> questions = questionRepository.findAllById(questionIds);

        Map<String, QuizQuestion> questionMap = new HashMap<>();
        for (QuizQuestion q : questions) {
            questionMap.put(q.getId(), q);
        }

        int totalQuestions = questions.size();
        int correctCount = 0;
        String subject = !session.getSubjects().isEmpty() ? session.getSubjects().get(0) : "General";
        String concept = "Core Concept";

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
        sessionRepository.save(session);

        StudentProfile profile = studentService.findOrCreateProfile(studentId != null ? studentId : session.getStudentId());
        String canonicalUserId = profile.getUserId() != null ? profile.getUserId() : profile.getId();

        if (passed) {
            // 1. Mark corresponding active/verification-pending recommendations as COMPLETED
            List<Recommendation> recs = recommendationRepository.findByUserIdAndStatus(canonicalUserId, Recommendation.Status.ACTIVE);
            recs.addAll(recommendationRepository.findByUserIdAndStatus(canonicalUserId, Recommendation.Status.VERIFICATION_PENDING));
            
            for (Recommendation r : recs) {
                if (r.getConceptName() != null && isConceptMatch(r.getConceptName(), concept)) {
                    r.setStatus(Recommendation.Status.COMPLETED);
                    recommendationRepository.save(r);
                }
            }

            // 2. Remove concept from StudentProfile.weakConcepts map
            if (profile.getWeakConcepts() != null && profile.getWeakConcepts().containsKey(subject)) {
                List<String> weaks = new ArrayList<>(profile.getWeakConcepts().get(subject));
                final String targetConcept = concept;
                weaks.removeIf(w -> isConceptMatch(w, targetConcept));
                profile.getWeakConcepts().put(subject, weaks);
                profileRepository.save(profile);
            }

            // 3. Update ConceptMastery document
            final String finalSubject = subject;
            final String finalConcept = targetConcept(concept);
            Optional<ConceptMastery> cmOpt = conceptMasteryRepository.findByUserIdAndConceptName(canonicalUserId, concept);
            ConceptMastery cm = cmOpt.orElseGet(() -> {
                ConceptMastery c = new ConceptMastery();
                c.setUserId(canonicalUserId);
                c.setStudentProfileId(profile.getId());
                c.setSubjectName(finalSubject);
                c.setConceptName(finalConcept);
                c.setTopic(finalConcept);
                return c;
            });
            cm.setMasteryLevel(ConceptMastery.MasteryLevel.MASTER);
            cm.setAccuracy(Math.max(85.0, percentage));
            cm.setConfidenceScore(100.0);
            cm.setLastAssessedAt(LocalDateTime.now());
            cm.setRecommendedAction("Remediated successfully! Concept cleared.");
            conceptMasteryRepository.save(cm);

            // 4. Force regenerate adaptive planner to clear remediated card from dashboard
            try {
                plannerService.generateLearningPlan(canonicalUserId);
            } catch (Exception e) {
                System.err.println("[ConceptRemediationService] Learning plan refresh note: " + e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("subject", subject);
        response.put("concept", concept);
        response.put("correctCount", correctCount);
        response.put("totalQuestions", totalQuestions);
        response.put("percentage", Math.round(percentage * 10.0) / 10.0);
        response.put("passed", passed);
        response.put("remediated", passed);
        response.put("message", passed 
                ? "Concept successfully remediated! Removed from weak concepts roadmap." 
                : "Remediation test not passed. Score must be >= 80% (4/5 correct). Concept remains flagged for practice.");

        return response;
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
}
