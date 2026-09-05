package com.edupilot.dto;

import com.edupilot.model.AssessmentResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AssessmentResultResponse {
    private String id;
    private String sessionId;
    private String userId;
    private String branch;
    private int semester;
    private String subjectCode;
    private String subjectName;
    private int totalQuestions;
    private int correctAnswers;
    private int incorrectAnswers;
    private int skippedQuestions;
    private int score;
    private int totalMarks;
    private double percentage;
    private double accuracy;
    private int timeTakenSeconds;
    private String masteryLevel;
    private Map<String, Map<String, Object>> topicBreakdown;
    private List<AssessmentResult.UserAnswer> userAnswers;
    private LocalDateTime createdAt;

    // Adaptive Assessment Handoff Bridge Fields
    private boolean adaptiveEligible = false;
    private List<String> targetAdaptiveConcepts;
    private List<ConceptEvaluationDTO> conceptEvaluations;

    public static class ConceptEvaluationDTO {
        private String concept;
        private double accuracy;
        private int attempts;
        private String masteryLevel;
        private double confidence;
        private String status;
        private boolean requiresAdaptiveTesting;

        public ConceptEvaluationDTO() {
        }

        public ConceptEvaluationDTO(String concept, double accuracy, int attempts, String masteryLevel, double confidence, String status, boolean requiresAdaptiveTesting) {
            this.concept = concept;
            this.accuracy = accuracy;
            this.attempts = attempts;
            this.masteryLevel = masteryLevel;
            this.confidence = confidence;
            this.status = status;
            this.requiresAdaptiveTesting = requiresAdaptiveTesting;
        }

        public String getConcept() { return concept; }
        public void setConcept(String concept) { this.concept = concept; }
        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
        public int getAttempts() { return attempts; }
        public void setAttempts(int attempts) { this.attempts = attempts; }
        public String getMasteryLevel() { return masteryLevel; }
        public void setMasteryLevel(String masteryLevel) { this.masteryLevel = masteryLevel; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public boolean isRequiresAdaptiveTesting() { return requiresAdaptiveTesting; }
        public void setRequiresAdaptiveTesting(boolean requiresAdaptiveTesting) { this.requiresAdaptiveTesting = requiresAdaptiveTesting; }
    }

    public AssessmentResultResponse() {
    }

    public AssessmentResultResponse(AssessmentResult res) {
        if (res != null) {
            this.id = res.getId();
            this.sessionId = res.getSessionId();
            this.userId = res.getUserId();
            this.branch = res.getBranch();
            this.semester = res.getSemester();
            this.subjectCode = res.getSubjectCode();
            this.subjectName = res.getSubjectName();
            this.totalQuestions = res.getTotalQuestions();
            this.correctAnswers = res.getCorrectAnswers();
            this.incorrectAnswers = res.getIncorrectAnswers();
            this.skippedQuestions = res.getSkippedQuestions();
            this.score = res.getScore();
            this.totalMarks = res.getTotalMarks();
            this.percentage = res.getPercentage();
            this.accuracy = res.getAccuracy();
            this.timeTakenSeconds = res.getTimeTakenSeconds();
            this.masteryLevel = res.getMasteryLevel();
            this.topicBreakdown = res.getTopicBreakdown();
            this.userAnswers = res.getUserAnswers();
            this.createdAt = res.getCreatedAt();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public int getIncorrectAnswers() {
        return incorrectAnswers;
    }

    public void setIncorrectAnswers(int incorrectAnswers) {
        this.incorrectAnswers = incorrectAnswers;
    }

    public int getSkippedQuestions() {
        return skippedQuestions;
    }

    public void setSkippedQuestions(int skippedQuestions) {
        this.skippedQuestions = skippedQuestions;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public int getTimeTakenSeconds() {
        return timeTakenSeconds;
    }

    public void setTimeTakenSeconds(int timeTakenSeconds) {
        this.timeTakenSeconds = timeTakenSeconds;
    }

    public String getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(String masteryLevel) {
        this.masteryLevel = masteryLevel;
    }

    public Map<String, Map<String, Object>> getTopicBreakdown() {
        return topicBreakdown;
    }

    public void setTopicBreakdown(Map<String, Map<String, Object>> topicBreakdown) {
        this.topicBreakdown = topicBreakdown;
    }

    public List<AssessmentResult.UserAnswer> getUserAnswers() {
        return userAnswers;
    }

    public void setUserAnswers(List<AssessmentResult.UserAnswer> userAnswers) {
        this.userAnswers = userAnswers;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdaptiveEligible() {
        return adaptiveEligible;
    }

    public void setAdaptiveEligible(boolean adaptiveEligible) {
        this.adaptiveEligible = adaptiveEligible;
    }

    public List<String> getTargetAdaptiveConcepts() {
        return targetAdaptiveConcepts;
    }

    public void setTargetAdaptiveConcepts(List<String> targetAdaptiveConcepts) {
        this.targetAdaptiveConcepts = targetAdaptiveConcepts;
    }

    public List<ConceptEvaluationDTO> getConceptEvaluations() {
        return conceptEvaluations;
    }

    public void setConceptEvaluations(List<ConceptEvaluationDTO> conceptEvaluations) {
        this.conceptEvaluations = conceptEvaluations;
    }
}
