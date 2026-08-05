package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "recommendations")
public class Recommendation {
    @Id
    private String id;
    private String userId;
    private String studentProfileId;
    private RecommendationType recommendationType = RecommendationType.CONCEPT_REVISION;
    private Priority priority = Priority.MEDIUM;
    private String subjectCode;
    private String subjectName;
    private String topic;
    private String conceptName;
    private String reason; // Mandatory Explainability Reason
    private String recommendedAction;
    private int estimatedStudyTimeMinutes = 20;
    private String difficulty = "MEDIUM";
    private double confidenceScore = 50.0;
    private Status status = Status.ACTIVE;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

    public enum RecommendationType {
        CONCEPT_REVISION,
        PRACTICE_SET,
        DIAGNOSTIC_RETEST,
        ADVANCED_PRACTICE,
        AI_TUTOR_SESSION,
        REVISION_PLAN
    }

    public enum Priority {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    public enum Status {
        ACTIVE,
        COMPLETED,
        DISMISSED
    }

    public Recommendation() {
    }

    public Recommendation(String id, String userId, String studentProfileId, RecommendationType recommendationType, 
                          Priority priority, String subjectCode, String subjectName, String topic, String conceptName, 
                          String reason, String recommendedAction, int estimatedStudyTimeMinutes, String difficulty, 
                          double confidenceScore, Status status, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.studentProfileId = studentProfileId;
        this.recommendationType = recommendationType;
        this.priority = priority;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.topic = topic;
        this.conceptName = conceptName;
        this.reason = reason;
        this.recommendedAction = recommendedAction;
        this.estimatedStudyTimeMinutes = estimatedStudyTimeMinutes;
        this.difficulty = difficulty;
        this.confidenceScore = confidenceScore;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStudentProfileId() {
        return studentProfileId;
    }

    public void setStudentProfileId(String studentProfileId) {
        this.studentProfileId = studentProfileId;
    }

    public RecommendationType getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(RecommendationType recommendationType) {
        this.recommendationType = recommendationType;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getConceptName() {
        return conceptName;
    }

    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public int getEstimatedStudyTimeMinutes() {
        return estimatedStudyTimeMinutes;
    }

    public void setEstimatedStudyTimeMinutes(int estimatedStudyTimeMinutes) {
        this.estimatedStudyTimeMinutes = estimatedStudyTimeMinutes;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
