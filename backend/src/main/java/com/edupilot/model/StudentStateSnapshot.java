package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "student_state_snapshots")
public class StudentStateSnapshot {
    @Id
    private String id;
    private String studentId;
    private LocalDateTime timestamp = LocalDateTime.now();
    private double overallKnowledgeScore;
    private double engagementScore;
    private Map<String, Double> topicMastery;

    public StudentStateSnapshot() {
    }

    public StudentStateSnapshot(String studentId, double overallKnowledgeScore, double engagementScore, Map<String, Double> topicMastery) {
        this.studentId = studentId;
        this.timestamp = LocalDateTime.now();
        this.overallKnowledgeScore = overallKnowledgeScore;
        this.engagementScore = engagementScore;
        this.topicMastery = topicMastery;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double getOverallKnowledgeScore() {
        return overallKnowledgeScore;
    }

    public void setOverallKnowledgeScore(double overallKnowledgeScore) {
        this.overallKnowledgeScore = overallKnowledgeScore;
    }

    public double getEngagementScore() {
        return engagementScore;
    }

    public void setEngagementScore(double engagementScore) {
        this.engagementScore = engagementScore;
    }

    public Map<String, Double> getTopicMastery() {
        return topicMastery;
    }

    public void setTopicMastery(Map<String, Double> topicMastery) {
        this.topicMastery = topicMastery;
    }
}
