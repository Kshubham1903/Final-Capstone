package com.edupilot.dto;

public class AdminAnalyticsOverviewDTO {
    private int totalStudents;
    private int activeStudentsLast7Days;
    private int totalAssessmentsCompleted;
    private int totalQuizzesCompleted;
    private double cohortAverageBaselineKnowledge;
    private double cohortAverageCurrentKnowledge;
    private double cohortAverageLearningGain;
    private double averageSatisfactionRating;
    private int atRiskStudentCount;

    public AdminAnalyticsOverviewDTO() {
    }

    public AdminAnalyticsOverviewDTO(int totalStudents, int activeStudentsLast7Days,
                                     int totalAssessmentsCompleted, int totalQuizzesCompleted,
                                     double cohortAverageBaselineKnowledge, double cohortAverageCurrentKnowledge,
                                     double cohortAverageLearningGain, double averageSatisfactionRating,
                                     int atRiskStudentCount) {
        this.totalStudents = totalStudents;
        this.activeStudentsLast7Days = activeStudentsLast7Days;
        this.totalAssessmentsCompleted = totalAssessmentsCompleted;
        this.totalQuizzesCompleted = totalQuizzesCompleted;
        this.cohortAverageBaselineKnowledge = cohortAverageBaselineKnowledge;
        this.cohortAverageCurrentKnowledge = cohortAverageCurrentKnowledge;
        this.cohortAverageLearningGain = cohortAverageLearningGain;
        this.averageSatisfactionRating = averageSatisfactionRating;
        this.atRiskStudentCount = atRiskStudentCount;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    public int getActiveStudentsLast7Days() {
        return activeStudentsLast7Days;
    }

    public void setActiveStudentsLast7Days(int activeStudentsLast7Days) {
        this.activeStudentsLast7Days = activeStudentsLast7Days;
    }

    public int getTotalAssessmentsCompleted() {
        return totalAssessmentsCompleted;
    }

    public void setTotalAssessmentsCompleted(int totalAssessmentsCompleted) {
        this.totalAssessmentsCompleted = totalAssessmentsCompleted;
    }

    public int getTotalQuizzesCompleted() {
        return totalQuizzesCompleted;
    }

    public void setTotalQuizzesCompleted(int totalQuizzesCompleted) {
        this.totalQuizzesCompleted = totalQuizzesCompleted;
    }

    public double getCohortAverageBaselineKnowledge() {
        return cohortAverageBaselineKnowledge;
    }

    public void setCohortAverageBaselineKnowledge(double cohortAverageBaselineKnowledge) {
        this.cohortAverageBaselineKnowledge = cohortAverageBaselineKnowledge;
    }

    public double getCohortAverageCurrentKnowledge() {
        return cohortAverageCurrentKnowledge;
    }

    public void setCohortAverageCurrentKnowledge(double cohortAverageCurrentKnowledge) {
        this.cohortAverageCurrentKnowledge = cohortAverageCurrentKnowledge;
    }

    public double getCohortAverageLearningGain() {
        return cohortAverageLearningGain;
    }

    public void setCohortAverageLearningGain(double cohortAverageLearningGain) {
        this.cohortAverageLearningGain = cohortAverageLearningGain;
    }

    public double getAverageSatisfactionRating() {
        return averageSatisfactionRating;
    }

    public void setAverageSatisfactionRating(double averageSatisfactionRating) {
        this.averageSatisfactionRating = averageSatisfactionRating;
    }

    public int getAtRiskStudentCount() {
        return atRiskStudentCount;
    }

    public void setAtRiskStudentCount(int atRiskStudentCount) {
        this.atRiskStudentCount = atRiskStudentCount;
    }
}
