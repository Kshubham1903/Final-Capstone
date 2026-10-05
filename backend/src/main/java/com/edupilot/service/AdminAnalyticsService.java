package com.edupilot.service;

import com.edupilot.dto.AdminAnalyticsFilterCriteria;
import com.edupilot.dto.AdminAnalyticsOverviewDTO;
import com.edupilot.dto.AdminCohortAnalyticsDTO;
import com.edupilot.dto.AdminCohortComparisonDTO;
import com.edupilot.dto.AdminCohortComparisonRequest;
import com.edupilot.dto.AdminCohortSubjectAnalyticsDTO;
import com.edupilot.dto.AdminCohortSubjectAnalyticsDTO.SubjectResearchSummaryDTO;
import com.edupilot.dto.AdminResearchTrendsDTO;
import com.edupilot.dto.AdminResearchTrendsDTO.TrendPointDTO;
import com.edupilot.dto.AdminStudentAnalyticsDTO;
import com.edupilot.dto.AdminStudentAnalyticsDTO.*;
import com.edupilot.dto.AdminStudentDirectoryDTO;
import com.edupilot.dto.LearningGainResponse;
import com.edupilot.dto.StudentGrowthResponseDTO;
import com.edupilot.model.*;
import com.edupilot.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AdminAnalyticsService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private StudentSatisfactionRepository satisfactionRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private RemediationSessionRepository remediationSessionRepository;

    @Autowired
    private StudentStateSnapshotRepository snapshotRepository;

    @Autowired
    private SubjectRoadmapRepository subjectRoadmapRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentGrowthService studentGrowthService;

    @Autowired
    private LearningGainService learningGainService;

    @Autowired
    private EvaluationService evaluationService;

    // ==========================================
    // SHARED FILTERING CORE & POPULATION CONTEXT
    // ==========================================

    public static class FilteredStudentContext {
        public final Set<String> validStudentIds = new LinkedHashSet<>();
        public final Map<String, User> userMap = new LinkedHashMap<>();
        public final Map<String, StudentProfile> profileMap = new HashMap<>();
        public final Map<String, String> profileToUserMap = new HashMap<>();
        public final Map<String, String> activityStatusMap = new HashMap<>();
        public final Map<String, Boolean> baselineMap = new HashMap<>();
    }

    public FilteredStudentContext resolveFilteredPopulation(AdminAnalyticsFilterCriteria criteria) {
        if (criteria != null) {
            criteria.validate();
        }

        FilteredStudentContext ctx = new FilteredStudentContext();

        List<User> studentUsers = userRepository.findByRole(User.Role.STUDENT);
        if (studentUsers == null || studentUsers.isEmpty()) {
            return ctx;
        }

        List<StudentProfile> allProfiles = studentProfileRepository.findAll();
        Map<String, StudentProfile> profilesByUserId = new HashMap<>();
        Map<String, StudentProfile> profilesByEmail = new HashMap<>();

        for (StudentProfile p : allProfiles) {
            if (p.getUserId() != null && !p.getUserId().isBlank()) {
                profilesByUserId.put(p.getUserId(), p);
            }
            if (p.getEmail() != null && !p.getEmail().isBlank()) {
                profilesByEmail.put(p.getEmail(), p);
            }
        }

        for (User u : studentUsers) {
            String userId = u.getId();
            if (userId == null || userId.isBlank()) {
                continue;
            }

            ctx.userMap.put(userId, u);
            StudentProfile profile = profilesByUserId.get(userId);
            if (profile == null && u.getEmail() != null) {
                profile = profilesByEmail.get(u.getEmail());
            }

            String profileId = null;
            String branch = null;
            Integer semester = null;
            if (profile != null) {
                ctx.profileMap.put(userId, profile);
                profileId = profile.getId();
                if (profileId != null) {
                    ctx.profileToUserMap.put(profileId, userId);
                }
                branch = profile.getBranch();
                semester = profile.getSemester();
            }

            // Determine Activity Status
            String activityStatus = "NO_ACTIVITY";
            try {
                LocalDateTime maxActivityTime = evaluationService.findLatestActivityTime(userId, profileId);
                if (maxActivityTime != null) {
                    long days = Duration.between(maxActivityTime, LocalDateTime.now()).toDays();
                    days = Math.max(0L, days);

                    if (days <= 7) {
                        activityStatus = "ACTIVE";
                    } else if (days <= 14) {
                        activityStatus = "AT_RISK";
                    } else {
                        activityStatus = "INACTIVE";
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to check activity status for student {}: {}", userId, ex.getMessage());
            }
            ctx.activityStatusMap.put(userId, activityStatus);

            // Determine Authentic Baseline
            boolean hasAuthenticBaseline = false;
            try {
                Map<String, Double> bMap = studentGrowthService.extractBaselineConceptAccuracies(userId, profileId);
                hasAuthenticBaseline = (bMap != null && !bMap.isEmpty());
            } catch (Exception ex) {
                log.warn("Failed to check baseline for student {}: {}", userId, ex.getMessage());
            }
            ctx.baselineMap.put(userId, hasAuthenticBaseline);

            // Apply Population Filters
            if (criteria != null) {
                // Branch filter
                if (criteria.getBranch() != null && !criteria.getBranch().trim().isEmpty()) {
                    if (branch == null || !branch.trim().equalsIgnoreCase(criteria.getBranch().trim())) {
                        continue;
                    }
                }

                // Semester filter
                if (criteria.getSemester() != null) {
                    if (semester == null || !semester.equals(criteria.getSemester())) {
                        continue;
                    }
                }

                // Activity status filter
                if (criteria.getActivityStatus() != null && !criteria.getActivityStatus().trim().isEmpty()) {
                    if (!activityStatus.equalsIgnoreCase(criteria.getActivityStatus().trim())) {
                        continue;
                    }
                }

                // Baseline filter
                if (criteria.getHasAuthenticBaseline() != null) {
                    if (hasAuthenticBaseline != criteria.getHasAuthenticBaseline()) {
                        continue;
                    }
                }
            }

            ctx.validStudentIds.add(userId);
        }

        return ctx;
    }

    // ==========================================
    // 1. OVERVIEW ANALYTICS
    // ==========================================

    public AdminAnalyticsOverviewDTO getAnalyticsOverview() {
        return getAnalyticsOverview(null);
    }

    public AdminAnalyticsOverviewDTO getAnalyticsOverview(AdminAnalyticsFilterCriteria criteria) {
        FilteredStudentContext ctx = resolveFilteredPopulation(criteria);
        Set<String> studentUserIds = ctx.validStudentIds;
        int totalStudents = studentUserIds.size();

        if (totalStudents == 0) {
            return new AdminAnalyticsOverviewDTO(0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0);
        }

        // 2. Filtered Assessment & Quiz Counts
        int totalAssessmentsCompleted = 0;
        int totalQuizzesCompleted = 0;

        List<AssessmentResult> allAssessments = assessmentResultRepository.findAll();
        for (AssessmentResult ar : allAssessments) {
            String studentId = resolveStudentId(ar.getUserId(), ar.getStudentProfileId(), studentUserIds, ctx.profileToUserMap);
            if (studentId != null) {
                if (criteria != null && !criteria.isDateTimeInRange(ar.getCreatedAt())) {
                    continue;
                }
                if (criteria != null && criteria.hasSubjectFilter() && !matchesSubject(ar.getSubjectCode(), ar.getSubjectName(), criteria.getSubjectCode())) {
                    continue;
                }
                totalAssessmentsCompleted++;
            }
        }

        List<QuizSession> allQuizzes = quizSessionRepository.findAll();
        for (QuizSession qs : allQuizzes) {
            String studentId = resolveStudentId(qs.getUserId(), qs.getStudentProfileId(), studentUserIds, ctx.profileToUserMap);
            if (studentId != null && qs.getStatus() == QuizSession.Status.COMPLETED) {
                LocalDateTime qTime = qs.getLastAnswerTime() != null ? qs.getLastAnswerTime() : qs.getStartTime();
                if (criteria != null && !criteria.isDateTimeInRange(qTime)) {
                    continue;
                }
                if (criteria != null && criteria.hasSubjectFilter() && !matchesSubject(qs.getSubjectCode(), qs.getSubjectName(), criteria.getSubjectCode())) {
                    continue;
                }
                totalQuizzesCompleted++;
            }
        }

        // 3. Satisfaction Ratings
        List<StudentSatisfaction> allRatings = satisfactionRepository.findAll();
        double averageSatisfactionRating = 0.0;
        int validRatingsCount = 0;
        double sumRating = 0.0;

        for (StudentSatisfaction sat : allRatings) {
            String studentId = resolveStudentId(sat.getStudentId(), null, studentUserIds, ctx.profileToUserMap);
            if (studentId != null) {
                if (criteria != null && !criteria.isDateTimeInRange(sat.getTimestamp())) {
                    continue;
                }
                sumRating += sat.getRating();
                validRatingsCount++;
            }
        }

        if (validRatingsCount > 0) {
            averageSatisfactionRating = round2(sumRating / validRatingsCount);
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
            String actStatus = ctx.activityStatusMap.getOrDefault(studentId, "NO_ACTIVITY");
            if ("ACTIVE".equalsIgnoreCase(actStatus)) {
                activeStudentsLast7Days++;
            } else if ("AT_RISK".equalsIgnoreCase(actStatus)) {
                atRiskStudentCount++;
            }

            // Growth ($K_0$ and $K_t$) - K0 lookup is global & immutable
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

    // ==========================================
    // 2. COHORT ANALYTICS
    // ==========================================

    public AdminCohortAnalyticsDTO getCohortAnalytics() {
        return getCohortAnalytics(null);
    }

    public AdminCohortAnalyticsDTO getCohortAnalytics(AdminAnalyticsFilterCriteria criteria) {
        FilteredStudentContext ctx = resolveFilteredPopulation(criteria);
        Set<String> studentUserIds = ctx.validStudentIds;
        int totalEnrolled = studentUserIds.size();

        if (totalEnrolled == 0) {
            Map<String, Double> emptyCategoryMap = new LinkedHashMap<>();
            emptyCategoryMap.put("AI_TUTOR", null);
            emptyCategoryMap.put("RECOMMENDATION", null);
            emptyCategoryMap.put("LEARNING_ACTIVITY", null);

            return new AdminCohortAnalyticsDTO(
                    0, 0, 0, 0, 0,
                    0.0, 0.0, 0.0,
                    new AdminCohortAnalyticsDTO.GrowthDistributionDTO(0, 0.0, 0, 0.0, 0, 0.0),
                    new AdminCohortAnalyticsDTO.SatisfactionDTO(0.0, 0, emptyCategoryMap),
                    "Insufficient diagnostic data: No enrolled students have completed a baseline assessment."
            );
        }

        // 2. Activity Stratification
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
            String actStatus = ctx.activityStatusMap.getOrDefault(studentId, "NO_ACTIVITY");
            if ("ACTIVE".equalsIgnoreCase(actStatus)) {
                activeLast7Days++;
            } else if ("AT_RISK".equalsIgnoreCase(actStatus)) {
                atRiskStudents++;
            } else {
                inactiveStudents++;
            }

            // Authentic Baseline Verification - K0 is globally immutable
            boolean hasBaseline = Boolean.TRUE.equals(ctx.baselineMap.get(studentId));
            if (hasBaseline) {
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
        int totalReviews = 0;
        Map<String, Double> byCategory = new LinkedHashMap<>();
        byCategory.put("AI_TUTOR", null);
        byCategory.put("RECOMMENDATION", null);
        byCategory.put("LEARNING_ACTIVITY", null);

        double sumRating = 0.0;
        Map<String, List<Double>> categoryMap = new HashMap<>();

        for (StudentSatisfaction sat : allRatings) {
            String studentId = resolveStudentId(sat.getStudentId(), null, studentUserIds, ctx.profileToUserMap);
            if (studentId != null) {
                if (criteria != null && !criteria.isDateTimeInRange(sat.getTimestamp())) {
                    continue;
                }
                sumRating += sat.getRating();
                totalReviews++;
                String cat = sat.getFeedbackType() != null ? sat.getFeedbackType().name() : "LEARNING_ACTIVITY";
                categoryMap.computeIfAbsent(cat, k -> new ArrayList<>()).add((double) sat.getRating());
            }
        }

        if (totalReviews > 0) {
            averageRating = round2(sumRating / totalReviews);

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

    // ==========================================
    // 2B. COHORT COMPARISON ANALYTICS
    // ==========================================

    public AdminCohortComparisonDTO compareCohorts(AdminCohortComparisonRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Comparison request body cannot be null.");
        }
        request.validate();

        AdminAnalyticsFilterCriteria criteriaA = request.getCohortA();
        AdminAnalyticsFilterCriteria criteriaB = request.getCohortB();

        AdminCohortComparisonDTO.CohortMetricsDTO metricsA = calculateCohortMetrics(criteriaA);
        AdminCohortComparisonDTO.CohortMetricsDTO metricsB = calculateCohortMetrics(criteriaB);

        // Calculate Differences (Cohort A - Cohort B)
        Integer enrolledDiff = metricsA.getTotalEnrolled() - metricsB.getTotalEnrolled();
        Integer evaluatedDiff = metricsA.getEvaluatedCohortSize() - metricsB.getEvaluatedCohortSize();

        Double baselineDiffPp = (metricsA.getMeanBaselineKnowledge() != null && metricsB.getMeanBaselineKnowledge() != null)
                ? round2(metricsA.getMeanBaselineKnowledge() - metricsB.getMeanBaselineKnowledge())
                : null;

        Double currentDiffPp = (metricsA.getMeanCurrentKnowledge() != null && metricsB.getMeanCurrentKnowledge() != null)
                ? round2(metricsA.getMeanCurrentKnowledge() - metricsB.getMeanCurrentKnowledge())
                : null;

        Double gainDiff = (metricsA.getMeanNormalizedGain() != null && metricsB.getMeanNormalizedGain() != null)
                ? round2(metricsA.getMeanNormalizedGain() - metricsB.getMeanNormalizedGain())
                : null;

        Double satDiff = (metricsA.getSatisfaction() != null && metricsA.getSatisfaction().getTotalReviews() > 0
                && metricsB.getSatisfaction() != null && metricsB.getSatisfaction().getTotalReviews() > 0)
                ? round2(metricsA.getSatisfaction().getAverageRating() - metricsB.getSatisfaction().getAverageRating())
                : null;

        AdminCohortComparisonDTO.DifferencesDTO differences = new AdminCohortComparisonDTO.DifferencesDTO(
                enrolledDiff,
                evaluatedDiff,
                baselineDiffPp,
                currentDiffPp,
                gainDiff,
                satDiff
        );

        // Data Sufficiency Warnings
        List<String> warnings = new ArrayList<>();
        boolean isComparisonValid = true;

        if (metricsA.getTotalEnrolled() == 0) {
            warnings.add("Cohort A has 0 enrolled students.");
            isComparisonValid = false;
        }
        if (metricsB.getTotalEnrolled() == 0) {
            warnings.add("Cohort B has 0 enrolled students.");
            isComparisonValid = false;
        }

        if (metricsA.getTotalEnrolled() > 0 && metricsA.getEvaluatedCohortSize() == 0) {
            warnings.add("Cohort A has no students with authentic diagnostic baselines; learning gain cannot be computed.");
        }
        if (metricsB.getTotalEnrolled() > 0 && metricsB.getEvaluatedCohortSize() == 0) {
            warnings.add("Cohort B has no students with authentic diagnostic baselines; learning gain cannot be computed.");
        }

        if (metricsA.getEvaluatedCohortSize() > 0 && metricsA.getEvaluatedCohortSize() < 5) {
            warnings.add("Cohort A has a small evaluated sample size (N < 5); descriptive averages may be unstable.");
        }
        if (metricsB.getEvaluatedCohortSize() > 0 && metricsB.getEvaluatedCohortSize() < 5) {
            warnings.add("Cohort B has a small evaluated sample size (N < 5); descriptive averages may be unstable.");
        }

        if (metricsA.getEvaluatedCohortSize() >= 5 && metricsB.getEvaluatedCohortSize() >= 5) {
            int maxN = Math.max(metricsA.getEvaluatedCohortSize(), metricsB.getEvaluatedCohortSize());
            int minN = Math.min(metricsA.getEvaluatedCohortSize(), metricsB.getEvaluatedCohortSize());
            if ((double) maxN / minN >= 3.0 || (maxN - minN) >= 20) {
                warnings.add("Cohorts have substantially unequal sample sizes; comparisons should be interpreted with caution.");
            }
        }

        AdminCohortComparisonDTO.DataSufficiencyDTO dataSufficiency = new AdminCohortComparisonDTO.DataSufficiencyDTO(
                isComparisonValid,
                warnings
        );

        String methodologyNote = "Descriptive cohort comparison based on persisted diagnostic assessments and learning activity. Differences may reflect pre-existing cohort characteristics, curriculum differences, missing data, or sample-size differences and should not be interpreted as causal effects.";

        AdminCohortComparisonDTO.CohortDataDTO cohortAData = new AdminCohortComparisonDTO.CohortDataDTO(
                "Cohort A",
                criteriaA,
                metricsA
        );

        AdminCohortComparisonDTO.CohortDataDTO cohortBData = new AdminCohortComparisonDTO.CohortDataDTO(
                "Cohort B",
                criteriaB,
                metricsB
        );

        return new AdminCohortComparisonDTO(
                cohortAData,
                cohortBData,
                differences,
                dataSufficiency,
                methodologyNote
        );
    }

    public AdminCohortComparisonDTO.CohortMetricsDTO calculateCohortMetrics(AdminAnalyticsFilterCriteria criteria) {
        FilteredStudentContext ctx = resolveFilteredPopulation(criteria);
        Set<String> studentUserIds = ctx.validStudentIds;
        int totalEnrolled = studentUserIds.size();

        Map<String, Double> emptyCategoryMap = new LinkedHashMap<>();
        emptyCategoryMap.put("AI_TUTOR", null);
        emptyCategoryMap.put("RECOMMENDATION", null);
        emptyCategoryMap.put("LEARNING_ACTIVITY", null);

        if (totalEnrolled == 0) {
            return new AdminCohortComparisonDTO.CohortMetricsDTO(
                    0, 0, 0, 0, 0,
                    null, null, null,
                    new AdminCohortAnalyticsDTO.GrowthDistributionDTO(0, 0.0, 0, 0.0, 0, 0.0),
                    new AdminCohortAnalyticsDTO.SatisfactionDTO(0.0, 0, emptyCategoryMap),
                    0
            );
        }

        // Check if subject-specific filter is requested
        if (criteria != null && criteria.hasSubjectFilter()) {
            return calculateSubjectFilteredCohortMetrics(criteria, ctx);
        }

        // Global cohort calculation
        int baselineSampleSize = 0;
        int currentKnowledgeSampleSize = 0;
        int learningGainSampleSize = 0;
        int evaluatedCohortSize = 0;

        double sumBaseline = 0.0;
        double sumCurrent = 0.0;
        double sumGain = 0.0;

        int improvedCount = 0;
        int unchangedCount = 0;
        int declinedCount = 0;

        for (String studentId : studentUserIds) {
            boolean hasBaseline = Boolean.TRUE.equals(ctx.baselineMap.get(studentId));
            if (hasBaseline) {
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
                        baselineSampleSize++;
                        currentKnowledgeSampleSize++;
                        evaluatedCohortSize++;
                    }
                } catch (Exception ex) {
                    log.warn("Failed to calculate growth for student {}: {}", studentId, ex.getMessage());
                }

                try {
                    LearningGainResponse gain = learningGainService.calculateStudentLearningGain(studentId);
                    if (gain != null && gain.getTopics() != null && !gain.getTopics().isEmpty()) {
                        sumGain += gain.getOverallLearningGain();
                        learningGainSampleSize++;
                    }
                } catch (Exception ex) {
                    log.warn("Failed to calculate learning gain for student {}: {}", studentId, ex.getMessage());
                }
            } else {
                // Non-baseline student: check if valid persisted current knowledge exists
                try {
                    StudentProfile profile = ctx.profileMap.get(studentId);
                    String profileId = profile != null ? profile.getId() : null;
                    List<ConceptMastery> cmList = conceptMasteryRepository.findByUserId(studentId);
                    if (cmList.isEmpty() && profileId != null) {
                        cmList = conceptMasteryRepository.findByUserId(profileId);
                    }
                    if (!cmList.isEmpty()) {
                        double sumAcc = 0.0;
                        int count = 0;
                        for (ConceptMastery cm : cmList) {
                            double acc = cm.getAccuracy() > 0 ? cm.getAccuracy() : cm.getMasteryScore();
                            sumAcc += acc;
                            count++;
                        }
                        if (count > 0) {
                            sumCurrent += (sumAcc / count);
                            currentKnowledgeSampleSize++;
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Failed to fetch current knowledge for student {}: {}", studentId, ex.getMessage());
                }
            }
        }

        Double meanBaselineKnowledge = baselineSampleSize > 0 ? round2(sumBaseline / baselineSampleSize) : null;
        Double meanCurrentKnowledge = currentKnowledgeSampleSize > 0 ? round2(sumCurrent / currentKnowledgeSampleSize) : null;
        Double meanNormalizedGain = learningGainSampleSize > 0 ? round2(sumGain / learningGainSampleSize) : null;

        int growthTotal = improvedCount + unchangedCount + declinedCount;
        double improvedPercentage = growthTotal > 0 ? round2((improvedCount * 100.0) / growthTotal) : 0.0;
        double unchangedPercentage = growthTotal > 0 ? round2((unchangedCount * 100.0) / growthTotal) : 0.0;
        double declinedPercentage = growthTotal > 0 ? round2((declinedCount * 100.0) / growthTotal) : 0.0;

        AdminCohortAnalyticsDTO.GrowthDistributionDTO growthDistribution =
                new AdminCohortAnalyticsDTO.GrowthDistributionDTO(
                        improvedCount, improvedPercentage,
                        unchangedCount, unchangedPercentage,
                        declinedCount, declinedPercentage
                );

        // Satisfaction Aggregation
        List<StudentSatisfaction> allRatings = satisfactionRepository.findAll();
        double averageRating = 0.0;
        int totalReviews = 0;
        Map<String, Double> byCategory = new LinkedHashMap<>();
        byCategory.put("AI_TUTOR", null);
        byCategory.put("RECOMMENDATION", null);
        byCategory.put("LEARNING_ACTIVITY", null);

        double sumRating = 0.0;
        Map<String, List<Double>> categoryMap = new HashMap<>();

        for (StudentSatisfaction sat : allRatings) {
            String studentId = resolveStudentId(sat.getStudentId(), null, studentUserIds, ctx.profileToUserMap);
            if (studentId != null) {
                if (criteria != null && !criteria.isDateTimeInRange(sat.getTimestamp())) {
                    continue;
                }
                sumRating += sat.getRating();
                totalReviews++;
                String cat = sat.getFeedbackType() != null ? sat.getFeedbackType().name() : "LEARNING_ACTIVITY";
                categoryMap.computeIfAbsent(cat, k -> new ArrayList<>()).add((double) sat.getRating());
            }
        }

        if (totalReviews > 0) {
            averageRating = round2(sumRating / totalReviews);
            for (String cat : List.of("AI_TUTOR", "RECOMMENDATION", "LEARNING_ACTIVITY")) {
                List<Double> scores = categoryMap.get(cat);
                if (scores != null && !scores.isEmpty()) {
                    double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    byCategory.put(cat, round2(avg));
                }
            }
        }

        AdminCohortAnalyticsDTO.SatisfactionDTO satisfaction =
                new AdminCohortAnalyticsDTO.SatisfactionDTO(
                        averageRating, totalReviews, byCategory
                );

        return new AdminCohortComparisonDTO.CohortMetricsDTO(
                totalEnrolled,
                evaluatedCohortSize,
                baselineSampleSize,
                currentKnowledgeSampleSize,
                learningGainSampleSize,
                meanBaselineKnowledge,
                meanCurrentKnowledge,
                meanNormalizedGain,
                growthDistribution,
                satisfaction,
                totalReviews
        );
    }

    private AdminCohortComparisonDTO.CohortMetricsDTO calculateSubjectFilteredCohortMetrics(
            AdminAnalyticsFilterCriteria criteria,
            FilteredStudentContext ctx
    ) {
        String filterSubjectCode = criteria.getSubjectCode().trim();
        Set<String> studentUserIds = ctx.validStudentIds;

        List<AssessmentResult> allAssessments = assessmentResultRepository.findAll();
        Map<String, List<AssessmentResult>> assessmentsByStudent = new HashMap<>();
        for (AssessmentResult ar : allAssessments) {
            String studentId = resolveStudentId(ar.getUserId(), ar.getStudentProfileId(), studentUserIds, ctx.profileToUserMap);
            if (studentId != null) {
                assessmentsByStudent.computeIfAbsent(studentId, k -> new ArrayList<>()).add(ar);
            }
        }

        Map<String, AssessmentResult> earliestDiagnosticByStudent = new HashMap<>();
        for (String studentId : studentUserIds) {
            List<AssessmentResult> sAssessments = assessmentsByStudent.get(studentId);
            if (sAssessments != null && !sAssessments.isEmpty()) {
                List<AssessmentResult> sortedAssessments = new ArrayList<>(sAssessments);
                sortedAssessments.sort(Comparator.comparing(AssessmentResult::getCreatedAt));
                for (AssessmentResult ar : sortedAssessments) {
                    if (ar.getTotalQuestions() > 0 || ar.getPercentage() > 0 || ar.getScore() > 0) {
                        earliestDiagnosticByStudent.put(studentId, ar);
                        break;
                    }
                }
            }
        }

        List<ConceptMastery> allConceptMastery = conceptMasteryRepository.findAll();
        Map<String, List<ConceptMastery>> masteryByStudent = new HashMap<>();
        for (ConceptMastery cm : allConceptMastery) {
            String studentId = resolveStudentId(cm.getUserId(), cm.getStudentProfileId(), studentUserIds, ctx.profileToUserMap);
            if (studentId != null) {
                masteryByStudent.computeIfAbsent(studentId, k -> new ArrayList<>()).add(cm);
            }
        }

        Set<String> enrolledSubjectStudents = new HashSet<>();
        List<Double> validBaselines = new ArrayList<>();
        List<Double> validCurrentKnowledge = new ArrayList<>();
        List<Double> validGains = new ArrayList<>();

        int improvedCount = 0;
        int unchangedCount = 0;
        int declinedCount = 0;

        for (String studentId : studentUserIds) {
            boolean hasSubjectAssociation = false;

            // Check profile enrollment
            StudentProfile sp = ctx.profileMap.get(studentId);
            if (sp != null && sp.getSubjects() != null) {
                for (String subj : sp.getSubjects()) {
                    if (matchesSubject(null, subj, filterSubjectCode)) {
                        hasSubjectAssociation = true;
                        break;
                    }
                }
            }

            // Check authentic subject baseline K0
            Double studentSubjectK0 = null;
            AssessmentResult earliestDiag = earliestDiagnosticByStudent.get(studentId);
            if (earliestDiag != null && matchesSubject(earliestDiag.getSubjectCode(), earliestDiag.getSubjectName(), filterSubjectCode)) {
                double pct = earliestDiag.getPercentage() > 0 ? earliestDiag.getPercentage() :
                        (earliestDiag.getScore() > 0 && earliestDiag.getTotalMarks() > 0 ? (earliestDiag.getScore() * 100.0 / earliestDiag.getTotalMarks()) : 0.0);
                if (pct <= 1.0 && pct > 0) pct *= 100.0;
                studentSubjectK0 = round2(pct);
                hasSubjectAssociation = true;
            }

            // Check current subject knowledge Kt
            Double studentSubjectKt = null;
            List<ConceptMastery> studentCms = masteryByStudent.get(studentId);
            if (studentCms != null && !studentCms.isEmpty()) {
                List<ConceptMastery> subjCms = studentCms.stream()
                        .filter(cm -> matchesSubject(cm.getSubjectCode(), cm.getSubjectName(), filterSubjectCode))
                        .collect(Collectors.toList());
                if (!subjCms.isEmpty()) {
                    hasSubjectAssociation = true;
                    double sumAcc = 0.0;
                    for (ConceptMastery cm : subjCms) {
                        double acc = cm.getAccuracy() > 0 ? cm.getAccuracy() : cm.getMasteryScore();
                        sumAcc += acc;
                    }
                    studentSubjectKt = round2(sumAcc / subjCms.size());
                }
            }

            // Also check assessments for this subject
            List<AssessmentResult> sArs = assessmentsByStudent.get(studentId);
            if (sArs != null) {
                for (AssessmentResult ar : sArs) {
                    if (matchesSubject(ar.getSubjectCode(), ar.getSubjectName(), filterSubjectCode)) {
                        hasSubjectAssociation = true;
                        break;
                    }
                }
            }

            if (hasSubjectAssociation) {
                enrolledSubjectStudents.add(studentId);
            }

            if (studentSubjectK0 != null) {
                validBaselines.add(studentSubjectK0);
            }
            if (studentSubjectKt != null) {
                validCurrentKnowledge.add(studentSubjectKt);
            }

            // Gain computed ONLY when BOTH authentic K0 and valid Kt exist
            if (studentSubjectK0 != null && studentSubjectKt != null) {
                double delta = studentSubjectKt - studentSubjectK0;
                if (delta > 0.0001) {
                    improvedCount++;
                } else if (delta < -0.0001) {
                    declinedCount++;
                } else {
                    unchangedCount++;
                }
                double gain = round2(LearningGainService.computeGain(studentSubjectK0 / 100.0, studentSubjectKt / 100.0));
                validGains.add(gain);
            }
        }

        int totalEnrolled = enrolledSubjectStudents.size();
        int evaluatedCohortSize = validBaselines.size();
        int baselineSampleSize = validBaselines.size();
        int currentKnowledgeSampleSize = validCurrentKnowledge.size();
        int learningGainSampleSize = validGains.size();

        Double meanBaselineKnowledge = baselineSampleSize > 0
                ? round2(validBaselines.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                : null;
        Double meanCurrentKnowledge = currentKnowledgeSampleSize > 0
                ? round2(validCurrentKnowledge.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                : null;
        Double meanNormalizedGain = learningGainSampleSize > 0
                ? round2(validGains.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                : null;

        int growthTotal = improvedCount + unchangedCount + declinedCount;
        double improvedPercentage = growthTotal > 0 ? round2((improvedCount * 100.0) / growthTotal) : 0.0;
        double unchangedPercentage = growthTotal > 0 ? round2((unchangedCount * 100.0) / growthTotal) : 0.0;
        double declinedPercentage = growthTotal > 0 ? round2((declinedCount * 100.0) / growthTotal) : 0.0;

        AdminCohortAnalyticsDTO.GrowthDistributionDTO growthDistribution =
                new AdminCohortAnalyticsDTO.GrowthDistributionDTO(
                        improvedCount, improvedPercentage,
                        unchangedCount, unchangedPercentage,
                        declinedCount, declinedPercentage
                );

        // Satisfaction for subject-associated students
        List<StudentSatisfaction> allRatings = satisfactionRepository.findAll();
        double averageRating = 0.0;
        int totalReviews = 0;
        Map<String, Double> byCategory = new LinkedHashMap<>();
        byCategory.put("AI_TUTOR", null);
        byCategory.put("RECOMMENDATION", null);
        byCategory.put("LEARNING_ACTIVITY", null);

        double sumRating = 0.0;
        Map<String, List<Double>> categoryMap = new HashMap<>();

        for (StudentSatisfaction sat : allRatings) {
            String studentId = resolveStudentId(sat.getStudentId(), null, enrolledSubjectStudents, ctx.profileToUserMap);
            if (studentId != null) {
                if (criteria != null && !criteria.isDateTimeInRange(sat.getTimestamp())) {
                    continue;
                }
                sumRating += sat.getRating();
                totalReviews++;
                String cat = sat.getFeedbackType() != null ? sat.getFeedbackType().name() : "LEARNING_ACTIVITY";
                categoryMap.computeIfAbsent(cat, k -> new ArrayList<>()).add((double) sat.getRating());
            }
        }

        if (totalReviews > 0) {
            averageRating = round2(sumRating / totalReviews);
            for (String cat : List.of("AI_TUTOR", "RECOMMENDATION", "LEARNING_ACTIVITY")) {
                List<Double> scores = categoryMap.get(cat);
                if (scores != null && !scores.isEmpty()) {
                    double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    byCategory.put(cat, round2(avg));
                }
            }
        }

        AdminCohortAnalyticsDTO.SatisfactionDTO satisfaction =
                new AdminCohortAnalyticsDTO.SatisfactionDTO(
                        averageRating, totalReviews, byCategory
                );

        return new AdminCohortComparisonDTO.CohortMetricsDTO(
                totalEnrolled,
                evaluatedCohortSize,
                baselineSampleSize,
                currentKnowledgeSampleSize,
                learningGainSampleSize,
                meanBaselineKnowledge,
                meanCurrentKnowledge,
                meanNormalizedGain,
                growthDistribution,
                satisfaction,
                totalReviews
        );
    }

    // ==========================================
    // 3. STUDENT DIRECTORY
    // ==========================================

    public List<AdminStudentDirectoryDTO> getStudentDirectory() {
        return getStudentDirectory(null);
    }

    public List<AdminStudentDirectoryDTO> getStudentDirectory(AdminAnalyticsFilterCriteria criteria) {
        FilteredStudentContext ctx = resolveFilteredPopulation(criteria);
        if (ctx.validStudentIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<AdminStudentDirectoryDTO> directory = new ArrayList<>();

        for (String userId : ctx.validStudentIds) {
            User user = ctx.userMap.get(userId);
            if (user == null) {
                continue;
            }

            String email = user.getEmail();
            String fullName = user.getFullName();

            StudentProfile profile = ctx.profileMap.get(userId);
            String branch = null;
            Integer semester = null;
            String profileId = null;

            if (profile != null) {
                profileId = profile.getId();
                branch = profile.getBranch();
                semester = profile.getSemester();
                if ((fullName == null || fullName.isBlank()) && profile.getFullName() != null) {
                    fullName = profile.getFullName();
                }
            }

            // If subjectCode filter is specified, verify student is associated with this subject
            if (criteria != null && criteria.hasSubjectFilter()) {
                boolean hasSubjectAssociation = false;
                if (profile != null && profile.getSubjects() != null) {
                    for (String subj : profile.getSubjects()) {
                        if (matchesSubject(null, subj, criteria.getSubjectCode())) {
                            hasSubjectAssociation = true;
                            break;
                        }
                    }
                }
                if (!hasSubjectAssociation) {
                    List<ConceptMastery> cms = conceptMasteryRepository.findByUserId(userId);
                    for (ConceptMastery cm : cms) {
                        if (matchesSubject(cm.getSubjectCode(), cm.getSubjectName(), criteria.getSubjectCode())) {
                            hasSubjectAssociation = true;
                            break;
                        }
                    }
                }
                if (!hasSubjectAssociation) {
                    continue;
                }
            }

            String activityStatus = ctx.activityStatusMap.getOrDefault(userId, "NO_ACTIVITY");
            boolean hasAuthenticBaseline = Boolean.TRUE.equals(ctx.baselineMap.get(userId));
            Double baselineKnowledge = null;
            Double currentKnowledge = null;
            Double growthPp = null;

            if (hasAuthenticBaseline) {
                try {
                    StudentGrowthResponseDTO growth = studentGrowthService.calculateStudentGrowth(userId);
                    if (growth != null) {
                        baselineKnowledge = round2(growth.getBaselineKnowledge());
                        currentKnowledge = round2(growth.getCurrentKnowledge());
                        growthPp = round2(growth.getCumulativeGrowth());
                    }
                } catch (Exception ex) {
                    log.warn("Failed to calculate student growth for student {}: {}", userId, ex.getMessage());
                }
            } else {
                try {
                    List<ConceptMastery> cmList = conceptMasteryRepository.findByUserId(userId);
                    if (cmList.isEmpty() && profileId != null) {
                        cmList = conceptMasteryRepository.findByUserId(profileId);
                    }
                    if (!cmList.isEmpty()) {
                        double sumAcc = 0.0;
                        int count = 0;
                        for (ConceptMastery cm : cmList) {
                            double acc = cm.getAccuracy() > 0 ? cm.getAccuracy() : cm.getMasteryScore();
                            sumAcc += acc;
                            count++;
                        }
                        if (count > 0) {
                            currentKnowledge = round2(sumAcc / count);
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Failed to fetch concept mastery for student {}: {}", userId, ex.getMessage());
                }
            }

            directory.add(new AdminStudentDirectoryDTO(
                    userId,
                    fullName,
                    email,
                    branch,
                    semester,
                    activityStatus,
                    hasAuthenticBaseline,
                    baselineKnowledge,
                    currentKnowledge,
                    growthPp
            ));
        }

        return directory;
    }

    // ==========================================
    // 4. INDIVIDUAL STUDENT ANALYTICS
    // ==========================================

    public AdminStudentAnalyticsDTO getIndividualStudentAnalytics(String rawUserId) {
        if (rawUserId == null || rawUserId.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        String canonicalUserId = studentService != null ? studentService.resolveUserId(rawUserId) : rawUserId.trim();
        Optional<User> userOpt = userRepository.findById(canonicalUserId);
        if (userOpt.isEmpty() && rawUserId.contains("@")) {
            userOpt = userRepository.findByEmail(rawUserId.trim());
        }
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Student not found for ID: " + rawUserId);
        }

        User user = userOpt.get();
        if (user.getRole() != User.Role.STUDENT) {
            throw new IllegalArgumentException("User with ID " + rawUserId + " is not a STUDENT.");
        }

        canonicalUserId = user.getId();
        String email = user.getEmail();

        // 1. Profile Resolution
        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(canonicalUserId);
        if (profileOpt.isEmpty() && email != null) {
            profileOpt = studentProfileRepository.findByEmail(email);
        }
        StudentProfile profile = profileOpt.orElse(null);
        String profileId = profile != null ? profile.getId() : null;

        String fullName = user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : (profile != null && profile.getFullName() != null ? profile.getFullName() : "");
        String branch = profile != null ? profile.getBranch() : null;
        Integer semester = profile != null ? profile.getSemester() : null;
        String institution = profile != null ? profile.getInstitution() : null;
        String degree = profile != null ? profile.getDegree() : null;
        String careerGoals = profile != null && profile.getCareerGoals() != null && !profile.getCareerGoals().isEmpty()
                ? String.join(", ", profile.getCareerGoals())
                : null;
        String learningStyle = profile != null ? profile.getLearningStyle() : null;
        List<String> enrolledSubjects = profile != null && profile.getSubjects() != null ? profile.getSubjects() : Collections.emptyList();

        StudentInfoDTO studentDto = new StudentInfoDTO(
                canonicalUserId, fullName, email, branch, semester, institution, degree, careerGoals, learningStyle, enrolledSubjects
        );

        // 2. Knowledge Metrics & Baseline Check
        Map<String, Double> baselineMap = studentGrowthService.extractBaselineConceptAccuracies(canonicalUserId, profileId);
        boolean hasAuthenticBaseline = baselineMap != null && !baselineMap.isEmpty();

        Double baselineK = null;
        Double currentK = null;
        Double growthPp = null;
        Double normalizedGain = null;

        if (hasAuthenticBaseline) {
            try {
                StudentGrowthResponseDTO growth = studentGrowthService.calculateStudentGrowth(canonicalUserId);
                if (growth != null) {
                    baselineK = round2(growth.getBaselineKnowledge());
                    currentK = round2(growth.getCurrentKnowledge());
                    growthPp = round2(growth.getCumulativeGrowth());
                }
            } catch (Exception ex) {
                log.warn("Failed to calculate student growth for {}: {}", canonicalUserId, ex.getMessage());
            }

            try {
                LearningGainResponse gain = learningGainService.calculateStudentLearningGain(canonicalUserId);
                if (gain != null && gain.getTopics() != null && !gain.getTopics().isEmpty()) {
                    normalizedGain = round2(gain.getOverallLearningGain());
                }
            } catch (Exception ex) {
                log.warn("Failed to calculate learning gain for {}: {}", canonicalUserId, ex.getMessage());
            }
        } else {
            try {
                List<ConceptMastery> cms = conceptMasteryRepository.findByUserId(canonicalUserId);
                if (cms.isEmpty() && profileId != null) {
                    cms = conceptMasteryRepository.findByUserId(profileId);
                }
                if (!cms.isEmpty()) {
                    double sum = 0.0;
                    int cnt = 0;
                    for (ConceptMastery cm : cms) {
                        double acc = cm.getAccuracy() > 0 ? cm.getAccuracy() : cm.getMasteryScore();
                        sum += acc;
                        cnt++;
                    }
                    if (cnt > 0) currentK = round2(sum / cnt);
                }
            } catch (Exception ex) {
                log.warn("Failed to query concept mastery for {}: {}", canonicalUserId, ex.getMessage());
            }
        }

        KnowledgeGrowthDTO knowledgeDto = new KnowledgeGrowthDTO(hasAuthenticBaseline, baselineK, currentK, growthPp, normalizedGain);

        // 3. Assessment Summary & History
        List<AssessmentResult> arList = assessmentResultRepository.findByUserIdOrderByCreatedAtDesc(canonicalUserId);
        if (arList.isEmpty() && profileId != null) {
            arList = assessmentResultRepository.findByUserIdOrderByCreatedAtDesc(profileId);
        }

        int totalAssessments = arList.size();
        int completedAssessments = (int) arList.stream()
                .filter(ar -> ar.getTotalQuestions() > 0 || ar.getPercentage() > 0 || ar.getScore() > 0)
                .count();

        LocalDateTime baselineDate = null;
        if (hasAuthenticBaseline && !arList.isEmpty()) {
            List<AssessmentResult> ascAr = new ArrayList<>(arList);
            ascAr.sort(Comparator.comparing(AssessmentResult::getCreatedAt));
            for (AssessmentResult ar : ascAr) {
                if (ar.getTotalQuestions() > 0 || ar.getPercentage() > 0 || ar.getScore() > 0) {
                    baselineDate = ar.getCreatedAt();
                    break;
                }
            }
        }

        LocalDateTime latestAssessmentDate = !arList.isEmpty() ? arList.get(0).getCreatedAt() : null;
        Double latestAssessmentScore = !arList.isEmpty() ? (double) arList.get(0).getScore() : null;
        Double latestAssessmentPercentage = !arList.isEmpty() ? round2(arList.get(0).getPercentage()) : null;

        AssessmentSummaryDTO assessmentSummaryDto = new AssessmentSummaryDTO(
                totalAssessments, completedAssessments, baselineDate, latestAssessmentDate, latestAssessmentScore, latestAssessmentPercentage
        );

        List<AssessmentRecordDTO> assessmentHistory = new ArrayList<>();
        for (AssessmentResult ar : arList) {
            double pct = ar.getPercentage() > 0 ? ar.getPercentage() : (ar.getScore() > 0 ? ar.getScore() : 0.0);
            if (pct <= 1.0) pct *= 100.0;
            double acc = ar.getAccuracy() > 0 ? ar.getAccuracy() : pct;

            assessmentHistory.add(new AssessmentRecordDTO(
                    ar.getId(),
                    ar.getSessionId(),
                    ar.getSubjectCode(),
                    ar.getSubjectName(),
                    "DIAGNOSTIC",
                    "COMPLETED",
                    ar.getScore(),
                    ar.getTotalMarks(),
                    round2(pct),
                    round2(acc),
                    ar.getMasteryLevel() != null ? ar.getMasteryLevel() : "NOVICE",
                    ar.getTotalQuestions(),
                    ar.getCorrectAnswers(),
                    ar.getIncorrectAnswers(),
                    ar.getSkippedQuestions(),
                    ar.getTopicBreakdown(),
                    ar.getCreatedAt()
            ));
        }

        // 4. Quiz Summary & History
        List<QuizSession> qsList = quizSessionRepository.findByUserIdOrderByLastAnswerTimeDesc(canonicalUserId);
        if (qsList.isEmpty() && profileId != null) {
            qsList = quizSessionRepository.findByUserIdOrderByLastAnswerTimeDesc(profileId);
        }

        List<QuizSession> completedQuizzes = qsList.stream()
                .filter(qs -> qs.getStatus() == QuizSession.Status.COMPLETED)
                .collect(Collectors.toList());

        int totalCompletedQuizzes = completedQuizzes.size();
        int totalQuizQuestions = completedQuizzes.stream().mapToInt(QuizSession::getTotalQuestions).sum();
        int totalQuizCorrect = completedQuizzes.stream().mapToInt(QuizSession::getCorrectCount).sum();
        double quizAccuracy = totalQuizQuestions > 0 ? round2((double) totalQuizCorrect * 100.0 / totalQuizQuestions) : 0.0;

        QuizSummaryDTO quizSummaryDto = new QuizSummaryDTO(
                totalCompletedQuizzes, totalQuizQuestions, totalQuizCorrect, quizAccuracy
        );

        List<QuizRecordDTO> quizHistory = new ArrayList<>();
        for (QuizSession qs : qsList) {
            double acc = qs.getTotalQuestions() > 0 ? round2((double) qs.getCorrectCount() * 100.0 / qs.getTotalQuestions()) : 0.0;
            quizHistory.add(new QuizRecordDTO(
                    qs.getId(),
                    qs.getSubjectCode(),
                    qs.getSubjectName(),
                    qs.getModuleType().name(),
                    qs.isVerificationQuiz(),
                    qs.getTargetConcept(),
                    qs.getTotalQuestions(),
                    qs.getCorrectCount(),
                    qs.getIncorrectCount(),
                    acc,
                    qs.getStatus() != null ? qs.getStatus().name() : "IN_PROGRESS",
                    qs.getStartTime(),
                    qs.getLastAnswerTime()
            ));
        }

        // 5. Concept Mastery Records
        List<ConceptMastery> cmList = conceptMasteryRepository.findByUserId(canonicalUserId);
        if (cmList.isEmpty() && profileId != null) {
            cmList = conceptMasteryRepository.findByUserId(profileId);
        }

        List<ConceptMasteryRecordDTO> conceptMasteryRecords = new ArrayList<>();
        for (ConceptMastery cm : cmList) {
            conceptMasteryRecords.add(new ConceptMasteryRecordDTO(
                    cm.getId(),
                    cm.getSubjectCode(),
                    cm.getSubjectName(),
                    cm.getTopic(),
                    cm.getConceptName(),
                    cm.getMasteryLevel() != null ? cm.getMasteryLevel().name() : "UNKNOWN",
                    cm.getStatus() != null ? cm.getStatus().name() : "UNASSESSED",
                    round2(cm.getAccuracy()),
                    round2(cm.getConfidenceScore()),
                    cm.getAttemptCount(),
                    cm.getCorrectCount(),
                    cm.getWrongCount(),
                    round2(cm.getMasteryScore()),
                    cm.getLastAssessedAt()
            ));
        }

        // 6. Subject Performance
        Map<String, List<ConceptMastery>> subjectGroup = cmList.stream()
                .filter(cm -> cm.getSubjectName() != null && !cm.getSubjectName().isBlank())
                .collect(Collectors.groupingBy(ConceptMastery::getSubjectName, LinkedHashMap::new, Collectors.toList()));

        if (subjectGroup.isEmpty() && profile != null && profile.getSubjects() != null) {
            for (String subj : profile.getSubjects()) {
                subjectGroup.put(subj, Collections.emptyList());
            }
        }

        List<SubjectPerformanceDTO> subjectPerformance = new ArrayList<>();
        for (Map.Entry<String, List<ConceptMastery>> entry : subjectGroup.entrySet()) {
            String subject = entry.getKey();
            List<ConceptMastery> cms = entry.getValue();

            int totalConcepts = cms.size();
            int weakConcepts = (int) cms.stream()
                    .filter(cm -> cm.getStatus() == ConceptMastery.ConceptStatus.WEAK || cm.getAccuracy() < 70.0)
                    .count();

            Double currentScore = null;
            if (totalConcepts > 0) {
                currentScore = round2(cms.stream()
                        .mapToDouble(cm -> cm.getAccuracy() > 0 ? cm.getAccuracy() : cm.getMasteryScore())
                        .average()
                        .orElse(0.0));
            } else if (profile != null && profile.getConceptMastery() != null && profile.getConceptMastery().containsKey(subject)) {
                currentScore = round2(profile.getConceptMastery().get(subject));
            }

            Double baselineScore = null;
            Double growthSubj = null;
            Double gainSubj = null;

            if (hasAuthenticBaseline) {
                baselineScore = findSubjectBaseline(subject, arList, baselineMap);
                if (baselineScore != null && currentScore != null) {
                    growthSubj = round2(currentScore - baselineScore);
                    gainSubj = round2(LearningGainService.computeGain(baselineScore / 100.0, currentScore / 100.0));
                }
            }

            // Roadmap progress
            Double roadmapProgress = null;
            String subjectCode = !cms.isEmpty() && cms.get(0).getSubjectCode() != null ? cms.get(0).getSubjectCode() : subject;
            try {
                Optional<SubjectRoadmap> roadmapOpt = subjectRoadmapRepository.findByUserIdAndSubjectCode(canonicalUserId, subjectCode);
                if (roadmapOpt.isEmpty() && profileId != null) {
                    roadmapOpt = subjectRoadmapRepository.findByUserIdAndSubjectCode(profileId, subjectCode);
                }
                if (roadmapOpt.isPresent() && roadmapOpt.get().getTopics() != null && !roadmapOpt.get().getTopics().isEmpty()) {
                    long comp = roadmapOpt.get().getTopics().stream().filter(SubjectRoadmap.RoadmapTopicNode::isCompleted).count();
                    roadmapProgress = round2((double) comp * 100.0 / roadmapOpt.get().getTopics().size());
                }
            } catch (Exception ex) {
                log.warn("Failed to query roadmap for subject {}: {}", subject, ex.getMessage());
            }

            subjectPerformance.add(new SubjectPerformanceDTO(
                    subjectCode,
                    subject,
                    baselineScore,
                    currentScore,
                    growthSubj,
                    gainSubj,
                    totalConcepts,
                    weakConcepts,
                    roadmapProgress
            ));
        }

        // 7. Learning Activity
        String activityStatus = "NO_ACTIVITY";
        LocalDateTime lastActivityAt = null;
        Long daysSinceLastActivity = null;

        try {
            LocalDateTime maxActivityTime = evaluationService.findLatestActivityTime(canonicalUserId, profileId);
            if (maxActivityTime != null) {
                lastActivityAt = maxActivityTime;
                long days = Duration.between(maxActivityTime, LocalDateTime.now()).toDays();
                days = Math.max(0L, days);
                daysSinceLastActivity = days;

                if (days <= 7) {
                    activityStatus = "ACTIVE";
                } else if (days <= 14) {
                    activityStatus = "AT_RISK";
                } else {
                    activityStatus = "INACTIVE";
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to check activity time for {}: {}", canonicalUserId, ex.getMessage());
        }

        List<StudySession> studySessions = studySessionRepository.findByUserIdOrderByStartTimeDesc(canonicalUserId);
        if (studySessions.isEmpty() && profileId != null) {
            studySessions = studySessionRepository.findByUserIdOrderByStartTimeDesc(profileId);
        }
        int totalStudySessions = studySessions.size();
        int totalStudyMinutes = studySessions.stream().mapToInt(StudySession::getActualDurationMinutes).sum();

        ActivityDTO activityDto = new ActivityDTO(
                activityStatus, lastActivityAt, daysSinceLastActivity, totalStudySessions, totalStudyMinutes
        );

        // 8. Satisfaction Ratings & Feedback
        List<StudentSatisfaction> ratings = satisfactionRepository.findByStudentId(canonicalUserId);
        if (ratings.isEmpty() && profileId != null) {
            ratings = satisfactionRepository.findByStudentId(profileId);
        }

        int totalReviews = ratings.size();
        Double averageRating = null;
        Double latestRating = null;
        Map<String, Double> byCategory = new LinkedHashMap<>();
        byCategory.put("AI_TUTOR", null);
        byCategory.put("RECOMMENDATION", null);
        byCategory.put("LEARNING_ACTIVITY", null);

        List<SatisfactionReviewDTO> reviews = new ArrayList<>();

        if (!ratings.isEmpty()) {
            double sumSat = 0.0;
            Map<String, List<Double>> catMap = new HashMap<>();
            for (StudentSatisfaction sat : ratings) {
                sumSat += sat.getRating();
                String cat = sat.getFeedbackType() != null ? sat.getFeedbackType().name() : "LEARNING_ACTIVITY";
                catMap.computeIfAbsent(cat, k -> new ArrayList<>()).add((double) sat.getRating());

                reviews.add(new SatisfactionReviewDTO(
                        sat.getRating(),
                        cat,
                        sat.getComment(),
                        sat.getTimestamp()
                ));
            }

            averageRating = round2(sumSat / totalReviews);
            latestRating = (double) ratings.get(0).getRating();

            for (String cat : List.of("AI_TUTOR", "RECOMMENDATION", "LEARNING_ACTIVITY")) {
                List<Double> scores = catMap.get(cat);
                if (scores != null && !scores.isEmpty()) {
                    double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    byCategory.put(cat, round2(avg));
                } else {
                    byCategory.put(cat, null);
                }
            }
        }

        SatisfactionSummaryDTO satisfactionDto = new SatisfactionSummaryDTO(
                averageRating, totalReviews, latestRating, byCategory, reviews
        );

        // 9. Remediation Sessions
        List<RemediationSession> remSessions = remediationSessionRepository.findByStudentIdOrderByCreatedAtDesc(canonicalUserId);
        if (remSessions.isEmpty() && profileId != null) {
            remSessions = remediationSessionRepository.findByStudentIdOrderByCreatedAtDesc(profileId);
        }

        int totalRemSessions = remSessions.size();
        int completedRemSessions = (int) remSessions.stream().filter(RemediationSession::isCompleted).count();
        int activeRemSessions = totalRemSessions - completedRemSessions;

        List<RemediationSessionRecordDTO> remSessionRecords = new ArrayList<>();
        for (RemediationSession rem : remSessions) {
            remSessionRecords.add(new RemediationSessionRecordDTO(
                    rem.getId(),
                    rem.getSubject(),
                    rem.getConcept(),
                    rem.getModuleType() != null ? rem.getModuleType().name() : "REMEDIATION",
                    rem.isCompleted(),
                    rem.getCreatedAt()
            ));
        }

        RemediationSummaryDTO remediationDto = new RemediationSummaryDTO(
                totalRemSessions, completedRemSessions, activeRemSessions, remSessionRecords
        );

        // 10. Trajectory Points (Persisted StudentStateSnapshot records)
        List<StudentStateSnapshot> snapshots = snapshotRepository.findByStudentIdOrderByTimestampAsc(canonicalUserId);
        if (snapshots.isEmpty() && profileId != null) {
            snapshots = snapshotRepository.findByStudentIdOrderByTimestampAsc(profileId);
        }

        List<TrajectoryPointDTO> trajectory = new ArrayList<>();
        for (StudentStateSnapshot snap : snapshots) {
            trajectory.add(new TrajectoryPointDTO(
                    snap.getTimestamp(),
                    round2(snap.getOverallKnowledgeScore()),
                    round2(snap.getEngagementScore()),
                    snap.getTopicMastery()
            ));
        }

        return new AdminStudentAnalyticsDTO(
                studentDto,
                knowledgeDto,
                assessmentSummaryDto,
                assessmentHistory,
                quizSummaryDto,
                quizHistory,
                subjectPerformance,
                conceptMasteryRecords,
                activityDto,
                satisfactionDto,
                remediationDto,
                trajectory
        );
    }

    // ==========================================
    // 5. HISTORICAL RESEARCH TRENDS
    // ==========================================

    public AdminResearchTrendsDTO getResearchTrends() {
        return getResearchTrends(null);
    }

    public AdminResearchTrendsDTO getResearchTrends(AdminAnalyticsFilterCriteria criteria) {
        FilteredStudentContext ctx = resolveFilteredPopulation(criteria);
        Set<String> validStudentIds = ctx.validStudentIds;

        if (validStudentIds.isEmpty()) {
            return new AdminResearchTrendsDTO(
                    0, 0, 0, 0, 0, 0, null, null,
                    "No enrolled students match the filter criteria.",
                    Collections.emptyList()
            );
        }

        // Count students with authentic diagnostic baselines
        int evaluatedStudentsWithBaseline = 0;
        for (String studentId : validStudentIds) {
            if (Boolean.TRUE.equals(ctx.baselineMap.get(studentId))) {
                evaluatedStudentsWithBaseline++;
            }
        }

        // 2. Batch retrieve chronological observations from 3 distinct sources
        List<AssessmentResult> assessments = assessmentResultRepository.findAll();
        List<StudentStateSnapshot> snapshots = snapshotRepository.findAll();
        List<QuizSession> allQuizzes = quizSessionRepository.findAll();
        List<QuizSession> quizzes = allQuizzes.stream()
                .filter(q -> q.getStatus() == QuizSession.Status.COMPLETED)
                .collect(Collectors.toList());

        class AssessmentObservation {
            final LocalDateTime timestamp;
            final String dateStr;
            final double scorePct;
            final String studentId;

            AssessmentObservation(LocalDateTime timestamp, double scorePct, String studentId) {
                this.timestamp = timestamp;
                this.dateStr = timestamp.toLocalDate().toString();
                this.scorePct = scorePct;
                this.studentId = studentId;
            }
        }

        class QuizObservation {
            final LocalDateTime timestamp;
            final String dateStr;
            final double accuracyPct;
            final String studentId;

            QuizObservation(LocalDateTime timestamp, double accuracyPct, String studentId) {
                this.timestamp = timestamp;
                this.dateStr = timestamp.toLocalDate().toString();
                this.accuracyPct = accuracyPct;
                this.studentId = studentId;
            }
        }

        class SnapshotObservation {
            final LocalDateTime timestamp;
            final String dateStr;
            final double knowledgePct;
            final double engagementPct;
            final String studentId;

            SnapshotObservation(LocalDateTime timestamp, double knowledgePct, double engagementPct, String studentId) {
                this.timestamp = timestamp;
                this.dateStr = timestamp.toLocalDate().toString();
                this.knowledgePct = knowledgePct;
                this.engagementPct = engagementPct;
                this.studentId = studentId;
            }
        }

        List<AssessmentObservation> assessmentObs = new ArrayList<>();
        List<QuizObservation> quizObs = new ArrayList<>();
        List<SnapshotObservation> snapshotObs = new ArrayList<>();
        List<LocalDateTime> allTimestamps = new ArrayList<>();
        Set<String> distinctStudentsRepresented = new HashSet<>();
        Set<String> allActiveDates = new TreeSet<>();

        // A. Ingest AssessmentResult records (PRIMARY GROUND TRUTH FOR KNOWLEDGE PERFORMANCE)
        for (AssessmentResult ar : assessments) {
            if (ar.getCreatedAt() == null) continue;
            if (criteria != null && !criteria.isDateTimeInRange(ar.getCreatedAt())) continue;
            if (criteria != null && criteria.hasSubjectFilter() && !matchesSubject(ar.getSubjectCode(), ar.getSubjectName(), criteria.getSubjectCode())) continue;

            String canonicalId = resolveStudentId(ar.getUserId(), ar.getStudentProfileId(), validStudentIds, ctx.profileToUserMap);
            if (canonicalId != null) {
                double pct = ar.getPercentage() > 0 ? ar.getPercentage() : (ar.getScore() > 0 && ar.getTotalMarks() > 0 ? (ar.getScore() * 100.0 / ar.getTotalMarks()) : 0.0);
                if (pct <= 1.0 && pct > 0) pct *= 100.0;
                assessmentObs.add(new AssessmentObservation(ar.getCreatedAt(), round2(pct), canonicalId));
                allTimestamps.add(ar.getCreatedAt());
                distinctStudentsRepresented.add(canonicalId);
                allActiveDates.add(ar.getCreatedAt().toLocalDate().toString());
            }
        }

        // B. Ingest QuizSession records (SEPARATE QUIZ ACCURACY METRIC)
        for (QuizSession q : quizzes) {
            LocalDateTime qTime = q.getLastAnswerTime() != null ? q.getLastAnswerTime() : q.getStartTime();
            if (qTime == null) continue;
            if (criteria != null && !criteria.isDateTimeInRange(qTime)) continue;
            if (criteria != null && criteria.hasSubjectFilter() && !matchesSubject(q.getSubjectCode(), q.getSubjectName(), criteria.getSubjectCode())) continue;

            String canonicalId = resolveStudentId(q.getUserId(), q.getStudentProfileId(), validStudentIds, ctx.profileToUserMap);
            if (canonicalId != null) {
                double acc = q.getTotalQuestions() > 0 ? (q.getCorrectCount() * 100.0 / q.getTotalQuestions()) : 0.0;
                quizObs.add(new QuizObservation(qTime, round2(acc), canonicalId));
                allTimestamps.add(qTime);
                distinctStudentsRepresented.add(canonicalId);
                allActiveDates.add(qTime.toLocalDate().toString());
            }
        }

        // C. Ingest StudentStateSnapshot records (SEPARATE BAYESIAN STATE & ENGAGEMENT METRICS)
        for (StudentStateSnapshot snap : snapshots) {
            if (snap.getTimestamp() == null) continue;
            if (criteria != null && !criteria.isDateTimeInRange(snap.getTimestamp())) continue;

            String canonicalId = resolveStudentId(snap.getStudentId(), null, validStudentIds, ctx.profileToUserMap);
            if (canonicalId != null) {
                double k = snap.getOverallKnowledgeScore();
                if (k <= 1.0) k *= 100.0;
                double eng = snap.getEngagementScore();
                if (eng <= 1.0) eng *= 100.0;
                snapshotObs.add(new SnapshotObservation(snap.getTimestamp(), round2(k), round2(eng), canonicalId));
                allTimestamps.add(snap.getTimestamp());
                distinctStudentsRepresented.add(canonicalId);
                allActiveDates.add(snap.getTimestamp().toLocalDate().toString());
            }
        }

        int totalAssessments = assessmentObs.size();
        int totalQuizzes = quizObs.size();
        int totalSnapshots = snapshotObs.size();
        int totalObservations = totalAssessments + totalQuizzes + totalSnapshots;

        if (totalObservations == 0) {
            return new AdminResearchTrendsDTO(
                    0, 0, 0, 0, 0, evaluatedStudentsWithBaseline, null, null,
                    "No historical empirical evaluation records found for student cohort.",
                    Collections.emptyList()
            );
        }

        allTimestamps.sort(LocalDateTime::compareTo);
        LocalDateTime earliestDate = allTimestamps.get(0);
        LocalDateTime latestDate = allTimestamps.get(allTimestamps.size() - 1);

        // Group observations by date
        Map<String, List<AssessmentObservation>> assessmentsByDate = assessmentObs.stream()
                .collect(Collectors.groupingBy(o -> o.dateStr));
        Map<String, List<QuizObservation>> quizzesByDate = quizObs.stream()
                .collect(Collectors.groupingBy(o -> o.dateStr));
        Map<String, List<SnapshotObservation>> snapshotsByDate = snapshotObs.stream()
                .collect(Collectors.groupingBy(o -> o.dateStr));

        List<AdminResearchTrendsDTO.TrendPointDTO> trendPoints = new ArrayList<>();

        for (String dateStr : allActiveDates) {
            List<AssessmentObservation> dayAssessments = assessmentsByDate.getOrDefault(dateStr, Collections.emptyList());
            List<QuizObservation> dayQuizzes = quizzesByDate.getOrDefault(dateStr, Collections.emptyList());
            List<SnapshotObservation> daySnapshots = snapshotsByDate.getOrDefault(dateStr, Collections.emptyList());

            Set<String> dayStudents = new HashSet<>();
            LocalDateTime repTime = null;

            // 1. Primary Knowledge Performance: AssessmentResult ONLY
            Double meanAssessmentScore = null;
            if (!dayAssessments.isEmpty()) {
                double sumAssessments = 0.0;
                for (AssessmentObservation a : dayAssessments) {
                    sumAssessments += a.scorePct;
                    dayStudents.add(a.studentId);
                    if (repTime == null) repTime = a.timestamp;
                }
                meanAssessmentScore = round2(sumAssessments / dayAssessments.size());
            }

            // 2. Separate Quiz Accuracy (Never blended into assessment score)
            Double meanQuizAccuracy = null;
            if (!dayQuizzes.isEmpty()) {
                double sumQuiz = 0.0;
                for (QuizObservation q : dayQuizzes) {
                    sumQuiz += q.accuracyPct;
                    dayStudents.add(q.studentId);
                    if (repTime == null) repTime = q.timestamp;
                }
                meanQuizAccuracy = round2(sumQuiz / dayQuizzes.size());
            }

            // 3. Separate Snapshot State & Engagement (Never blended into assessment score)
            Double meanSnapshotKnowledge = null;
            Double meanEngagement = null;
            if (!daySnapshots.isEmpty()) {
                double sumSnapK = 0.0;
                double sumEng = 0.0;
                for (SnapshotObservation s : daySnapshots) {
                    sumSnapK += s.knowledgePct;
                    sumEng += s.engagementPct;
                    dayStudents.add(s.studentId);
                    if (repTime == null) repTime = s.timestamp;
                }
                meanSnapshotKnowledge = round2(sumSnapK / daySnapshots.size());
                meanEngagement = round2(sumEng / daySnapshots.size());
            }

            if (repTime == null) {
                repTime = LocalDateTime.parse(dateStr + "T00:00:00");
            }

            int dayObsCount = dayAssessments.size() + dayQuizzes.size() + daySnapshots.size();

            trendPoints.add(new AdminResearchTrendsDTO.TrendPointDTO(
                    dateStr,
                    repTime,
                    meanAssessmentScore,
                    meanQuizAccuracy,
                    meanSnapshotKnowledge,
                    meanEngagement,
                    dayObsCount,
                    dayStudents.size(),
                    dayAssessments.size(),
                    dayQuizzes.size(),
                    daySnapshots.size()
            ));
        }

        trendPoints.sort(Comparator.comparing(AdminResearchTrendsDTO.TrendPointDTO::getDate));

        String dataSufficiencyNote = "Historical research trends reflect empirical student evaluations and activity recorded in system logs. Dates without activity are omitted.";

        return new AdminResearchTrendsDTO(
                totalObservations,
                totalAssessments,
                totalQuizzes,
                totalSnapshots,
                distinctStudentsRepresented.size(),
                evaluatedStudentsWithBaseline,
                earliestDate,
                latestDate,
                dataSufficiencyNote,
                trendPoints
        );
    }

    // ==========================================
    // 6. COHORT SUBJECT ANALYTICS
    // ==========================================

    public AdminCohortSubjectAnalyticsDTO getCohortSubjectAnalytics() {
        return getCohortSubjectAnalytics(null);
    }

    public AdminCohortSubjectAnalyticsDTO getCohortSubjectAnalytics(AdminAnalyticsFilterCriteria criteria) {
        FilteredStudentContext ctx = resolveFilteredPopulation(criteria);
        Set<String> validStudentIds = ctx.validStudentIds;

        if (validStudentIds.isEmpty()) {
            return new AdminCohortSubjectAnalyticsDTO(
                    0, 0, 0,
                    "No enrolled students match the filter criteria.",
                    Collections.emptyList()
            );
        }

        // 2. Batch retrieve collections
        List<AssessmentResult> allAssessments = assessmentResultRepository.findAll();
        List<ConceptMastery> allConceptMastery = conceptMasteryRepository.findAll();
        List<QuizSession> allQuizzes = quizSessionRepository.findAll();
        List<SubjectRoadmap> allRoadmaps = subjectRoadmapRepository.findAll();

        // 3. Group by canonical student ID
        Map<String, List<AssessmentResult>> assessmentsByStudent = new HashMap<>();
        for (AssessmentResult ar : allAssessments) {
            String studentId = resolveStudentId(ar.getUserId(), ar.getStudentProfileId(), validStudentIds, ctx.profileToUserMap);
            if (studentId != null) {
                assessmentsByStudent.computeIfAbsent(studentId, k -> new ArrayList<>()).add(ar);
            }
        }

        Map<String, List<ConceptMastery>> masteryByStudent = new HashMap<>();
        for (ConceptMastery cm : allConceptMastery) {
            String studentId = resolveStudentId(cm.getUserId(), cm.getStudentProfileId(), validStudentIds, ctx.profileToUserMap);
            if (studentId != null) {
                if (criteria != null && !criteria.isDateTimeInRange(cm.getLastAssessedAt())) {
                    continue;
                }
                masteryByStudent.computeIfAbsent(studentId, k -> new ArrayList<>()).add(cm);
            }
        }

        Map<String, List<QuizSession>> quizzesByStudent = new HashMap<>();
        for (QuizSession qs : allQuizzes) {
            String studentId = resolveStudentId(qs.getUserId(), qs.getStudentProfileId(), validStudentIds, ctx.profileToUserMap);
            if (studentId != null && qs.getStatus() == QuizSession.Status.COMPLETED) {
                LocalDateTime qTime = qs.getLastAnswerTime() != null ? qs.getLastAnswerTime() : qs.getStartTime();
                if (criteria != null && !criteria.isDateTimeInRange(qTime)) {
                    continue;
                }
                quizzesByStudent.computeIfAbsent(studentId, k -> new ArrayList<>()).add(qs);
            }
        }

        Map<String, List<SubjectRoadmap>> roadmapsByStudent = new HashMap<>();
        for (SubjectRoadmap sr : allRoadmaps) {
            String studentId = resolveStudentId(sr.getUserId(), null, validStudentIds, ctx.profileToUserMap);
            if (studentId != null) {
                LocalDateTime rTime = sr.getUpdatedAt() != null ? sr.getUpdatedAt() : sr.getCreatedAt();
                if (criteria != null && !criteria.isDateTimeInRange(rTime)) {
                    continue;
                }
                roadmapsByStudent.computeIfAbsent(studentId, k -> new ArrayList<>()).add(sr);
            }
        }

        // 4. Discover all distinct canonical subjects across data sources
        Map<String, SubjectCatalogInfo> canonicalSubjects = new LinkedHashMap<>();

        java.util.function.Consumer<SubjectCatalogInfo> registerSubject = (info) -> {
            if (info != null && info.subjectName != null && !info.subjectName.isBlank()) {
                if (criteria != null && criteria.hasSubjectFilter()) {
                    if (!matchesSubject(info.subjectCode, info.subjectName, criteria.getSubjectCode())) {
                        return;
                    }
                }
                String key = normalizeSubjectName(info.subjectName);
                canonicalSubjects.compute(key, (k, existing) -> {
                    if (existing == null) return info;
                    if ((existing.subjectCode == null || existing.subjectCode.isBlank()) && (info.subjectCode != null && !info.subjectCode.isBlank())) {
                        existing.subjectCode = info.subjectCode;
                    }
                    return existing;
                });
            }
        };

        // Scan profiles of valid cohort
        for (String studentId : validStudentIds) {
            StudentProfile p = ctx.profileMap.get(studentId);
            if (p != null && p.getSubjects() != null) {
                for (String subj : p.getSubjects()) {
                    registerSubject.accept(new SubjectCatalogInfo(null, subj.trim()));
                }
            }
        }

        // Scan assessments of valid cohort
        for (Map.Entry<String, List<AssessmentResult>> entry : assessmentsByStudent.entrySet()) {
            for (AssessmentResult ar : entry.getValue()) {
                if (ar.getSubjectName() != null && !ar.getSubjectName().isBlank()) {
                    registerSubject.accept(new SubjectCatalogInfo(ar.getSubjectCode(), ar.getSubjectName().trim()));
                }
            }
        }

        // Scan concept mastery of valid cohort
        for (Map.Entry<String, List<ConceptMastery>> entry : masteryByStudent.entrySet()) {
            for (ConceptMastery cm : entry.getValue()) {
                if (cm.getSubjectName() != null && !cm.getSubjectName().isBlank()) {
                    registerSubject.accept(new SubjectCatalogInfo(cm.getSubjectCode(), cm.getSubjectName().trim()));
                }
            }
        }

        // Scan quizzes of valid cohort
        for (Map.Entry<String, List<QuizSession>> entry : quizzesByStudent.entrySet()) {
            for (QuizSession qs : entry.getValue()) {
                if (qs.getSubjectName() != null && !qs.getSubjectName().isBlank()) {
                    registerSubject.accept(new SubjectCatalogInfo(qs.getSubjectCode(), qs.getSubjectName().trim()));
                }
            }
        }

        // Scan roadmaps of valid cohort
        for (Map.Entry<String, List<SubjectRoadmap>> entry : roadmapsByStudent.entrySet()) {
            for (SubjectRoadmap sr : entry.getValue()) {
                if (sr.getSubjectName() != null && !sr.getSubjectName().isBlank()) {
                    registerSubject.accept(new SubjectCatalogInfo(sr.getSubjectCode(), sr.getSubjectName().trim()));
                }
            }
        }

        if (canonicalSubjects.isEmpty()) {
            return new AdminCohortSubjectAnalyticsDTO(
                    0, validStudentIds.size(), 0,
                    "No subject records found across filtered student cohort.",
                    Collections.emptyList()
            );
        }

        // 5. Pre-compute each student's EARLIEST authentic diagnostic assessment GLOBALLY (K0 is immutable and never bounded by date filters)
        Map<String, AssessmentResult> earliestDiagnosticByStudent = new HashMap<>();
        for (String studentId : validStudentIds) {
            List<AssessmentResult> sAssessments = assessmentsByStudent.get(studentId);
            if (sAssessments != null && !sAssessments.isEmpty()) {
                List<AssessmentResult> sortedAssessments = new ArrayList<>(sAssessments);
                sortedAssessments.sort(Comparator.comparing(AssessmentResult::getCreatedAt));
                for (AssessmentResult ar : sortedAssessments) {
                    if (ar.getTotalQuestions() > 0 || ar.getPercentage() > 0 || ar.getScore() > 0) {
                        earliestDiagnosticByStudent.put(studentId, ar);
                        break;
                    }
                }
            }
        }

        int globalEvaluatedStudentsWithBaseline = 0;
        for (String studentId : validStudentIds) {
            if (Boolean.TRUE.equals(ctx.baselineMap.get(studentId))) {
                globalEvaluatedStudentsWithBaseline++;
            }
        }

        // 6. Calculate subject research metrics for each canonical subject
        List<AdminCohortSubjectAnalyticsDTO.SubjectResearchSummaryDTO> subjectSummaries = new ArrayList<>();

        for (Map.Entry<String, SubjectCatalogInfo> entry : canonicalSubjects.entrySet()) {
            SubjectCatalogInfo catInfo = entry.getValue();
            String subjectName = catInfo.subjectName;
            String subjectCode = catInfo.subjectCode != null && !catInfo.subjectCode.isBlank() ? catInfo.subjectCode : subjectName;

            Set<String> subjectStudentsRepresented = new HashSet<>();
            List<Double> validBaselines = new ArrayList<>();
            List<Double> validCurrentKnowledge = new ArrayList<>();
            List<Double> validGrowthPoints = new ArrayList<>();
            List<Double> validNormalizedGains = new ArrayList<>();

            // A. Per-student subject evaluation
            for (String studentId : validStudentIds) {
                boolean hasSubjectEvidence = false;

                // Check profile enrollment
                StudentProfile sp = ctx.profileMap.get(studentId);
                if (sp != null && sp.getSubjects() != null) {
                    for (String subj : sp.getSubjects()) {
                        if (isSubjectMatch(subj, subjectName)) {
                            hasSubjectEvidence = true;
                            break;
                        }
                    }
                }

                // 1. Calculate authentic Subject K0 (strictly from earliest diagnostic globally)
                Double studentSubjectK0 = null;
                AssessmentResult earliestDiag = earliestDiagnosticByStudent.get(studentId);
                if (earliestDiag != null) {
                    if (isSubjectMatch(earliestDiag.getSubjectName(), subjectName) ||
                            (earliestDiag.getSubjectCode() != null && earliestDiag.getSubjectCode().equalsIgnoreCase(subjectCode))) {
                        double pct = earliestDiag.getPercentage() > 0 ? earliestDiag.getPercentage() :
                                (earliestDiag.getScore() > 0 && earliestDiag.getTotalMarks() > 0 ? (earliestDiag.getScore() * 100.0 / earliestDiag.getTotalMarks()) : 0.0);
                        if (pct <= 1.0 && pct > 0) pct *= 100.0;
                        studentSubjectK0 = round2(pct);
                        hasSubjectEvidence = true;
                    }
                }

                if (studentSubjectK0 != null) {
                    validBaselines.add(studentSubjectK0);
                }

                // 2. Calculate Current Subject Knowledge Kt (from student's ConceptMastery records for this subject)
                Double studentSubjectKt = null;
                List<ConceptMastery> studentCms = masteryByStudent.get(studentId);
                if (studentCms != null && !studentCms.isEmpty()) {
                    List<ConceptMastery> subjCms = studentCms.stream()
                            .filter(cm -> isSubjectMatch(cm.getSubjectName(), subjectName) || (cm.getSubjectCode() != null && cm.getSubjectCode().equalsIgnoreCase(subjectCode)))
                            .collect(Collectors.toList());
                    if (!subjCms.isEmpty()) {
                        hasSubjectEvidence = true;
                        double sumAcc = 0.0;
                        for (ConceptMastery cm : subjCms) {
                            double acc = cm.getAccuracy() > 0 ? cm.getAccuracy() : cm.getMasteryScore();
                            sumAcc += acc;
                        }
                        studentSubjectKt = round2(sumAcc / subjCms.size());
                    }
                }

                if (studentSubjectKt != null) {
                    validCurrentKnowledge.add(studentSubjectKt);
                }

                // 3. Growth & Normalized Gain (ONLY when BOTH authentic K0 and valid Kt exist)
                if (studentSubjectK0 != null && studentSubjectKt != null) {
                    double growth = round2(studentSubjectKt - studentSubjectK0);
                    double gain = round2(LearningGainService.computeGain(studentSubjectK0 / 100.0, studentSubjectKt / 100.0));
                    validGrowthPoints.add(growth);
                    validNormalizedGains.add(gain);
                }

                // Check quiz activity
                List<QuizSession> sQuizzes = quizzesByStudent.get(studentId);
                if (sQuizzes != null) {
                    for (QuizSession qs : sQuizzes) {
                        if (isSubjectMatch(qs.getSubjectName(), subjectName) || (qs.getSubjectCode() != null && qs.getSubjectCode().equalsIgnoreCase(subjectCode))) {
                            hasSubjectEvidence = true;
                            break;
                        }
                    }
                }

                // Check roadmaps
                List<SubjectRoadmap> sRoadmaps = roadmapsByStudent.get(studentId);
                if (sRoadmaps != null) {
                    for (SubjectRoadmap sr : sRoadmaps) {
                        if (isSubjectMatch(sr.getSubjectName(), subjectName) || (sr.getSubjectCode() != null && sr.getSubjectCode().equalsIgnoreCase(subjectCode))) {
                            hasSubjectEvidence = true;
                            break;
                        }
                    }
                }

                if (hasSubjectEvidence) {
                    subjectStudentsRepresented.add(studentId);
                }
            }

            // B. Aggregate Subject Means
            Double meanBaseline = !validBaselines.isEmpty() ? round2(validBaselines.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)) : null;
            Double meanCurrent = !validCurrentKnowledge.isEmpty() ? round2(validCurrentKnowledge.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)) : null;
            Double meanGrowth = !validGrowthPoints.isEmpty() ? round2(validGrowthPoints.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)) : null;
            Double meanGain = !validNormalizedGains.isEmpty() ? round2(validNormalizedGains.stream().mapToDouble(Double::doubleValue).average().orElse(0.0)) : null;

            // C. Concept Analytics for this subject (across filtered students)
            List<ConceptMastery> allSubjMastery = allConceptMastery.stream()
                    .filter(cm -> {
                        String studentId = resolveStudentId(cm.getUserId(), cm.getStudentProfileId(), validStudentIds, ctx.profileToUserMap);
                        if (studentId == null) return false;
                        if (criteria != null && !criteria.isDateTimeInRange(cm.getLastAssessedAt())) return false;
                        return (isSubjectMatch(cm.getSubjectName(), subjectName) || (cm.getSubjectCode() != null && cm.getSubjectCode().equalsIgnoreCase(subjectCode)));
                    })
                    .collect(Collectors.toList());

            Set<String> distinctConceptNames = new HashSet<>();
            int weakConceptCount = 0;
            Map<String, Integer> masteryDist = new LinkedHashMap<>();
            for (ConceptMastery.MasteryLevel ml : ConceptMastery.MasteryLevel.values()) {
                masteryDist.put(ml.name(), 0);
            }

            for (ConceptMastery cm : allSubjMastery) {
                String cName = cm.getConceptName() != null && !cm.getConceptName().isBlank() ? cm.getConceptName() : cm.getTopic();
                if (cName != null && !cName.isBlank()) {
                    distinctConceptNames.add(cName.trim());
                }
                double acc = cm.getAccuracy() > 0 ? cm.getAccuracy() : cm.getMasteryScore();
                if (cm.getStatus() == ConceptMastery.ConceptStatus.WEAK || acc < 70.0) {
                    weakConceptCount++;
                }
                String mlName = cm.getMasteryLevel() != null ? cm.getMasteryLevel().name() : "UNKNOWN";
                masteryDist.put(mlName, masteryDist.getOrDefault(mlName, 0) + 1);
            }

            int conceptCount = distinctConceptNames.size();

            // D. Quiz Analytics for this subject
            List<QuizSession> allSubjQuizzes = allQuizzes.stream()
                    .filter(qs -> {
                        String studentId = resolveStudentId(qs.getUserId(), qs.getStudentProfileId(), validStudentIds, ctx.profileToUserMap);
                        if (studentId == null || qs.getStatus() != QuizSession.Status.COMPLETED) return false;
                        LocalDateTime qTime = qs.getLastAnswerTime() != null ? qs.getLastAnswerTime() : qs.getStartTime();
                        if (criteria != null && !criteria.isDateTimeInRange(qTime)) return false;
                        return (isSubjectMatch(qs.getSubjectName(), subjectName) || (qs.getSubjectCode() != null && qs.getSubjectCode().equalsIgnoreCase(subjectCode)));
                    })
                    .collect(Collectors.toList());

            int quizSessionsCount = allSubjQuizzes.size();
            int totalQuizQuestions = allSubjQuizzes.stream().mapToInt(QuizSession::getTotalQuestions).sum();
            int correctQuizAnswers = allSubjQuizzes.stream().mapToInt(QuizSession::getCorrectCount).sum();
            Double meanQuizAccuracy = totalQuizQuestions > 0 ? round2((double) correctQuizAnswers * 100.0 / totalQuizQuestions) : null;

            // E. Roadmap Analytics for this subject
            List<SubjectRoadmap> allSubjRoadmaps = allRoadmaps.stream()
                    .filter(sr -> {
                        String studentId = resolveStudentId(sr.getUserId(), null, validStudentIds, ctx.profileToUserMap);
                        if (studentId == null) return false;
                        LocalDateTime rTime = sr.getUpdatedAt() != null ? sr.getUpdatedAt() : sr.getCreatedAt();
                        if (criteria != null && !criteria.isDateTimeInRange(rTime)) return false;
                        return (isSubjectMatch(sr.getSubjectName(), subjectName) || (sr.getSubjectCode() != null && sr.getSubjectCode().equalsIgnoreCase(subjectCode)));
                    })
                    .collect(Collectors.toList());

            int studentsWithRoadmap = (int) allSubjRoadmaps.stream().map(SubjectRoadmap::getUserId).filter(Objects::nonNull).distinct().count();
            int totalRoadmapTopics = 0;
            int completedRoadmapTopics = 0;
            for (SubjectRoadmap sr : allSubjRoadmaps) {
                if (sr.getTopics() != null) {
                    totalRoadmapTopics += sr.getTopics().size();
                    completedRoadmapTopics += (int) sr.getTopics().stream().filter(SubjectRoadmap.RoadmapTopicNode::isCompleted).count();
                }
            }
            Double roadmapCompletion = totalRoadmapTopics > 0 ? round2((double) completedRoadmapTopics * 100.0 / totalRoadmapTopics) : null;

            subjectSummaries.add(new AdminCohortSubjectAnalyticsDTO.SubjectResearchSummaryDTO(
                    subjectCode,
                    subjectName,
                    subjectStudentsRepresented.size(),
                    validCurrentKnowledge.size(),
                    validBaselines.size(),
                    meanBaseline,
                    meanCurrent,
                    meanGrowth,
                    meanGain,
                    conceptCount,
                    weakConceptCount,
                    masteryDist,
                    quizSessionsCount,
                    totalQuizQuestions,
                    correctQuizAnswers,
                    meanQuizAccuracy,
                    studentsWithRoadmap,
                    totalRoadmapTopics,
                    completedRoadmapTopics,
                    roadmapCompletion
            ));
        }

        // Sort subjects by name alphabetically
        subjectSummaries.sort(Comparator.comparing(AdminCohortSubjectAnalyticsDTO.SubjectResearchSummaryDTO::getSubjectName));

        String dataSufficiencyNote = "Subject baseline and learning gain statistics include only students with verified subject-level diagnostic baselines. Missing baseline data is not synthesized or defaulted.";

        return new AdminCohortSubjectAnalyticsDTO(
                subjectSummaries.size(),
                validStudentIds.size(),
                globalEvaluatedStudentsWithBaseline,
                dataSufficiencyNote,
                subjectSummaries
        );
    }

    private static class SubjectCatalogInfo {
        String subjectCode;
        String subjectName;

        SubjectCatalogInfo(String subjectCode, String subjectName) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
        }
    }

    private Double findSubjectBaseline(String subjectName, List<AssessmentResult> arList, Map<String, Double> baselineMap) {
        if (baselineMap != null && !baselineMap.isEmpty() && arList != null && !arList.isEmpty()) {
            for (int i = arList.size() - 1; i >= 0; i--) {
                AssessmentResult ar = arList.get(i);
                if (ar.getSubjectName() != null && isSubjectMatch(ar.getSubjectName(), subjectName)) {
                    double sc = ar.getPercentage() > 0 ? ar.getPercentage() : (ar.getScore() > 0 ? ar.getScore() : 0.0);
                    if (sc <= 1.0) sc *= 100.0;
                    return round2(sc);
                }
            }
        }
        return null;
    }

    private boolean isSubjectMatch(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        String clean1 = s1.trim().toLowerCase();
        String clean2 = s2.trim().toLowerCase();
        return clean1.equals(clean2) || clean1.contains(clean2) || clean2.contains(clean1);
    }

    private boolean matchesSubject(String recordSubjectCode, String recordSubjectName, String filterSubjectCode) {
        if (filterSubjectCode == null || filterSubjectCode.trim().isEmpty()) {
            return true;
        }
        String cleanFilter = filterSubjectCode.trim();
        if (recordSubjectCode != null && recordSubjectCode.trim().equalsIgnoreCase(cleanFilter)) {
            return true;
        }
        if (recordSubjectName != null && (recordSubjectName.trim().equalsIgnoreCase(cleanFilter) || isSubjectMatch(recordSubjectName, cleanFilter))) {
            return true;
        }
        return false;
    }

    private String resolveStudentId(String userId, String profileId, Set<String> validStudentIds, Map<String, String> profileToUserMap) {
        if (userId != null && validStudentIds.contains(userId)) {
            return userId;
        }
        if (profileId != null && profileToUserMap.containsKey(profileId)) {
            String mapped = profileToUserMap.get(profileId);
            if (validStudentIds.contains(mapped)) {
                return mapped;
            }
        }
        if (userId != null && profileToUserMap.containsKey(userId)) {
            String mapped = profileToUserMap.get(userId);
            if (validStudentIds.contains(mapped)) {
                return mapped;
            }
        }
        return null;
    }

    private String normalizeSubjectName(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private static double round2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
