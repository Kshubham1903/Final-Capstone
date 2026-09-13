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
    private String modality;
    private String pace;
    private String workload;
    private String whatExplanation;
    private String howExplanation;
    private String paceExplanation;
    private double confidenceScore;
    private double masteryScore;
    private double accuracy;
    private String prevTopic;
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
            this.modality = rec.getModality() != null ? rec.getModality() : "VISUAL";
            this.pace = rec.getPace() != null ? rec.getPace() : "NORMAL";
            this.workload = rec.getWorkload() != null ? rec.getWorkload() : "MEDIUM";
            this.whatExplanation = rec.getWhatExplanation();
            this.howExplanation = rec.getHowExplanation();
            this.paceExplanation = rec.getPaceExplanation();
            this.confidenceScore = rec.getConfidenceScore();
            this.masteryScore = rec.getMasteryScore();
            this.accuracy = rec.getAccuracy();
            this.prevTopic = rec.getPrevTopic();
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

    public double getMasteryScore() {
        return masteryScore;
    }

    public void setMasteryScore(double masteryScore) {
        this.masteryScore = masteryScore;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public String getPrevTopic() {
        return prevTopic;
    }

    public void setPrevTopic(String prevTopic) {
        this.prevTopic = prevTopic;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public String getPace() {
        return pace;
    }

    public void setPace(String pace) {
        this.pace = pace;
    }

    public String getWorkload() {
        return workload;
    }

    public void setWorkload(String workload) {
        this.workload = workload;
    }

    public String getWhatExplanation() {
        return whatExplanation;
    }

    public void setWhatExplanation(String whatExplanation) {
        this.whatExplanation = whatExplanation;
    }

    public String getHowExplanation() {
        return howExplanation;
    }

    public void setHowExplanation(String howExplanation) {
        this.howExplanation = howExplanation;
    }

    public String getPaceExplanation() {
        return paceExplanation;
    }

    public void setPaceExplanation(String paceExplanation) {
        this.paceExplanation = paceExplanation;
    }
}
