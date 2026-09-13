package com.edupilot.service;

import com.edupilot.dto.EvaluationMetricsResponse;
import com.edupilot.model.*;
import com.edupilot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EvaluationService {

    @Autowired
    private StudentService studentService;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    @Autowired
    private StudentStateSnapshotRepository snapshotRepository;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private StudentSatisfactionRepository satisfactionRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    public EvaluationMetricsResponse calculateEvaluationMetrics(String userId) {
        String resolvedUserId = studentService.resolveUserId(userId);
        StudentProfile profile = studentService.findOrCreateProfile(resolvedUserId);

        // 1. Completion Rate calculation
        List<Recommendation> allRecs = recommendationRepository.findByUserId(resolvedUserId);
        if (allRecs.isEmpty() && profile.getId() != null) {
            allRecs = recommendationRepository.findByUserId(profile.getId());
        }

        int totalRecs = allRecs.size();
        int completedRecs = 0;
        for (Recommendation r : allRecs) {
            if (r.getStatus() == Recommendation.Status.COMPLETED) {
                completedRecs++;
            }
        }

        double completionRate = totalRecs > 0 ? round2((double) completedRecs / totalRecs) : 0.0;

        // 2. Time to Mastery calculation per topic
        List<ConceptMastery> cmList = conceptMasteryRepository.findByUserId(resolvedUserId);
        if (cmList.isEmpty() && profile.getId() != null) {
            cmList = conceptMasteryRepository.findByUserId(profile.getId());
        }

        List<StudentStateSnapshot> snapshots = snapshotRepository.findByStudentIdOrderByTimestampAsc(resolvedUserId);
        List<AssessmentResult> assessments = assessmentResultRepository.findByUserId(resolvedUserId);
        List<QuizSession> quizSessions = quizSessionRepository.findByUserId(resolvedUserId);

        List<EvaluationMetricsResponse.TopicTimeToMasteryDTO> topicMetrics = new ArrayList<>();
        double sumCalculatedMinutes = 0.0;
        int calculatedCount = 0;

        for (ConceptMastery cm : cmList) {
            String topic = cm.getTopic() != null && !cm.getTopic().isEmpty() ? cm.getTopic() : cm.getConceptName();
            if (topic == null || topic.trim().isEmpty()) continue;

            int attempts = cm.getAttemptCount() > 0 ? cm.getAttemptCount() : countAttemptsForTopic(topic, quizSessions);

            LocalDateTime baselineTime = findBaselineTimeForTopic(topic, assessments, quizSessions, snapshots);
            LocalDateTime achievedTime = findAchievedTimeForTopic(topic, snapshots, cm);

            boolean masteryAchieved = achievedTime != null || (cm.getAccuracy() >= 0.80 || cm.getMasteryScore() >= 80.0);

            if (baselineTime != null && achievedTime != null && !achievedTime.isBefore(baselineTime)) {
                long minutes = Duration.between(baselineTime, achievedTime).toMinutes();
                topicMetrics.add(new EvaluationMetricsResponse.TopicTimeToMasteryDTO(
                        topic, 0.80, true, minutes, attempts, "calculated"
                ));
                sumCalculatedMinutes += minutes;
                calculatedCount++;
            } else if (masteryAchieved) {
                LocalDateTime lastAssessed = cm.getLastAssessedAt();
                if (baselineTime != null && lastAssessed != null && !lastAssessed.isBefore(baselineTime)) {
                    long minutes = Duration.between(baselineTime, lastAssessed).toMinutes();
                    topicMetrics.add(new EvaluationMetricsResponse.TopicTimeToMasteryDTO(
                            topic, 0.80, true, minutes, attempts, "calculated"
                    ));
                    sumCalculatedMinutes += minutes;
                    calculatedCount++;
                } else {
                    topicMetrics.add(new EvaluationMetricsResponse.TopicTimeToMasteryDTO(
                            topic, 0.80, true, null, attempts, "insufficient_data"
                    ));
                }
            } else if (baselineTime != null) {
                topicMetrics.add(new EvaluationMetricsResponse.TopicTimeToMasteryDTO(
                        topic, 0.80, false, null, attempts, "in_progress"
                ));
            } else {
                topicMetrics.add(new EvaluationMetricsResponse.TopicTimeToMasteryDTO(
                        topic, 0.80, false, null, attempts, "insufficient_data"
                ));
            }
        }

        Double averageTimeToMastery = calculatedCount > 0 ? round2(sumCalculatedMinutes / calculatedCount) : null;

        // 3. Real Knowledge Retention calculation
        List<EvaluationMetricsResponse.TopicRetentionDTO> topicRetention = new ArrayList<>();
        double sumRetention = 0.0;
        int calculatedRetentionCount = 0;

        for (ConceptMastery cm : cmList) {
            String topic = cm.getTopic() != null && !cm.getTopic().isEmpty() ? cm.getTopic() : cm.getConceptName();
            if (topic == null || topic.trim().isEmpty()) continue;

            LocalDateTime achievementTime = null;
            Double achievementMastery = null;

            for (StudentStateSnapshot s : snapshots) {
                if (s.getTimestamp() != null && s.getTopicMastery() != null && s.getTopicMastery().containsKey(topic)) {
                    double score = s.getTopicMastery().get(topic);
                    if (score >= 0.80) {
                        achievementTime = s.getTimestamp();
                        achievementMastery = score;
                        break;
                    }
                }
            }

            if (achievementTime == null && (cm.getAccuracy() >= 0.80 || cm.getMasteryScore() >= 80.0)) {
                achievementTime = cm.getLastAssessedAt() != null ? cm.getLastAssessedAt() : LocalDateTime.now().minusHours(4);
                achievementMastery = cm.getAccuracy() >= 0.80 ? cm.getAccuracy() : (cm.getMasteryScore() / 100.0);
            }

            LocalDateTime reassessmentTime = null;
            Double reassessmentMastery = null;

            if (achievementTime != null) {
                for (StudentStateSnapshot s : snapshots) {
                    if (s.getTimestamp() != null && s.getTimestamp().isAfter(achievementTime) && s.getTopicMastery() != null && s.getTopicMastery().containsKey(topic)) {
                        reassessmentTime = s.getTimestamp();
                        reassessmentMastery = s.getTopicMastery().get(topic);
                    }
                }

                if (reassessmentTime == null && cm.getLastAssessedAt() != null && cm.getLastAssessedAt().isAfter(achievementTime)) {
                    reassessmentTime = cm.getLastAssessedAt();
                    double cur = cm.getAccuracy() > 0 ? cm.getAccuracy() : (cm.getMasteryScore() / 100.0);
                    reassessmentMastery = cur > 1.0 ? cur / 100.0 : cur;
                }
            }

            if (achievementMastery != null && achievementMastery > 0 && reassessmentMastery != null && reassessmentTime != null) {
                double ret = Math.min(1.0, Math.max(0.0, reassessmentMastery / achievementMastery));
                ret = round4(ret);

                topicRetention.add(new EvaluationMetricsResponse.TopicRetentionDTO(
                        topic,
                        achievementTime.toString(),
                        round2(achievementMastery),
                        reassessmentTime.toString(),
                        round2(reassessmentMastery),
                        ret,
                        "CALCULATED"
                ));

                sumRetention += ret;
                calculatedRetentionCount++;
            } else {
                topicRetention.add(new EvaluationMetricsResponse.TopicRetentionDTO(
                        topic,
                        achievementTime != null ? achievementTime.toString() : null,
                        achievementMastery != null ? round2(achievementMastery) : null,
                        null,
                        null,
                        null,
                        "INSUFFICIENT_DATA"
                ));
            }
        }

        Double overallRetention = calculatedRetentionCount > 0 ? round2(sumRetention / calculatedRetentionCount) : null;

        // 4. Student Satisfaction calculation
        List<StudentSatisfaction> ratings = satisfactionRepository.findByStudentId(resolvedUserId);
        if (ratings.isEmpty() && profile.getId() != null) {
            ratings = satisfactionRepository.findByStudentId(profile.getId());
        }

        Double averageSatisfaction = null;
        int satisfactionResponseCount = 0;

        if (!ratings.isEmpty()) {
            double sumSat = 0.0;
            for (StudentSatisfaction s : ratings) {
                sumSat += s.getRating();
            }
            averageSatisfaction = round2(sumSat / ratings.size());
            satisfactionResponseCount = ratings.size();
        }

        // 5. Inactivity & Dropout Risk calculation
        LocalDateTime maxActivityTime = findLatestActivityTime(resolvedUserId, profile.getId());

        String lastActivityAt = null;
        Long daysSinceLastActivity = null;
        String activityStatus = "NO_ACTIVITY";
        Double dropoutRisk = null;

        if (maxActivityTime != null) {
            lastActivityAt = maxActivityTime.toString();
            long days = Duration.between(maxActivityTime, LocalDateTime.now()).toDays();
            daysSinceLastActivity = Math.max(0L, days);

            if (daysSinceLastActivity <= 7) {
                activityStatus = "ACTIVE";
                dropoutRisk = daysSinceLastActivity == 0 ? 0.0 : round2(Math.min(1.0, daysSinceLastActivity * 0.05 + 0.15));
            } else if (daysSinceLastActivity <= 14) {
                activityStatus = "AT_RISK";
                dropoutRisk = round2(Math.min(1.0, daysSinceLastActivity * 0.05 + 0.15));
            } else {
                activityStatus = "INACTIVE";
                dropoutRisk = round2(Math.min(1.0, daysSinceLastActivity * 0.05 + 0.15));
            }
        }

        // Cohort Dropout Rate calculation
        Double dropoutRate = null;
        try {
            List<StudentProfile> allProfiles = studentProfileRepository.findAll();
            int eligibleCount = 0;
            int inactiveCount = 0;

            for (StudentProfile p : allProfiles) {
                String pUserId = p.getUserId() != null ? p.getUserId() : p.getId();
                if (pUserId == null) continue;

                LocalDateTime pMaxTime = findLatestActivityTime(pUserId, p.getId());
                if (pMaxTime != null) {
                    eligibleCount++;
                    long d = Duration.between(pMaxTime, LocalDateTime.now()).toDays();
                    if (d > 14) {
                        inactiveCount++;
                    }
                }
            }

            if (eligibleCount > 0) {
                dropoutRate = round2((double) inactiveCount / eligibleCount);
            }
        } catch (Exception ex) {
            dropoutRate = null;
        }

        return new EvaluationMetricsResponse(
                resolvedUserId,
                completionRate,
                totalRecs,
                completedRecs,
                averageTimeToMastery,
                topicMetrics,
                overallRetention,
                topicRetention,
                averageSatisfaction,
                satisfactionResponseCount,
                lastActivityAt,
                daysSinceLastActivity,
                activityStatus,
                dropoutRisk,
                dropoutRate
        );
    }

    private LocalDateTime findLatestActivityTime(String userId, String profileId) {
        LocalDateTime maxTime = null;

        try {
            List<StudySession> studySessions = studySessionRepository.findByUserIdOrderByStartTimeDesc(userId);
            if (studySessions.isEmpty() && profileId != null) {
                studySessions = studySessionRepository.findByUserIdOrderByStartTimeDesc(profileId);
            }
            for (StudySession ss : studySessions) {
                LocalDateTime t = ss.getEndTime() != null ? ss.getEndTime() : ss.getStartTime();
                if (t != null && (maxTime == null || t.isAfter(maxTime))) maxTime = t;
            }
        } catch (Exception ignored) {}

        try {
            List<QuizSession> quizSessions = quizSessionRepository.findByUserId(userId);
            if (quizSessions.isEmpty() && profileId != null) {
                quizSessions = quizSessionRepository.findByUserId(profileId);
            }
            for (QuizSession qs : quizSessions) {
                LocalDateTime t = qs.getLastAnswerTime() != null ? qs.getLastAnswerTime() : qs.getStartTime();
                if (t != null && (maxTime == null || t.isAfter(maxTime))) maxTime = t;
            }
        } catch (Exception ignored) {}

        try {
            List<AssessmentResult> assessments = assessmentResultRepository.findByUserId(userId);
            if (assessments.isEmpty() && profileId != null) {
                assessments = assessmentResultRepository.findByUserId(profileId);
            }
            for (AssessmentResult ar : assessments) {
                LocalDateTime t = ar.getCreatedAt();
                if (t != null && (maxTime == null || t.isAfter(maxTime))) maxTime = t;
            }
        } catch (Exception ignored) {}

        try {
            List<StudentStateSnapshot> snapshots = snapshotRepository.findByStudentIdOrderByTimestampAsc(userId);
            for (StudentStateSnapshot s : snapshots) {
                LocalDateTime t = s.getTimestamp();
                if (t != null && (maxTime == null || t.isAfter(maxTime))) maxTime = t;
            }
        } catch (Exception ignored) {}

        try {
            List<ConceptMastery> cmList = conceptMasteryRepository.findByUserId(userId);
            if (cmList.isEmpty() && profileId != null) {
                cmList = conceptMasteryRepository.findByUserId(profileId);
            }
            for (ConceptMastery cm : cmList) {
                LocalDateTime t = cm.getLastAssessedAt();
                if (t != null && (maxTime == null || t.isAfter(maxTime))) maxTime = t;
            }
        } catch (Exception ignored) {}

        try {
            List<StudentSatisfaction> ratings = satisfactionRepository.findByStudentId(userId);
            if (ratings.isEmpty() && profileId != null) {
                ratings = satisfactionRepository.findByStudentId(profileId);
            }
            for (StudentSatisfaction sat : ratings) {
                LocalDateTime t = sat.getTimestamp();
                if (t != null && (maxTime == null || t.isAfter(maxTime))) maxTime = t;
            }
        } catch (Exception ignored) {}

        return maxTime;
    }

    private LocalDateTime findBaselineTimeForTopic(String topic, List<AssessmentResult> assessments, List<QuizSession> quizSessions, List<StudentStateSnapshot> snapshots) {
        LocalDateTime earliest = null;

        for (AssessmentResult ar : assessments) {
            if (ar.getCreatedAt() != null && matchesTopic(topic, ar)) {
                if (earliest == null || ar.getCreatedAt().isBefore(earliest)) {
                    earliest = ar.getCreatedAt();
                }
            }
        }

        for (QuizSession qs : quizSessions) {
            if (qs.getLastAnswerTime() != null && matchesTopic(topic, qs)) {
                if (earliest == null || qs.getLastAnswerTime().isBefore(earliest)) {
                    earliest = qs.getLastAnswerTime();
                }
            }
        }

        for (StudentStateSnapshot s : snapshots) {
            if (s.getTimestamp() != null && s.getTopicMastery() != null && s.getTopicMastery().containsKey(topic)) {
                if (earliest == null || s.getTimestamp().isBefore(earliest)) {
                    earliest = s.getTimestamp();
                }
            }
        }

        return earliest;
    }

    private LocalDateTime findAchievedTimeForTopic(String topic, List<StudentStateSnapshot> snapshots, ConceptMastery cm) {
        for (StudentStateSnapshot s : snapshots) {
            if (s.getTimestamp() != null && s.getTopicMastery() != null) {
                Double score = s.getTopicMastery().get(topic);
                if (score != null && score >= 0.80) {
                    return s.getTimestamp();
                }
            }
        }
        if ((cm.getAccuracy() >= 0.80 || cm.getMasteryScore() >= 80.0) && cm.getLastAssessedAt() != null) {
            return cm.getLastAssessedAt();
        }
        return null;
    }

    private boolean matchesTopic(String topic, AssessmentResult ar) {
        if (ar.getSubjectName() != null && ar.getSubjectName().equalsIgnoreCase(topic)) return true;
        if (ar.getTopicBreakdown() != null && ar.getTopicBreakdown().containsKey(topic)) return true;
        if (ar.getUserAnswers() != null) {
            return ar.getUserAnswers().stream().anyMatch(u -> u.getTopic() != null && u.getTopic().equalsIgnoreCase(topic));
        }
        return false;
    }

    private boolean matchesTopic(String topic, QuizSession qs) {
        if (qs.getSubjectName() != null && qs.getSubjectName().equalsIgnoreCase(topic)) return true;
        if (qs.getTargetConcept() != null && qs.getTargetConcept().equalsIgnoreCase(topic)) return true;
        if (qs.getAnswers() != null) {
            return qs.getAnswers().stream().anyMatch(a -> a.getConcept() != null && a.getConcept().equalsIgnoreCase(topic));
        }
        return false;
    }

    private int countAttemptsForTopic(String topic, List<QuizSession> quizSessions) {
        int count = 0;
        for (QuizSession qs : quizSessions) {
            if (matchesTopic(topic, qs)) {
                count += (qs.getTotalQuestions() > 0 ? qs.getTotalQuestions() : 1);
            }
        }
        return Math.max(1, count);
    }

    private static double round2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static double round4(double val) {
        return BigDecimal.valueOf(val).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
