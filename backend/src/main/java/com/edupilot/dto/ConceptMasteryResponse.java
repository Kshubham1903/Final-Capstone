package com.edupilot.dto;

import com.edupilot.model.ConceptMastery;

import java.time.LocalDateTime;

public class ConceptMasteryResponse {
    private String id;
    private String userId;
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
    private LocalDateTime lastAssessedAt;
    private String recommendedAction;

    public ConceptMasteryResponse() {
    }

    public ConceptMasteryResponse(ConceptMastery cm) {
        if (cm != null) {
            this.id = cm.getId();
            this.userId = cm.getUserId();
            this.subjectCode = cm.getSubjectCode();
            this.subjectName = cm.getSubjectName();
            this.topic = cm.getTopic();
            this.conceptName = cm.getConceptName();
            this.masteryLevel = cm.getMasteryLevel() != null ? cm.getMasteryLevel().name() : "UNKNOWN";
            this.status = cm.getStatus() != null ? cm.getStatus().name() : "UNASSESSED";
            this.accuracy = cm.getAccuracy();
            this.confidenceScore = cm.getConfidenceScore();
            this.attemptCount = cm.getAttemptCount();
            this.correctCount = cm.getCorrectCount();
            this.lastAssessedAt = cm.getLastAssessedAt();
            this.recommendedAction = cm.getRecommendedAction();
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

    public String getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(String masteryLevel) {
        this.masteryLevel = masteryLevel;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public LocalDateTime getLastAssessedAt() {
        return lastAssessedAt;
    }

    public void setLastAssessedAt(LocalDateTime lastAssessedAt) {
        this.lastAssessedAt = lastAssessedAt;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
