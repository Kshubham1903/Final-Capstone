package com.edupilot.service;

import com.edupilot.dto.AdminAnalyticsOverviewDTO;
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

    private static double round2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
