package com.edupilot.service;

import com.edupilot.dto.AdminAnalyticsOverviewDTO;
import com.edupilot.dto.AdminCohortAnalyticsDTO;
import com.edupilot.dto.LearningGainResponse;
import com.edupilot.dto.StudentGrowthResponseDTO;
import com.edupilot.model.QuizSession;
import com.edupilot.model.StudentSatisfaction;
import com.edupilot.model.User;
import com.edupilot.repository.AssessmentResultRepository;
import com.edupilot.repository.QuizSessionRepository;
import com.edupilot.repository.StudentSatisfactionRepository;
import com.edupilot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AdminAnalyticsService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private StudentSatisfactionRepository satisfactionRepository;

    @Autowired
    private StudentGrowthService studentGrowthService;

    @Autowired
    private LearningGainService learningGainService;

    @Autowired
    private EvaluationService evaluationService;

    public AdminAnalyticsOverviewDTO getAnalyticsOverview() {
        // 1. Authoritative Student Population from User collection
        List<User> studentUsers = userRepository.findByRole(User.Role.STUDENT);
        Set<String> studentUserIds = new LinkedHashSet<>();
        for (User u : studentUsers) {
            if (u.getId() != null && !u.getId().isBlank()) {
                studentUserIds.add(u.getId());
            }
        }

        int totalStudents = studentUserIds.size();

        // 2. Global Assessment & Quiz Counts
        int totalAssessmentsCompleted = (int) assessmentResultRepository.count();
        int totalQuizzesCompleted = (int) quizSessionRepository.countByStatus(QuizSession.Status.COMPLETED);

        // 3. Satisfaction Ratings
        List<StudentSatisfaction> allRatings = satisfactionRepository.findAll();
        double averageSatisfactionRating = 0.0;
        if (!allRatings.isEmpty()) {
            double sumRating = 0.0;
            for (StudentSatisfaction sat : allRatings) {
                sumRating += sat.getRating();
            }
            averageSatisfactionRating = round2(sumRating / allRatings.size());
        }

        // 4. Student-level aggregated metrics
        int activeStudentsLast7Days = 0;
        int atRiskStudentCount = 0;
        double sumBaseline = 0.0;
        double sumCurrent = 0.0;
        double sumGain = 0.0;
        int validGrowthCount = 0;
        int validGainCount = 0;

        for (String studentId : studentUserIds) {
            // Activity & Risk (Direct latest activity scan without O(N) cohort loop)
            try {
                LocalDateTime maxActivityTime = evaluationService.findLatestActivityTime(studentId, null);
                if (maxActivityTime != null) {
                    long days = Duration.between(maxActivityTime, LocalDateTime.now()).toDays();
                    days = Math.max(0L, days);

                    if (days <= 7) {
                        activeStudentsLast7Days++;
                    } else if (days <= 14) {
                        atRiskStudentCount++;
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to check activity time for student {}: {}", studentId, ex.getMessage());
            }

            // Growth ($K_0$ and $K_t$)
            try {
                StudentGrowthResponseDTO growth = studentGrowthService.calculateStudentGrowth(studentId);
                if (growth != null) {
                    sumBaseline += growth.getBaselineKnowledge();
                    sumCurrent += growth.getCurrentKnowledge();
                    validGrowthCount++;
                }
            } catch (Exception ex) {
                log.warn("Failed to calculate student growth for student {}: {}", studentId, ex.getMessage());
            }

            // Learning Gain ($g$)
            try {
                LearningGainResponse gain = learningGainService.calculateStudentLearningGain(studentId);
                if (gain != null) {
                    sumGain += gain.getOverallLearningGain();
                    validGainCount++;
                }
            } catch (Exception ex) {
                log.warn("Failed to calculate learning gain for student {}: {}", studentId, ex.getMessage());
            }
        }

        double cohortAverageBaselineKnowledge = validGrowthCount > 0 ? round2(sumBaseline / validGrowthCount) : 0.0;
        double cohortAverageCurrentKnowledge = validGrowthCount > 0 ? round2(sumCurrent / validGrowthCount) : 0.0;
        double cohortAverageLearningGain = validGainCount > 0 ? round2(sumGain / validGainCount) : 0.0;

        return new AdminAnalyticsOverviewDTO(
                totalStudents,
                activeStudentsLast7Days,
                totalAssessmentsCompleted,
                totalQuizzesCompleted,
                cohortAverageBaselineKnowledge,
                cohortAverageCurrentKnowledge,
                cohortAverageLearningGain,
                averageSatisfactionRating,
                atRiskStudentCount
        );
    }

    public AdminCohortAnalyticsDTO getCohortAnalytics() {
        // 1. Authoritative Student Population from User collection
        List<User> studentUsers = userRepository.findByRole(User.Role.STUDENT);
        Set<String> studentUserIds = new LinkedHashSet<>();
        for (User u : studentUsers) {
            if (u.getId() != null && !u.getId().isBlank()) {
                studentUserIds.add(u.getId());
            }
        }

        int totalEnrolled = studentUserIds.size();

        // 2. Activity Stratification (Account for all totalEnrolled students)
        int activeLast7Days = 0;
        int atRiskStudents = 0;
        int inactiveStudents = 0;

        // 3. Evaluated Cohort Knowledge & Growth Accumulators
        int evaluatedCohortSize = 0;
        int validGainCount = 0;
        double sumBaseline = 0.0;
        double sumCurrent = 0.0;
        double sumGain = 0.0;

        int improvedCount = 0;
        int unchangedCount = 0;
        int declinedCount = 0;

        for (String studentId : studentUserIds) {
            // A. Activity Classification
            try {
                LocalDateTime maxActivityTime = evaluationService.findLatestActivityTime(studentId, null);
                if (maxActivityTime != null) {
                    long days = Duration.between(maxActivityTime, LocalDateTime.now()).toDays();
                    days = Math.max(0L, days);

                    if (days <= 7) {
                        activeLast7Days++;
                    } else if (days <= 14) {
                        atRiskStudents++;
                    } else {
                        inactiveStudents++;
                    }
                } else {
                    inactiveStudents++;
                }
            } catch (Exception ex) {
                log.warn("Failed to check activity time for student {}: {}", studentId, ex.getMessage());
                inactiveStudents++;
            }

            // B. Authentic Baseline Verification (Only evaluate students with genuine T0 diagnostics)
            Map<String, Double> baselineMap = studentGrowthService.extractBaselineConceptAccuracies(studentId, null);
            if (baselineMap != null && !baselineMap.isEmpty()) {
                try {
                    StudentGrowthResponseDTO growth = studentGrowthService.calculateStudentGrowth(studentId);
                    if (growth != null) {
                        double b = growth.getBaselineKnowledge();
                        double c = growth.getCurrentKnowledge();
                        double delta = c - b;

                        if (delta > 0.0001) {
                            improvedCount++;
                        } else if (delta < -0.0001) {
                            declinedCount++;
                        } else {
                            unchangedCount++;
                        }

                        sumBaseline += b;
                        sumCurrent += c;
                        evaluatedCohortSize++;
                    }
                } catch (Exception ex) {
                    log.warn("Failed to calculate growth for student {}: {}", studentId, ex.getMessage());
                }

                try {
                    LearningGainResponse gain = learningGainService.calculateStudentLearningGain(studentId);
                    if (gain != null && gain.getTopics() != null && !gain.getTopics().isEmpty()) {
                        sumGain += gain.getOverallLearningGain();
                        validGainCount++;
                    }
                } catch (Exception ex) {
                    log.warn("Failed to calculate learning gain for student {}: {}", studentId, ex.getMessage());
                }
            }
        }

        // 4. Means & Growth Distribution Percentages
        double meanBaselineKnowledge = evaluatedCohortSize > 0 ? round2(sumBaseline / evaluatedCohortSize) : 0.0;
        double meanCurrentKnowledge = evaluatedCohortSize > 0 ? round2(sumCurrent / evaluatedCohortSize) : 0.0;
        double meanNormalizedGain = validGainCount > 0 ? round2(sumGain / validGainCount) : 0.0;

        double improvedPercentage = evaluatedCohortSize > 0 ? round2((improvedCount * 100.0) / evaluatedCohortSize) : 0.0;
        double unchangedPercentage = evaluatedCohortSize > 0 ? round2((unchangedCount * 100.0) / evaluatedCohortSize) : 0.0;
        double declinedPercentage = evaluatedCohortSize > 0 ? round2((declinedCount * 100.0) / evaluatedCohortSize) : 0.0;

        AdminCohortAnalyticsDTO.GrowthDistributionDTO growthDistribution =
                new AdminCohortAnalyticsDTO.GrowthDistributionDTO(
                        improvedCount, improvedPercentage,
                        unchangedCount, unchangedPercentage,
                        declinedCount, declinedPercentage
                );

        // 5. Satisfaction Aggregation & Category Breakdown
        List<StudentSatisfaction> allRatings = satisfactionRepository.findAll();
        double averageRating = 0.0;
        int totalReviews = allRatings.size();
        Map<String, Double> byCategory = new LinkedHashMap<>();
        byCategory.put("AI_TUTOR", null);
        byCategory.put("RECOMMENDATION", null);
        byCategory.put("LEARNING_ACTIVITY", null);

        if (!allRatings.isEmpty()) {
            double sumRating = 0.0;
            Map<String, List<Double>> categoryMap = new HashMap<>();
            for (StudentSatisfaction sat : allRatings) {
                sumRating += sat.getRating();
                String cat = sat.getFeedbackType() != null ? sat.getFeedbackType().name() : "LEARNING_ACTIVITY";
                categoryMap.computeIfAbsent(cat, k -> new ArrayList<>()).add((double) sat.getRating());
            }
            averageRating = round2(sumRating / allRatings.size());

            for (String cat : List.of("AI_TUTOR", "RECOMMENDATION", "LEARNING_ACTIVITY")) {
                List<Double> scores = categoryMap.get(cat);
                if (scores != null && !scores.isEmpty()) {
                    double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    byCategory.put(cat, round2(avg));
                } else {
                    byCategory.put(cat, null);
                }
            }
        }

        AdminCohortAnalyticsDTO.SatisfactionDTO satisfaction =
                new AdminCohortAnalyticsDTO.SatisfactionDTO(
                        averageRating, totalReviews, byCategory
                );

        // 6. Data Sufficiency Annotation
        String dataSufficiencyNote = evaluatedCohortSize > 0
                ? "Based on " + evaluatedCohortSize + " student(s) with verified diagnostic baselines."
                : "Insufficient diagnostic data: No enrolled students have completed a baseline assessment.";

        return new AdminCohortAnalyticsDTO(
                totalEnrolled,
                evaluatedCohortSize,
                activeLast7Days,
                atRiskStudents,
                inactiveStudents,
                meanBaselineKnowledge,
                meanCurrentKnowledge,
                meanNormalizedGain,
                growthDistribution,
                satisfaction,
                dataSufficiencyNote
        );
    }

    private static double round2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
