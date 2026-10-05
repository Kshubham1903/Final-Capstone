package com.edupilot.service;

import com.edupilot.dto.AdminAnalyticsOverviewDTO;
import com.edupilot.dto.AdminCohortAnalyticsDTO;
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

    public List<AdminStudentDirectoryDTO> getStudentDirectory() {
        List<User> studentUsers = userRepository.findByRole(User.Role.STUDENT);
        if (studentUsers == null || studentUsers.isEmpty()) {
            return Collections.emptyList();
        }

        List<AdminStudentDirectoryDTO> directory = new ArrayList<>();

        for (User user : studentUsers) {
            String userId = user.getId();
            if (userId == null || userId.isBlank()) {
                continue;
            }

            String email = user.getEmail();
            String fullName = user.getFullName();

            // Profile information lookup
            Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(userId);
            if (profileOpt.isEmpty() && email != null) {
                profileOpt = studentProfileRepository.findByEmail(email);
            }

            String branch = null;
            Integer semester = null;
            String profileId = null;

            if (profileOpt.isPresent()) {
                StudentProfile profile = profileOpt.get();
                profileId = profile.getId();
                branch = profile.getBranch();
                semester = profile.getSemester();
                if ((fullName == null || fullName.isBlank()) && profile.getFullName() != null) {
                    fullName = profile.getFullName();
                }
            }

            // Activity status determination
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

            // Authentic baseline check
            boolean hasAuthenticBaseline = false;
            Double baselineKnowledge = null;
            Double currentKnowledge = null;
            Double growthPp = null;

            Map<String, Double> baselineMap = studentGrowthService.extractBaselineConceptAccuracies(userId, profileId);
            if (baselineMap != null && !baselineMap.isEmpty()) {
                hasAuthenticBaseline = true;
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
                // When baseline is missing, attempt to compute current knowledge from authentic concept mastery records if present
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
            // When baseline is missing, estimate current knowledge from ConceptMastery if available
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

    private static double round2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
