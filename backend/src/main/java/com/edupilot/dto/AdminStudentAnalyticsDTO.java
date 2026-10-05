package com.edupilot.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AdminStudentAnalyticsDTO {

    private StudentInfoDTO student;
    private KnowledgeGrowthDTO knowledge;
    private AssessmentSummaryDTO assessmentSummary;
    private List<AssessmentRecordDTO> assessmentHistory;
    private QuizSummaryDTO quizSummary;
    private List<QuizRecordDTO> quizHistory;
    private List<SubjectPerformanceDTO> subjectPerformance;
    private List<ConceptMasteryRecordDTO> conceptMastery;
    private ActivityDTO activity;
    private SatisfactionSummaryDTO satisfaction;
    private RemediationSummaryDTO remediation;
    private List<TrajectoryPointDTO> trajectory;

    public AdminStudentAnalyticsDTO() {
    }

    public AdminStudentAnalyticsDTO(StudentInfoDTO student,
                                    KnowledgeGrowthDTO knowledge,
                                    AssessmentSummaryDTO assessmentSummary,
                                    List<AssessmentRecordDTO> assessmentHistory,
                                    QuizSummaryDTO quizSummary,
                                    List<QuizRecordDTO> quizHistory,
                                    List<SubjectPerformanceDTO> subjectPerformance,
                                    List<ConceptMasteryRecordDTO> conceptMastery,
                                    ActivityDTO activity,
                                    SatisfactionSummaryDTO satisfaction,
                                    RemediationSummaryDTO remediation,
                                    List<TrajectoryPointDTO> trajectory) {
        this.student = student;
        this.knowledge = knowledge;
        this.assessmentSummary = assessmentSummary;
        this.assessmentHistory = assessmentHistory;
        this.quizSummary = quizSummary;
        this.quizHistory = quizHistory;
        this.subjectPerformance = subjectPerformance;
        this.conceptMastery = conceptMastery;
        this.activity = activity;
        this.satisfaction = satisfaction;
        this.remediation = remediation;
        this.trajectory = trajectory;
    }

    // --- 1. Student Identity DTO ---
    public static class StudentInfoDTO {
        private String userId;
        private String fullName;
        private String email;
        private String branch;
        private Integer semester;
        private String institution;
        private String degree;
        private String careerGoals;
        private String learningStyle;
        private List<String> enrolledSubjects;

        public StudentInfoDTO() {
        }

        public StudentInfoDTO(String userId, String fullName, String email, String branch,
                              Integer semester, String institution, String degree,
                              String careerGoals, String learningStyle, List<String> enrolledSubjects) {
            this.userId = userId;
            this.fullName = fullName;
            this.email = email;
            this.branch = branch;
            this.semester = semester;
            this.institution = institution;
            this.degree = degree;
            this.careerGoals = careerGoals;
            this.learningStyle = learningStyle;
            this.enrolledSubjects = enrolledSubjects;
        }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }
        public Integer getSemester() { return semester; }
        public void setSemester(Integer semester) { this.semester = semester; }
        public String getInstitution() { return institution; }
        public void setInstitution(String institution) { this.institution = institution; }
        public String getDegree() { return degree; }
        public void setDegree(String degree) { this.degree = degree; }
        public String getCareerGoals() { return careerGoals; }
        public void setCareerGoals(String careerGoals) { this.careerGoals = careerGoals; }
        public String getLearningStyle() { return learningStyle; }
        public void setLearningStyle(String learningStyle) { this.learningStyle = learningStyle; }
        public List<String> getEnrolledSubjects() { return enrolledSubjects; }
        public void setEnrolledSubjects(List<String> enrolledSubjects) { this.enrolledSubjects = enrolledSubjects; }
    }

    // --- 2. Knowledge Growth DTO ---
    public static class KnowledgeGrowthDTO {
        private boolean hasAuthenticBaseline;
        private Double baselineKnowledge;
        private Double currentKnowledge;
        private Double growthPp;
        private Double normalizedLearningGain;

        public KnowledgeGrowthDTO() {
        }

        public KnowledgeGrowthDTO(boolean hasAuthenticBaseline, Double baselineKnowledge,
                                  Double currentKnowledge, Double growthPp, Double normalizedLearningGain) {
            this.hasAuthenticBaseline = hasAuthenticBaseline;
            this.baselineKnowledge = baselineKnowledge;
            this.currentKnowledge = currentKnowledge;
            this.growthPp = growthPp;
            this.normalizedLearningGain = normalizedLearningGain;
        }

        public boolean isHasAuthenticBaseline() { return hasAuthenticBaseline; }
        public void setHasAuthenticBaseline(boolean hasAuthenticBaseline) { this.hasAuthenticBaseline = hasAuthenticBaseline; }
        public Double getBaselineKnowledge() { return baselineKnowledge; }
        public void setBaselineKnowledge(Double baselineKnowledge) { this.baselineKnowledge = baselineKnowledge; }
        public Double getCurrentKnowledge() { return currentKnowledge; }
        public void setCurrentKnowledge(Double currentKnowledge) { this.currentKnowledge = currentKnowledge; }
        public Double getGrowthPp() { return growthPp; }
        public void setGrowthPp(Double growthPp) { this.growthPp = growthPp; }
        public Double getNormalizedLearningGain() { return normalizedLearningGain; }
        public void setNormalizedLearningGain(Double normalizedLearningGain) { this.normalizedLearningGain = normalizedLearningGain; }
    }

    // --- 3. Assessment Summary DTO ---
    public static class AssessmentSummaryDTO {
        private int totalAssessments;
        private int completedAssessments;
        private LocalDateTime baselineAssessmentDate;
        private LocalDateTime latestAssessmentDate;
        private Double latestAssessmentScore;
        private Double latestAssessmentPercentage;

        public AssessmentSummaryDTO() {
        }

        public AssessmentSummaryDTO(int totalAssessments, int completedAssessments,
                                    LocalDateTime baselineAssessmentDate, LocalDateTime latestAssessmentDate,
                                    Double latestAssessmentScore, Double latestAssessmentPercentage) {
            this.totalAssessments = totalAssessments;
            this.completedAssessments = completedAssessments;
            this.baselineAssessmentDate = baselineAssessmentDate;
            this.latestAssessmentDate = latestAssessmentDate;
            this.latestAssessmentScore = latestAssessmentScore;
            this.latestAssessmentPercentage = latestAssessmentPercentage;
        }

        public int getTotalAssessments() { return totalAssessments; }
        public void setTotalAssessments(int totalAssessments) { this.totalAssessments = totalAssessments; }
        public int getCompletedAssessments() { return completedAssessments; }
        public void setCompletedAssessments(int completedAssessments) { this.completedAssessments = completedAssessments; }
        public LocalDateTime getBaselineAssessmentDate() { return baselineAssessmentDate; }
        public void setBaselineAssessmentDate(LocalDateTime baselineAssessmentDate) { this.baselineAssessmentDate = baselineAssessmentDate; }
        public LocalDateTime getLatestAssessmentDate() { return latestAssessmentDate; }
        public void setLatestAssessmentDate(LocalDateTime latestAssessmentDate) { this.latestAssessmentDate = latestAssessmentDate; }
        public Double getLatestAssessmentScore() { return latestAssessmentScore; }
        public void setLatestAssessmentScore(Double latestAssessmentScore) { this.latestAssessmentScore = latestAssessmentScore; }
        public Double getLatestAssessmentPercentage() { return latestAssessmentPercentage; }
        public void setLatestAssessmentPercentage(Double latestAssessmentPercentage) { this.latestAssessmentPercentage = latestAssessmentPercentage; }
    }

    // --- 4. Assessment Record DTO ---
    public static class AssessmentRecordDTO {
        private String id;
        private String sessionId;
        private String subjectCode;
        private String subjectName;
        private String moduleType;
        private String status;
        private double score;
        private int totalMarks;
        private double percentage;
        private double accuracy;
        private String masteryLevel;
        private int totalQuestions;
        private int correctAnswers;
        private int incorrectAnswers;
        private int skippedQuestions;
        private Map<String, Map<String, Object>> topicBreakdown;
        private LocalDateTime createdAt;

        public AssessmentRecordDTO() {
        }

        public AssessmentRecordDTO(String id, String sessionId, String subjectCode, String subjectName,
                                   String moduleType, String status, double score, int totalMarks,
                                   double percentage, double accuracy, String masteryLevel,
                                   int totalQuestions, int correctAnswers, int incorrectAnswers,
                                   int skippedQuestions, Map<String, Map<String, Object>> topicBreakdown,
                                   LocalDateTime createdAt) {
            this.id = id;
            this.sessionId = sessionId;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.moduleType = moduleType;
            this.status = status;
            this.score = score;
            this.totalMarks = totalMarks;
            this.percentage = percentage;
            this.accuracy = accuracy;
            this.masteryLevel = masteryLevel;
            this.totalQuestions = totalQuestions;
            this.correctAnswers = correctAnswers;
            this.incorrectAnswers = incorrectAnswers;
            this.skippedQuestions = skippedQuestions;
            this.topicBreakdown = topicBreakdown;
            this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
        public String getModuleType() { return moduleType; }
        public void setModuleType(String moduleType) { this.moduleType = moduleType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public int getTotalMarks() { return totalMarks; }
        public void setTotalMarks(int totalMarks) { this.totalMarks = totalMarks; }
        public double getPercentage() { return percentage; }
        public void setPercentage(double percentage) { this.percentage = percentage; }
        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
        public String getMasteryLevel() { return masteryLevel; }
        public void setMasteryLevel(String masteryLevel) { this.masteryLevel = masteryLevel; }
        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
        public int getCorrectAnswers() { return correctAnswers; }
        public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
        public int getIncorrectAnswers() { return incorrectAnswers; }
        public void setIncorrectAnswers(int incorrectAnswers) { this.incorrectAnswers = incorrectAnswers; }
        public int getSkippedQuestions() { return skippedQuestions; }
        public void setSkippedQuestions(int skippedQuestions) { this.skippedQuestions = skippedQuestions; }
        public Map<String, Map<String, Object>> getTopicBreakdown() { return topicBreakdown; }
        public void setTopicBreakdown(Map<String, Map<String, Object>> topicBreakdown) { this.topicBreakdown = topicBreakdown; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    // --- 5. Quiz Summary DTO ---
    public static class QuizSummaryDTO {
        private int totalCompletedQuizzes;
        private int totalQuestions;
        private int totalCorrect;
        private double accuracy;

        public QuizSummaryDTO() {
        }

        public QuizSummaryDTO(int totalCompletedQuizzes, int totalQuestions, int totalCorrect, double accuracy) {
            this.totalCompletedQuizzes = totalCompletedQuizzes;
            this.totalQuestions = totalQuestions;
            this.totalCorrect = totalCorrect;
            this.accuracy = accuracy;
        }

        public int getTotalCompletedQuizzes() { return totalCompletedQuizzes; }
        public void setTotalCompletedQuizzes(int totalCompletedQuizzes) { this.totalCompletedQuizzes = totalCompletedQuizzes; }
        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
        public int getTotalCorrect() { return totalCorrect; }
        public void setTotalCorrect(int totalCorrect) { this.totalCorrect = totalCorrect; }
        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    }

    // --- 6. Quiz Record DTO ---
    public static class QuizRecordDTO {
        private String id;
        private String subjectCode;
        private String subjectName;
        private String moduleType;
        private boolean isVerificationQuiz;
        private String targetConcept;
        private int totalQuestions;
        private int correctCount;
        private int incorrectCount;
        private double accuracy;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime lastAnswerTime;

        public QuizRecordDTO() {
        }

        public QuizRecordDTO(String id, String subjectCode, String subjectName, String moduleType,
                             boolean isVerificationQuiz, String targetConcept, int totalQuestions,
                             int correctCount, int incorrectCount, double accuracy, String status,
                             LocalDateTime startTime, LocalDateTime lastAnswerTime) {
            this.id = id;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.moduleType = moduleType;
            this.isVerificationQuiz = isVerificationQuiz;
            this.targetConcept = targetConcept;
            this.totalQuestions = totalQuestions;
            this.correctCount = correctCount;
            this.incorrectCount = incorrectCount;
            this.accuracy = accuracy;
            this.status = status;
            this.startTime = startTime;
            this.lastAnswerTime = lastAnswerTime;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
        public String getModuleType() { return moduleType; }
        public void setModuleType(String moduleType) { this.moduleType = moduleType; }
        public boolean isVerificationQuiz() { return isVerificationQuiz; }
        public void setVerificationQuiz(boolean verificationQuiz) { isVerificationQuiz = verificationQuiz; }
        public String getTargetConcept() { return targetConcept; }
        public void setTargetConcept(String targetConcept) { this.targetConcept = targetConcept; }
        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
        public int getCorrectCount() { return correctCount; }
        public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
        public int getIncorrectCount() { return incorrectCount; }
        public void setIncorrectCount(int incorrectCount) { this.incorrectCount = incorrectCount; }
        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getLastAnswerTime() { return lastAnswerTime; }
        public void setLastAnswerTime(LocalDateTime lastAnswerTime) { this.lastAnswerTime = lastAnswerTime; }
    }

    // --- 7. Subject Performance DTO ---
    public static class SubjectPerformanceDTO {
        private String subjectCode;
        private String subjectName;
        private Double baselineScore;
        private Double currentScore;
        private Double growthPp;
        private Double normalizedGain;
        private int totalConcepts;
        private int weakConcepts;
        private Double roadmapProgressPercentage;

        public SubjectPerformanceDTO() {
        }

        public SubjectPerformanceDTO(String subjectCode, String subjectName, Double baselineScore,
                                     Double currentScore, Double growthPp, Double normalizedGain,
                                     int totalConcepts, int weakConcepts, Double roadmapProgressPercentage) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.baselineScore = baselineScore;
            this.currentScore = currentScore;
            this.growthPp = growthPp;
            this.normalizedGain = normalizedGain;
            this.totalConcepts = totalConcepts;
            this.weakConcepts = weakConcepts;
            this.roadmapProgressPercentage = roadmapProgressPercentage;
        }

        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
        public Double getBaselineScore() { return baselineScore; }
        public void setBaselineScore(Double baselineScore) { this.baselineScore = baselineScore; }
        public Double getCurrentScore() { return currentScore; }
        public void setCurrentScore(Double currentScore) { this.currentScore = currentScore; }
        public Double getGrowthPp() { return growthPp; }
        public void setGrowthPp(Double growthPp) { this.growthPp = growthPp; }
        public Double getNormalizedGain() { return normalizedGain; }
        public void setNormalizedGain(Double normalizedGain) { this.normalizedGain = normalizedGain; }
        public int getTotalConcepts() { return totalConcepts; }
        public void setTotalConcepts(int totalConcepts) { this.totalConcepts = totalConcepts; }
        public int getWeakConcepts() { return weakConcepts; }
        public void setWeakConcepts(int weakConcepts) { this.weakConcepts = weakConcepts; }
        public Double getRoadmapProgressPercentage() { return roadmapProgressPercentage; }
        public void setRoadmapProgressPercentage(Double roadmapProgressPercentage) { this.roadmapProgressPercentage = roadmapProgressPercentage; }
    }

    // --- 8. Concept Mastery Record DTO ---
    public static class ConceptMasteryRecordDTO {
        private String id;
        private String subjectCode;
        private String subjectName;
        private String topic;
        private String conceptName;
        private String masteryLevel;
        private String status;
        private double accuracy;
        private double confidenceScore;
        private int attemptCount;
        private int correctCount;
        private int wrongCount;
        private double masteryScore;
        private LocalDateTime lastAssessedAt;

        public ConceptMasteryRecordDTO() {
        }

        public ConceptMasteryRecordDTO(String id, String subjectCode, String subjectName, String topic,
                                       String conceptName, String masteryLevel, String status,
                                       double accuracy, double confidenceScore, int attemptCount,
                                       int correctCount, int wrongCount, double masteryScore,
                                       LocalDateTime lastAssessedAt) {
            this.id = id;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.topic = topic;
            this.conceptName = conceptName;
            this.masteryLevel = masteryLevel;
            this.status = status;
            this.accuracy = accuracy;
            this.confidenceScore = confidenceScore;
            this.attemptCount = attemptCount;
            this.correctCount = correctCount;
            this.wrongCount = wrongCount;
            this.masteryScore = masteryScore;
            this.lastAssessedAt = lastAssessedAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getConceptName() { return conceptName; }
        public void setConceptName(String conceptName) { this.conceptName = conceptName; }
        public String getMasteryLevel() { return masteryLevel; }
        public void setMasteryLevel(String masteryLevel) { this.masteryLevel = masteryLevel; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
        public int getAttemptCount() { return attemptCount; }
        public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }
        public int getCorrectCount() { return correctCount; }
        public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
        public int getWrongCount() { return wrongCount; }
        public void setWrongCount(int wrongCount) { this.wrongCount = wrongCount; }
        public double getMasteryScore() { return masteryScore; }
        public void setMasteryScore(double masteryScore) { this.masteryScore = masteryScore; }
        public LocalDateTime getLastAssessedAt() { return lastAssessedAt; }
        public void setLastAssessedAt(LocalDateTime lastAssessedAt) { this.lastAssessedAt = lastAssessedAt; }
    }

    // --- 9. Activity DTO ---
    public static class ActivityDTO {
        private String status;
        private LocalDateTime lastActivityAt;
        private Long daysSinceLastActivity;
        private int totalStudySessions;
        private int totalStudyMinutes;

        public ActivityDTO() {
        }

        public ActivityDTO(String status, LocalDateTime lastActivityAt, Long daysSinceLastActivity,
                           int totalStudySessions, int totalStudyMinutes) {
            this.status = status;
            this.lastActivityAt = lastActivityAt;
            this.daysSinceLastActivity = daysSinceLastActivity;
            this.totalStudySessions = totalStudySessions;
            this.totalStudyMinutes = totalStudyMinutes;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getLastActivityAt() { return lastActivityAt; }
        public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
        public Long getDaysSinceLastActivity() { return daysSinceLastActivity; }
        public void setDaysSinceLastActivity(Long daysSinceLastActivity) { this.daysSinceLastActivity = daysSinceLastActivity; }
        public int getTotalStudySessions() { return totalStudySessions; }
        public void setTotalStudySessions(int totalStudySessions) { this.totalStudySessions = totalStudySessions; }
        public int getTotalStudyMinutes() { return totalStudyMinutes; }
        public void setTotalStudyMinutes(int totalStudyMinutes) { this.totalStudyMinutes = totalStudyMinutes; }
    }

    // --- 10. Satisfaction Summary DTO ---
    public static class SatisfactionSummaryDTO {
        private Double averageRating;
        private int totalReviews;
        private Double latestRating;
        private Map<String, Double> byCategory;
        private List<SatisfactionReviewDTO> reviews;

        public SatisfactionSummaryDTO() {
        }

        public SatisfactionSummaryDTO(Double averageRating, int totalReviews, Double latestRating,
                                      Map<String, Double> byCategory, List<SatisfactionReviewDTO> reviews) {
            this.averageRating = averageRating;
            this.totalReviews = totalReviews;
            this.latestRating = latestRating;
            this.byCategory = byCategory;
            this.reviews = reviews;
        }

        public Double getAverageRating() { return averageRating; }
        public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
        public int getTotalReviews() { return totalReviews; }
        public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }
        public Double getLatestRating() { return latestRating; }
        public void setLatestRating(Double latestRating) { this.latestRating = latestRating; }
        public Map<String, Double> getByCategory() { return byCategory; }
        public void setByCategory(Map<String, Double> byCategory) { this.byCategory = byCategory; }
        public List<SatisfactionReviewDTO> getReviews() { return reviews; }
        public void setReviews(List<SatisfactionReviewDTO> reviews) { this.reviews = reviews; }
    }

    public static class SatisfactionReviewDTO {
        private int rating;
        private String feedbackType;
        private String comment;
        private LocalDateTime timestamp;

        public SatisfactionReviewDTO() {
        }

        public SatisfactionReviewDTO(int rating, String feedbackType, String comment, LocalDateTime timestamp) {
            this.rating = rating;
            this.feedbackType = feedbackType;
            this.comment = comment;
            this.timestamp = timestamp;
        }

        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }
        public String getFeedbackType() { return feedbackType; }
        public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }

    // --- 11. Remediation Summary DTO ---
    public static class RemediationSummaryDTO {
        private int totalSessions;
        private int completedSessions;
        private int activeSessions;
        private List<RemediationSessionRecordDTO> sessions;

        public RemediationSummaryDTO() {
        }

        public RemediationSummaryDTO(int totalSessions, int completedSessions, int activeSessions,
                                     List<RemediationSessionRecordDTO> sessions) {
            this.totalSessions = totalSessions;
            this.completedSessions = completedSessions;
            this.activeSessions = activeSessions;
            this.sessions = sessions;
        }

        public int getTotalSessions() { return totalSessions; }
        public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }
        public int getCompletedSessions() { return completedSessions; }
        public void setCompletedSessions(int completedSessions) { this.completedSessions = completedSessions; }
        public int getActiveSessions() { return activeSessions; }
        public void setActiveSessions(int activeSessions) { this.activeSessions = activeSessions; }
        public List<RemediationSessionRecordDTO> getSessions() { return sessions; }
        public void setSessions(List<RemediationSessionRecordDTO> sessions) { this.sessions = sessions; }
    }

    public static class RemediationSessionRecordDTO {
        private String id;
        private String subject;
        private String concept;
        private String moduleType;
        private boolean completed;
        private LocalDateTime createdAt;

        public RemediationSessionRecordDTO() {
        }

        public RemediationSessionRecordDTO(String id, String subject, String concept,
                                           String moduleType, boolean completed, LocalDateTime createdAt) {
            this.id = id;
            this.subject = subject;
            this.concept = concept;
            this.moduleType = moduleType;
            this.completed = completed;
            this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getConcept() { return concept; }
        public void setConcept(String concept) { this.concept = concept; }
        public String getModuleType() { return moduleType; }
        public void setModuleType(String moduleType) { this.moduleType = moduleType; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    // --- 12. Trajectory Point DTO ---
    public static class TrajectoryPointDTO {
        private LocalDateTime timestamp;
        private double overallKnowledgeScore;
        private double engagementScore;
        private Map<String, Double> topicMastery;

        public TrajectoryPointDTO() {
        }

        public TrajectoryPointDTO(LocalDateTime timestamp, double overallKnowledgeScore,
                                  double engagementScore, Map<String, Double> topicMastery) {
            this.timestamp = timestamp;
            this.overallKnowledgeScore = overallKnowledgeScore;
            this.engagementScore = engagementScore;
            this.topicMastery = topicMastery;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public double getOverallKnowledgeScore() { return overallKnowledgeScore; }
        public void setOverallKnowledgeScore(double overallKnowledgeScore) { this.overallKnowledgeScore = overallKnowledgeScore; }
        public double getEngagementScore() { return engagementScore; }
        public void setEngagementScore(double engagementScore) { this.engagementScore = engagementScore; }
        public Map<String, Double> getTopicMastery() { return topicMastery; }
        public void setTopicMastery(Map<String, Double> topicMastery) { this.topicMastery = topicMastery; }
    }

    // --- Top-Level Getters and Setters ---
    public StudentInfoDTO getStudent() { return student; }
    public void setStudent(StudentInfoDTO student) { this.student = student; }
    public KnowledgeGrowthDTO getKnowledge() { return knowledge; }
    public void setKnowledge(KnowledgeGrowthDTO knowledge) { this.knowledge = knowledge; }
    public AssessmentSummaryDTO getAssessmentSummary() { return assessmentSummary; }
    public void setAssessmentSummary(AssessmentSummaryDTO assessmentSummary) { this.assessmentSummary = assessmentSummary; }
    public List<AssessmentRecordDTO> getAssessmentHistory() { return assessmentHistory; }
    public void setAssessmentHistory(List<AssessmentRecordDTO> assessmentHistory) { this.assessmentHistory = assessmentHistory; }
    public QuizSummaryDTO getQuizSummary() { return quizSummary; }
    public void setQuizSummary(QuizSummaryDTO quizSummary) { this.quizSummary = quizSummary; }
    public List<QuizRecordDTO> getQuizHistory() { return quizHistory; }
    public void setQuizHistory(List<QuizRecordDTO> quizHistory) { this.quizHistory = quizHistory; }
    public List<SubjectPerformanceDTO> getSubjectPerformance() { return subjectPerformance; }
    public void setSubjectPerformance(List<SubjectPerformanceDTO> subjectPerformance) { this.subjectPerformance = subjectPerformance; }
    public List<ConceptMasteryRecordDTO> getConceptMastery() { return conceptMastery; }
    public void setConceptMastery(List<ConceptMasteryRecordDTO> conceptMastery) { this.conceptMastery = conceptMastery; }
    public ActivityDTO getActivity() { return activity; }
    public void setActivity(ActivityDTO activity) { this.activity = activity; }
    public SatisfactionSummaryDTO getSatisfaction() { return satisfaction; }
    public void setSatisfaction(SatisfactionSummaryDTO satisfaction) { this.satisfaction = satisfaction; }
    public RemediationSummaryDTO getRemediation() { return remediation; }
    public void setRemediation(RemediationSummaryDTO remediation) { this.remediation = remediation; }
    public List<TrajectoryPointDTO> getTrajectory() { return trajectory; }
    public void setTrajectory(List<TrajectoryPointDTO> trajectory) { this.trajectory = trajectory; }
}
