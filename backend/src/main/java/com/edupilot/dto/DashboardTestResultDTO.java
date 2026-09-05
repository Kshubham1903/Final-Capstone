package com.edupilot.dto;

import java.util.Map;

public class DashboardTestResultDTO {
    private String sessionId;
    private String studentId;
    private Map<String, Double> subjectScorePercentage;
    private Map<String, Integer> correctCountPerSubject;
    private int totalQuestions;
    private int totalCorrect;
    private double overallPercentage;
    private String createdAt;

    public DashboardTestResultDTO() {
    }

    public DashboardTestResultDTO(String sessionId, String studentId, Map<String, Double> subjectScorePercentage, 
                                  Map<String, Integer> correctCountPerSubject, int totalQuestions, 
                                  int totalCorrect, double overallPercentage, String createdAt) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.subjectScorePercentage = subjectScorePercentage;
        this.correctCountPerSubject = correctCountPerSubject;
        this.totalQuestions = totalQuestions;
        this.totalCorrect = totalCorrect;
        this.overallPercentage = overallPercentage;
        this.createdAt = createdAt;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
