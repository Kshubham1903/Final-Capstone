package com.edupilot.service;

import com.edupilot.dto.AttemptMasteryPointDTO;
import com.edupilot.dto.DailyMasteryPointDTO;
import com.edupilot.model.AssessmentResult;
import com.edupilot.model.DashboardTestResult;
import com.edupilot.model.QuizSession;
import com.edupilot.model.StudentProfile;
import com.edupilot.repository.AssessmentResultRepository;
import com.edupilot.repository.DashboardTestResultRepository;
import com.edupilot.repository.QuizSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SubjectProgressHistoryService {

    @Autowired
    private StudentService studentService;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private DashboardTestResultRepository dashboardTestResultRepository;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    private static class DayStats {
        int questionsAnswered = 0;
        int correctAnswers = 0;
    }

    public List<DailyMasteryPointDTO> getDailyMasteryHistory(String studentId, String subject, int days) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("studentId is required");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("subject is required");
        }
        if (days <= 0) {
            days = 30;
        }

        StudentProfile profile = studentService.findOrCreateProfile(studentId);
        String canonicalUserId = profile.getUserId() != null ? profile.getUserId() : profile.getId();

        double currentMastery = 50.0;
        if (profile.getConceptMastery() != null) {
            for (Map.Entry<String, Double> entry : profile.getConceptMastery().entrySet()) {
                if (entry.getKey().equalsIgnoreCase(subject) || subject.equalsIgnoreCase(entry.getKey())) {
                    currentMastery = entry.getValue();
                    break;
                }
            }
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        Map<LocalDate, DayStats> dailyActivityMap = new HashMap<>();

        // 1. Gather historical QuizSessions
        List<QuizSession> quizSessions = quizSessionRepository.findByUserId(canonicalUserId);
        if (quizSessions.isEmpty() && profile.getId() != null) {
            quizSessions = quizSessionRepository.findByStudentProfileId(profile.getId());
        }

        for (QuizSession session : quizSessions) {
            if (session.getSubjectName() != null && isSubjectMatch(session.getSubjectName(), subject)) {
                LocalDateTime ts = session.getLastAnswerTime() != null ? session.getLastAnswerTime() : session.getStartTime();
                if (ts != null) {
                    LocalDate date = ts.toLocalDate();
                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        DayStats stats = dailyActivityMap.computeIfAbsent(date, d -> new DayStats());
                        stats.questionsAnswered += session.getTotalQuestions();
                        stats.correctAnswers += session.getCorrectCount();
                    }
                }
            }
        }

        // 2. Gather historical DashboardTestResults
        List<DashboardTestResult> testResults = dashboardTestResultRepository.findByStudentId(studentId);
        if (testResults.isEmpty() && profile.getId() != null) {
            testResults = dashboardTestResultRepository.findByStudentId(profile.getId());
        }
        if (testResults.isEmpty() && canonicalUserId != null) {
            testResults = dashboardTestResultRepository.findByStudentId(canonicalUserId);
        }

        for (DashboardTestResult result : testResults) {
            if (result.getCreatedAt() != null) {
                LocalDate date = result.getCreatedAt().toLocalDate();
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    if (result.getCorrectCountPerSubject() != null) {
                        for (Map.Entry<String, Integer> entry : result.getCorrectCountPerSubject().entrySet()) {
                            if (isSubjectMatch(entry.getKey(), subject)) {
                                DayStats stats = dailyActivityMap.computeIfAbsent(date, d -> new DayStats());
                                int correct = entry.getValue() != null ? entry.getValue() : 0;
                                stats.questionsAnswered += 5; // Dashboard test generates 5 questions per subject
                                stats.correctAnswers += correct;
                            }
                        }
                    }
                }
            }
        }

        // 3. Construct chronological time-series list with carried-forward mastery
        List<DailyMasteryPointDTO> history = new ArrayList<>();
        
        // Estimate starting baseline mastery so that running updates smoothly converge to currentMastery
        double runningMastery = Math.max(30.0, currentMastery - 15.0);

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            DayStats stats = dailyActivityMap.get(date);

            if (stats != null && stats.questionsAnswered > 0) {
                double dailyAccuracy = ((double) stats.correctAnswers / stats.questionsAnswered) * 100.0;
                // Weighted rolling update: 60% previous mastery + 40% daily performance
                runningMastery = (0.6 * runningMastery) + (0.4 * dailyAccuracy);
                history.add(new DailyMasteryPointDTO(
                        date,
                        Math.round(runningMastery * 10.0) / 10.0,
                        stats.questionsAnswered,
                        stats.correctAnswers
                ));
            } else {
                // Carry forward previous known mastery value when no attempts occur on this day
                history.add(new DailyMasteryPointDTO(
                        date,
                        Math.round(runningMastery * 10.0) / 10.0,
                        0,
                        0
                ));
            }
        }

        // Adjust final point to match live current profile mastery if available
        if (!history.isEmpty()) {
            DailyMasteryPointDTO lastPoint = history.get(history.size() - 1);
            lastPoint.setMasteryPercentage(Math.round(currentMastery * 10.0) / 10.0);
        }

        return history;
    }

    public List<AttemptMasteryPointDTO> getPerAttemptMasteryHistory(String studentId, String subject, int days) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("studentId is required");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("subject is required");
        }
        if (days <= 0) {
            days = 30;
        }

        StudentProfile profile = studentService.findOrCreateProfile(studentId);
        String canonicalUserId = profile.getUserId() != null ? profile.getUserId() : profile.getId();

        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        List<AttemptMasteryPointDTO> attempts = new ArrayList<>();

        // 1. Gather QuizSession attempts
        List<QuizSession> quizSessions = quizSessionRepository.findByUserId(canonicalUserId);
        if (quizSessions.isEmpty() && profile.getId() != null) {
            quizSessions = quizSessionRepository.findByStudentProfileId(profile.getId());
        }

        for (QuizSession session : quizSessions) {
            if (session.getSubjectName() != null && isSubjectMatch(session.getSubjectName(), subject)) {
                LocalDateTime ts = session.getLastAnswerTime() != null ? session.getLastAnswerTime() : session.getStartTime();
                if (ts != null && !ts.isBefore(cutoffTime)) {
                    int total = session.getTotalQuestions();
                    int correct = session.getCorrectCount();
                    if (total > 0) {
                        double pct = ((double) correct / total) * 100.0;
                        attempts.add(new AttemptMasteryPointDTO(
                                session.getId(),
                                ts,
                                Math.round(pct * 10.0) / 10.0,
                                total,
                                correct,
                                subject
                        ));
                    }
                }
            }
        }

        // 2. Gather DashboardTestResult attempts
        List<DashboardTestResult> testResults = dashboardTestResultRepository.findByStudentId(studentId);
        if (testResults.isEmpty() && profile.getId() != null) {
            testResults = dashboardTestResultRepository.findByStudentId(profile.getId());
        }
        if (testResults.isEmpty() && canonicalUserId != null) {
            testResults = dashboardTestResultRepository.findByStudentId(canonicalUserId);
        }

        for (DashboardTestResult result : testResults) {
            LocalDateTime ts = result.getCreatedAt();
            if (ts != null && !ts.isBefore(cutoffTime)) {
                if (result.getSubjectScorePercentage() != null) {
                    for (Map.Entry<String, Double> entry : result.getSubjectScorePercentage().entrySet()) {
                        if (isSubjectMatch(entry.getKey(), subject)) {
                            double pct = entry.getValue() != null ? entry.getValue() : 0.0;
                            int correct = 0;
                            if (result.getCorrectCountPerSubject() != null && result.getCorrectCountPerSubject().get(entry.getKey()) != null) {
                                correct = result.getCorrectCountPerSubject().get(entry.getKey());
                            } else {
                                correct = (int) Math.round((pct / 100.0) * 5);
                            }
                            attempts.add(new AttemptMasteryPointDTO(
                                    result.getId(),
                                    ts,
                                    Math.round(pct * 10.0) / 10.0,
                                    5,
                                    correct,
                                    subject
                            ));
                        }
                    }
                }
            }
        }

        // 3. Gather AssessmentResult attempts
        List<AssessmentResult> assessmentResults = assessmentResultRepository.findByUserId(canonicalUserId);
        if (assessmentResults.isEmpty() && profile.getId() != null) {
            assessmentResults = assessmentResultRepository.findByUserId(profile.getId());
        }

        for (AssessmentResult res : assessmentResults) {
            if (res.getSubjectName() != null && isSubjectMatch(res.getSubjectName(), subject)) {
                LocalDateTime ts = res.getCreatedAt();
                if (ts != null && !ts.isBefore(cutoffTime)) {
                    double pct = res.getPercentage() > 0 ? res.getPercentage() : (res.getScore() > 0 ? res.getScore() : 0.0);
                    int total = res.getTotalQuestions() > 0 ? res.getTotalQuestions() : 5;
                    int correct = res.getCorrectAnswers() > 0 ? res.getCorrectAnswers() : (int) Math.round((pct / 100.0) * total);
                    attempts.add(new AttemptMasteryPointDTO(
                            res.getId(),
                            ts,
                            Math.round(pct * 10.0) / 10.0,
                            total,
                            correct,
                            subject
                    ));
                }
            }
        }

        // Sort attempts strictly chronologically by dateTime (oldest attempt first -> newest last)
        attempts.sort(Comparator.comparing(AttemptMasteryPointDTO::getDateTime));

        return attempts;
    }

    private boolean isSubjectMatch(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        String clean1 = s1.trim().toLowerCase();
        String clean2 = s2.trim().toLowerCase();
        return clean1.equals(clean2) || clean1.contains(clean2) || clean2.contains(clean1);
    }
}
