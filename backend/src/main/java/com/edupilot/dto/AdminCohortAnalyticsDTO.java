package com.edupilot.dto;

import java.util.Map;

public class AdminCohortAnalyticsDTO {

    private int totalEnrolled;
    private int evaluatedCohortSize;
    private int activeLast7Days;
    private int atRiskStudents;
    private int inactiveStudents;

    private double meanBaselineKnowledge;
    private double meanCurrentKnowledge;
    private double meanNormalizedGain;

    private GrowthDistributionDTO growthDistribution;
    private SatisfactionDTO satisfaction;
    private String dataSufficiencyNote;

    public static class GrowthDistributionDTO {
        private int improvedCount;
        private double improvedPercentage;
        private int unchangedCount;
        private double unchangedPercentage;
        private int declinedCount;
        private double declinedPercentage;

        public GrowthDistributionDTO() {
        }

        public GrowthDistributionDTO(int improvedCount, double improvedPercentage,
                                     int unchangedCount, double unchangedPercentage,
                                     int declinedCount, double declinedPercentage) {
            this.improvedCount = improvedCount;
            this.improvedPercentage = improvedPercentage;
            this.unchangedCount = unchangedCount;
            this.unchangedPercentage = unchangedPercentage;
            this.declinedCount = declinedCount;
            this.declinedPercentage = declinedPercentage;
        }

        public int getImprovedCount() {
            return improvedCount;
        }

        public void setImprovedCount(int improvedCount) {
            this.improvedCount = improvedCount;
        }

        public double getImprovedPercentage() {
            return improvedPercentage;
        }

        public void setImprovedPercentage(double improvedPercentage) {
            this.improvedPercentage = improvedPercentage;
        }

        public int getUnchangedCount() {
            return unchangedCount;
        }

        public void setUnchangedCount(int unchangedCount) {
            this.unchangedCount = unchangedCount;
        }

        public double getUnchangedPercentage() {
            return unchangedPercentage;
        }

        public void setUnchangedPercentage(double unchangedPercentage) {
            this.unchangedPercentage = unchangedPercentage;
        }

        public int getDeclinedCount() {
            return declinedCount;
        }

        public void setDeclinedCount(int declinedCount) {
            this.declinedCount = declinedCount;
        }

        public double getDeclinedPercentage() {
            return declinedPercentage;
        }

        public void setDeclinedPercentage(double declinedPercentage) {
            this.declinedPercentage = declinedPercentage;
        }
    }

    public static class SatisfactionDTO {
        private double averageRating;
        private int totalReviews;
        private Map<String, Double> byCategory;

        public SatisfactionDTO() {
        }

        public SatisfactionDTO(double averageRating, int totalReviews, Map<String, Double> byCategory) {
            this.averageRating = averageRating;
            this.totalReviews = totalReviews;
            this.byCategory = byCategory;
        }

        public double getAverageRating() {
            return averageRating;
        }

        public void setAverageRating(double averageRating) {
            this.averageRating = averageRating;
        }

        public int getTotalReviews() {
            return totalReviews;
        }

        public void setTotalReviews(int totalReviews) {
            this.totalReviews = totalReviews;
        }

        public Map<String, Double> getByCategory() {
            return byCategory;
        }

        public void setByCategory(Map<String, Double> byCategory) {
            this.byCategory = byCategory;
        }
    }

    public AdminCohortAnalyticsDTO() {
    }

    public AdminCohortAnalyticsDTO(int totalEnrolled, int evaluatedCohortSize, int activeLast7Days,
                                   int atRiskStudents, int inactiveStudents, double meanBaselineKnowledge,
                                   double meanCurrentKnowledge, double meanNormalizedGain,
                                   GrowthDistributionDTO growthDistribution, SatisfactionDTO satisfaction,
                                   String dataSufficiencyNote) {
        this.totalEnrolled = totalEnrolled;
        this.evaluatedCohortSize = evaluatedCohortSize;
        this.activeLast7Days = activeLast7Days;
        this.atRiskStudents = atRiskStudents;
        this.inactiveStudents = inactiveStudents;
        this.meanBaselineKnowledge = meanBaselineKnowledge;
        this.meanCurrentKnowledge = meanCurrentKnowledge;
        this.meanNormalizedGain = meanNormalizedGain;
        this.growthDistribution = growthDistribution;
        this.satisfaction = satisfaction;
        this.dataSufficiencyNote = dataSufficiencyNote;
    }

    public int getTotalEnrolled() {
        return totalEnrolled;
    }

    public void setTotalEnrolled(int totalEnrolled) {
        this.totalEnrolled = totalEnrolled;
    }

    public int getEvaluatedCohortSize() {
        return evaluatedCohortSize;
    }

    public void setEvaluatedCohortSize(int evaluatedCohortSize) {
        this.evaluatedCohortSize = evaluatedCohortSize;
    }

    public int getActiveLast7Days() {
        return activeLast7Days;
    }

    public void setActiveLast7Days(int activeLast7Days) {
        this.activeLast7Days = activeLast7Days;
    }

    public int getAtRiskStudents() {
        return atRiskStudents;
    }

    public void setAtRiskStudents(int atRiskStudents) {
        this.atRiskStudents = atRiskStudents;
    }

    public int getInactiveStudents() {
        return inactiveStudents;
    }

    public void setInactiveStudents(int inactiveStudents) {
        this.inactiveStudents = inactiveStudents;
    }

    public double getMeanBaselineKnowledge() {
        return meanBaselineKnowledge;
    }

    public void setMeanBaselineKnowledge(double meanBaselineKnowledge) {
        this.meanBaselineKnowledge = meanBaselineKnowledge;
    }

    public double getMeanCurrentKnowledge() {
        return meanCurrentKnowledge;
    }

    public void setMeanCurrentKnowledge(double meanCurrentKnowledge) {
        this.meanCurrentKnowledge = meanCurrentKnowledge;
    }

    public double getMeanNormalizedGain() {
        return meanNormalizedGain;
    }

    public void setMeanNormalizedGain(double meanNormalizedGain) {
        this.meanNormalizedGain = meanNormalizedGain;
    }

    public GrowthDistributionDTO getGrowthDistribution() {
        return growthDistribution;
    }

    public void setGrowthDistribution(GrowthDistributionDTO growthDistribution) {
        this.growthDistribution = growthDistribution;
    }

    public SatisfactionDTO getSatisfaction() {
        return satisfaction;
    }

    public void setSatisfaction(SatisfactionDTO satisfaction) {
        this.satisfaction = satisfaction;
    }

    public String getDataSufficiencyNote() {
        return dataSufficiencyNote;
    }

    public void setDataSufficiencyNote(String dataSufficiencyNote) {
        this.dataSufficiencyNote = dataSufficiencyNote;
    }
}
