package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "concept_mastery")
public class ConceptMastery {
    @Id
    private String id;
    private String userId;
    private String studentProfileId;
    private String subjectCode;
    private String subjectName;
    private String topic;
    private String conceptName;
    private MasteryLevel masteryLevel = MasteryLevel.UNKNOWN;
    private ConceptStatus status = ConceptStatus.UNASSESSED;
    private double accuracy = 0.0;
    private double confidenceScore = 0.0;
    private int attemptCount = 0;
    private int correctCount = 0;
    private int wrongCount = 0;
    private int recentWrongAnswerCount = 0;
    private double masteryScore = 50.0;
    private LocalDateTime lastAssessedAt;
    private String recommendedAction;

    public enum MasteryLevel {
        UNKNOWN,
        BEGINNER,
        INTERMEDIATE,
        PROFICIENT,
        MASTER
    }

    public enum ConceptStatus {
        UNASSESSED,
        UNCERTAIN,
        WEAK,
        STRONG
    }

    public ConceptMastery() {
    }

    public ConceptMastery(String id, String userId, String studentProfileId, String subjectCode, String subjectName, 
                          String topic, String conceptName, MasteryLevel masteryLevel, double accuracy, 
                          double confidenceScore, int attemptCount, int correctCount, LocalDateTime lastAssessedAt, 
                          String recommendedAction) {
        this.id = id;
        this.userId = userId;
        this.studentProfileId = studentProfileId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.topic = topic;
        this.conceptName = conceptName;
        this.masteryLevel = masteryLevel;
        this.accuracy = accuracy;
        this.confidenceScore = confidenceScore;
        this.attemptCount = attemptCount;
        this.correctCount = correctCount;
        this.lastAssessedAt = lastAssessedAt;
        this.recommendedAction = recommendedAction;
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

    public MasteryLevel getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(MasteryLevel masteryLevel) {
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

    public int getWrongCount() {
        return wrongCount;
    }

    public void setWrongCount(int wrongCount) {
        this.wrongCount = wrongCount;
    }

    public int getRecentWrongAnswerCount() {
        return recentWrongAnswerCount;
    }

    public void setRecentWrongAnswerCount(int recentWrongAnswerCount) {
        this.recentWrongAnswerCount = recentWrongAnswerCount;
    }

    public double getMasteryScore() {
        return masteryScore;
    }

    public void setMasteryScore(double masteryScore) {
        this.masteryScore = masteryScore;
    }

    public ConceptStatus getStatus() {
        return status;
    }

    public void setStatus(ConceptStatus status) {
        this.status = status;
    }
}
