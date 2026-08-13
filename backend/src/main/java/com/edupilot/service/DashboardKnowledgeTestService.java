package com.edupilot.service;

import com.edupilot.dto.DashboardTestQuestionDTO;
import com.edupilot.dto.DashboardTestResultDTO;
import com.edupilot.dto.DashboardTestSubmissionDTO;
import com.edupilot.model.DashboardTestResult;
import com.edupilot.model.DashboardTestSession;
import com.edupilot.model.QuizQuestion;
import com.edupilot.model.StudentProfile;
import com.edupilot.repository.DashboardTestResultRepository;
import com.edupilot.repository.DashboardTestSessionRepository;
import com.edupilot.repository.QuizQuestionRepository;
import com.edupilot.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardKnowledgeTestService {

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private QuizQuestionRepository questionRepository;

    @Autowired
    private QuizGenerationService quizGenerationService;

    @Autowired
    private DashboardTestSessionRepository sessionRepository;

    @Autowired
    private DashboardTestResultRepository resultRepository;

    public Map<String, Object> generateTestForStudent(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("studentId is required and cannot be blank");
        }

        StudentProfile profile = studentService.findOrCreateProfile(studentId);

        List<String> subjects = profile.getSubjects();
        if (subjects == null || subjects.isEmpty()) {
            subjects = List.of(
                    "Data Structures & Algorithms", 
                    "Database Management Systems", 
                    "Operating Systems", 
                    "Computer Networks", 
                    "Software Engineering"
            );
        }

        List<QuizQuestion> allGeneratedQuestions = new ArrayList<>();
        List<String> questionIds = new ArrayList<>();

        for (String subject : subjects) {
            List<QuizQuestion> subjectQuestions = quizGenerationService.generate(
                    subject, 
                    QuizQuestion.Difficulty.EASY, 
                    5
            );
            allGeneratedQuestions.addAll(subjectQuestions);
            for (QuizQuestion q : subjectQuestions) {
                if (q.getId() != null) {
                    questionIds.add(q.getId());
                }
            }
            try {
                Thread.sleep(400);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        DashboardTestSession session = new DashboardTestSession();
        session.setStudentId(profile.getId() != null ? profile.getId() : studentId);
        session.setSubjects(subjects);
        session.setQuestionIds(questionIds);
        session.setCompleted(false);
        sessionRepository.save(session);

        List<DashboardTestQuestionDTO> questionDTOs = allGeneratedQuestions.stream()
                .map(q -> new DashboardTestQuestionDTO(
                        q.getId(),
                        q.getSubject(),
                        q.getConcept(),
                        q.getQuestionText(),
                        q.getOptions()
                ))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("studentId", studentId);
        response.put("subjects", subjects);
        response.put("totalQuestions", questionDTOs.size());
        response.put("questions", questionDTOs);

        return response;
    }

    public DashboardTestResultDTO gradeSubmission(DashboardTestSubmissionDTO submission) {
        if (submission == null || submission.getStudentId() == null || submission.getStudentId().trim().isEmpty()) {
            throw new IllegalArgumentException("studentId is required in submission");
        }
        if (submission.getSessionId() == null || submission.getSessionId().trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId is required in submission");
        }

        DashboardTestSession session = sessionRepository.findById(submission.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Test session not found for ID: " + submission.getSessionId()));

        List<QuizQuestion> questions = questionRepository.findAllById(session.getQuestionIds());
        Map<String, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q, (q1, q2) -> q1));

        Map<String, Integer> correctPerSubject = new HashMap<>();
        Map<String, Integer> totalPerSubject = new HashMap<>();

        if (session.getSubjects() != null) {
            for (String sub : session.getSubjects()) {
                correctPerSubject.put(sub, 0);
                totalPerSubject.put(sub, 0);
            }
        }

        int totalCorrect = 0;
        int totalQuestions = submission.getAnswers() != null ? submission.getAnswers().size() : 0;

        if (submission.getAnswers() != null) {
            for (DashboardTestSubmissionDTO.AnswerEntry answer : submission.getAnswers()) {
                QuizQuestion q = questionMap.get(answer.getQuestionId());
                if (q != null) {
                    String subject = q.getSubject() != null ? q.getSubject() : "General";
                    totalPerSubject.put(subject, totalPerSubject.getOrDefault(subject, 0) + 1);

                    if (answer.getSelectedOptionIndex() == q.getCorrectOptionIndex()) {
                        correctPerSubject.put(subject, correctPerSubject.getOrDefault(subject, 0) + 1);
                        totalCorrect++;
                    }
                }
            }
        }

        Map<String, Double> subjectScorePercentage = new HashMap<>();
        for (Map.Entry<String, Integer> entry : totalPerSubject.entrySet()) {
            String subject = entry.getKey();
            int total = entry.getValue();
            int correct = correctPerSubject.getOrDefault(subject, 0);
            double pct = total > 0 ? Math.round(((double) correct / total * 100.0) * 10.0) / 10.0 : 0.0;
            subjectScorePercentage.put(subject, pct);
        }

        double overallPercentage = totalQuestions > 0 
                ? Math.round(((double) totalCorrect / totalQuestions * 100.0) * 10.0) / 10.0 
                : 0.0;

        DashboardTestResult result = new DashboardTestResult();
        result.setSessionId(session.getId());
        result.setStudentId(submission.getStudentId());
        result.setSubjectScorePercentage(subjectScorePercentage);
        result.setCorrectCountPerSubject(correctPerSubject);
        result.setTotalQuestions(totalQuestions);
        result.setTotalCorrect(totalCorrect);
        result.setOverallPercentage(overallPercentage);
        resultRepository.save(result);

        session.setCompleted(true);
        sessionRepository.save(session);

        String createdAtStr = result.getCreatedAt() != null 
                ? result.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) 
                : "";

        return new DashboardTestResultDTO(
                result.getSessionId(),
                result.getStudentId(),
                result.getSubjectScorePercentage(),
                result.getCorrectCountPerSubject(),
                result.getTotalQuestions(),
                result.getTotalCorrect(),
                result.getOverallPercentage(),
                createdAtStr
        );
    }

    public DashboardTestResultDTO getLatestResult(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("studentId is required");
        }

        Optional<DashboardTestResult> optResult = resultRepository.findTopByStudentIdOrderByCreatedAtDesc(studentId);
        if (optResult.isEmpty()) {
            // Also try resolving studentProfileId if studentId passed was userId
            Optional<StudentProfile> optProfile = profileRepository.findByUserId(studentId);
            if (optProfile.isPresent()) {
                optResult = resultRepository.findTopByStudentIdOrderByCreatedAtDesc(optProfile.get().getId());
            }
        }

        if (optResult.isEmpty()) {
            return null;
        }

        DashboardTestResult result = optResult.get();
        String createdAtStr = result.getCreatedAt() != null 
                ? result.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) 
                : "";

        return new DashboardTestResultDTO(
                result.getSessionId(),
                result.getStudentId(),
                result.getSubjectScorePercentage(),
                result.getCorrectCountPerSubject(),
                result.getTotalQuestions(),
                result.getTotalCorrect(),
                result.getOverallPercentage(),
                createdAtStr
        );
    }
}
