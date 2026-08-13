package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "dashboard_test_results")
public class DashboardTestResult {
    @Id
    private String id;
    private String sessionId;
    private String studentId;
    private Map<String, Double> subjectScorePercentage;
    private Map<String, Integer> correctCountPerSubject;
    private int totalQuestions;
    private int totalCorrect;
    private double overallPercentage;
    private LocalDateTime createdAt = LocalDateTime.now();

    public DashboardTestResult() {
    }

    public DashboardTestResult(String id, String sessionId, String studentId, Map<String, Double> subjectScorePercentage, 
                               Map<String, Integer> correctCountPerSubject, int totalQuestions, 
                               int totalCorrect, double overallPercentage, LocalDateTime createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.subjectScorePercentage = subjectScorePercentage;
        this.correctCountPerSubject = correctCountPerSubject;
        this.totalQuestions = totalQuestions;
        this.totalCorrect = totalCorrect;
        this.overallPercentage = overallPercentage;
        this.createdAt = createdAt;
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

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Map<String, Double> getSubjectScorePercentage() {
        return subjectScorePercentage;
    }

    public void setSubjectScorePercentage(Map<String, Double> subjectScorePercentage) {
        this.subjectScorePercentage = subjectScorePercentage;
    }

    public Map<String, Integer> getCorrectCountPerSubject() {
        return correctCountPerSubject;
    }

    public void setCorrectCountPerSubject(Map<String, Integer> correctCountPerSubject) {
        this.correctCountPerSubject = correctCountPerSubject;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getTotalCorrect() {
        return totalCorrect;
    }

    public void setTotalCorrect(int totalCorrect) {
        this.totalCorrect = totalCorrect;
    }

    public double getOverallPercentage() {
        return overallPercentage;
    }

    public void setOverallPercentage(double overallPercentage) {
        this.overallPercentage = overallPercentage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
