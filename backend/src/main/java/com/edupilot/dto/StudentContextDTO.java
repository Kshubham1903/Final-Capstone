package com.edupilot.dto;

import java.util.ArrayList;
import java.util.List;

public class StudentContextDTO {
    private String studentId;
    private String studentName = "Student";
    private String degree = "B.Tech";
    private String branch = "Computer Science & Engineering";
    private int semester = 1;
    private double currentCgpa = 8.0;
    private double targetCgpa = 8.5;
    private double sgi = 7.5;
    private String riskLevel = "LOW";
    private double learningHealthScore = 75.0;
    private List<String> strongConcepts = new ArrayList<>();
    private List<String> weakConcepts = new ArrayList<>();
    private List<String> activeRecommendations = new ArrayList<>();
    private String todayFocusTask = "Core Concept Revision";
    private String activeSubject = "Data Structures & Algorithms";
    private String conversationSummary;

    public StudentContextDTO() {
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
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

    public double getCurrentCgpa() {
        return currentCgpa;
    }

    public void setCurrentCgpa(double currentCgpa) {
        this.currentCgpa = currentCgpa;
    }

    public double getTargetCgpa() {
        return targetCgpa;
    }

    public void setTargetCgpa(double targetCgpa) {
        this.targetCgpa = targetCgpa;
    }

    public double getSgi() {
        return sgi;
    }

    public void setSgi(double sgi) {
        this.sgi = sgi;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getLearningHealthScore() {
        return learningHealthScore;
    }

    public void setLearningHealthScore(double learningHealthScore) {
        this.learningHealthScore = learningHealthScore;
    }

    public List<String> getStrongConcepts() {
        return strongConcepts;
    }

    public void setStrongConcepts(List<String> strongConcepts) {
        this.strongConcepts = strongConcepts;
    }

    public List<String> getWeakConcepts() {
        return weakConcepts;
    }

    public void setWeakConcepts(List<String> weakConcepts) {
        this.weakConcepts = weakConcepts;
    }

    public List<String> getActiveRecommendations() {
        return activeRecommendations;
    }

    public void setActiveRecommendations(List<String> activeRecommendations) {
        this.activeRecommendations = activeRecommendations;
    }

    public String getTodayFocusTask() {
        return todayFocusTask;
    }

    public void setTodayFocusTask(String todayFocusTask) {
        this.todayFocusTask = todayFocusTask;
    }

    public String getActiveSubject() {
        return activeSubject;
    }

    public void setActiveSubject(String activeSubject) {
        this.activeSubject = activeSubject;
    }

    public String getConversationSummary() {
        return conversationSummary;
    }

    public void setConversationSummary(String conversationSummary) {
        this.conversationSummary = conversationSummary;
    }
}
