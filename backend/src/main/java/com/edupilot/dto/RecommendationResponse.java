package com.edupilot.dto;

import com.edupilot.model.Recommendation;

import java.time.LocalDateTime;

public class RecommendationResponse {
    private String id;
    private String userId;
    private String recommendationType;
    private String priority;
    private String subjectCode;
    private String subjectName;
    private String topic;
    private String conceptName;
    private String reason; // Mandatory Explainability Reason
    private String recommendedAction;
    private int estimatedStudyTimeMinutes;
    private String difficulty;
    private double confidenceScore;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public RecommendationResponse() {
    }

    public RecommendationResponse(Recommendation rec) {
        if (rec != null) {
            this.id = rec.getId();
            this.userId = rec.getUserId();
            this.recommendationType = rec.getRecommendationType() != null ? rec.getRecommendationType().name() : "CONCEPT_REVISION";
            this.priority = rec.getPriority() != null ? rec.getPriority().name() : "MEDIUM";
            this.subjectCode = rec.getSubjectCode();
            this.subjectName = rec.getSubjectName();
            this.topic = rec.getTopic();
            this.conceptName = rec.getConceptName();
            this.reason = rec.getReason();
            this.recommendedAction = rec.getRecommendedAction();
            this.estimatedStudyTimeMinutes = rec.getEstimatedStudyTimeMinutes();
            this.difficulty = rec.getDifficulty();
            this.confidenceScore = rec.getConfidenceScore();
            this.status = rec.getStatus() != null ? rec.getStatus().name() : "ACTIVE";
            this.createdAt = rec.getCreatedAt();
            this.expiresAt = rec.getExpiresAt();
        }
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

    public String getRecommendationType() {
        return recommendationType;
    }

    public void setRecommendationType(String recommendationType) {
        this.recommendationType = recommendationType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
