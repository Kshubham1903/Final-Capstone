package com.edupilot.dto;

import java.util.List;

public class AdminCohortComparisonDTO {

    private CohortDataDTO cohortA;
    private CohortDataDTO cohortB;
    private DifferencesDTO differences;
    private DataSufficiencyDTO dataSufficiency;
    private String methodologyNote;

    public AdminCohortComparisonDTO() {
    }

    public AdminCohortComparisonDTO(
            CohortDataDTO cohortA,
            CohortDataDTO cohortB,
            DifferencesDTO differences,
            DataSufficiencyDTO dataSufficiency,
            String methodologyNote
    ) {
        this.cohortA = cohortA;
        this.cohortB = cohortB;
        this.differences = differences;
        this.dataSufficiency = dataSufficiency;
        this.methodologyNote = methodologyNote;
    }

    public static class CohortDataDTO {
        private String label;
        private AdminAnalyticsFilterCriteria criteria;
        private CohortMetricsDTO metrics;

        public CohortDataDTO() {
        }

        public CohortDataDTO(String label, AdminAnalyticsFilterCriteria criteria, CohortMetricsDTO metrics) {
            this.label = label;
            this.criteria = criteria;
            this.metrics = metrics;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public AdminAnalyticsFilterCriteria getCriteria() {
            return criteria;
        }

        public void setCriteria(AdminAnalyticsFilterCriteria criteria) {
            this.criteria = criteria;
        }

        public CohortMetricsDTO getMetrics() {
            return metrics;
        }

        public void setMetrics(CohortMetricsDTO metrics) {
            this.metrics = metrics;
        }
    }

    public static class CohortMetricsDTO {
        private int totalEnrolled;
        private int evaluatedCohortSize;
        private int baselineSampleSize;
        private int currentKnowledgeSampleSize;
        private int learningGainSampleSize;
        private Double meanBaselineKnowledge;
        private Double meanCurrentKnowledge;
        private Double meanNormalizedGain;
        private AdminCohortAnalyticsDTO.GrowthDistributionDTO growthDistribution;
        private AdminCohortAnalyticsDTO.SatisfactionDTO satisfaction;
        private int satisfactionSampleSize;

        public CohortMetricsDTO() {
        }

        public CohortMetricsDTO(
                int totalEnrolled,
                int evaluatedCohortSize,
                int baselineSampleSize,
                int currentKnowledgeSampleSize,
                int learningGainSampleSize,
                Double meanBaselineKnowledge,
                Double meanCurrentKnowledge,
                Double meanNormalizedGain,
                AdminCohortAnalyticsDTO.GrowthDistributionDTO growthDistribution,
                AdminCohortAnalyticsDTO.SatisfactionDTO satisfaction,
                int satisfactionSampleSize
        ) {
            this.totalEnrolled = totalEnrolled;
            this.evaluatedCohortSize = evaluatedCohortSize;
            this.baselineSampleSize = baselineSampleSize;
            this.currentKnowledgeSampleSize = currentKnowledgeSampleSize;
            this.learningGainSampleSize = learningGainSampleSize;
            this.meanBaselineKnowledge = meanBaselineKnowledge;
            this.meanCurrentKnowledge = meanCurrentKnowledge;
            this.meanNormalizedGain = meanNormalizedGain;
            this.growthDistribution = growthDistribution;
            this.satisfaction = satisfaction;
            this.satisfactionSampleSize = satisfactionSampleSize;
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

        public int getBaselineSampleSize() {
            return baselineSampleSize;
        }

        public void setBaselineSampleSize(int baselineSampleSize) {
            this.baselineSampleSize = baselineSampleSize;
        }

        public int getCurrentKnowledgeSampleSize() {
            return currentKnowledgeSampleSize;
        }

        public void setCurrentKnowledgeSampleSize(int currentKnowledgeSampleSize) {
            this.currentKnowledgeSampleSize = currentKnowledgeSampleSize;
        }

        public int getLearningGainSampleSize() {
            return learningGainSampleSize;
        }

        public void setLearningGainSampleSize(int learningGainSampleSize) {
            this.learningGainSampleSize = learningGainSampleSize;
        }

        public Double getMeanBaselineKnowledge() {
            return meanBaselineKnowledge;
        }

        public void setMeanBaselineKnowledge(Double meanBaselineKnowledge) {
            this.meanBaselineKnowledge = meanBaselineKnowledge;
        }

        public Double getMeanCurrentKnowledge() {
            return meanCurrentKnowledge;
        }

        public void setMeanCurrentKnowledge(Double meanCurrentKnowledge) {
            this.meanCurrentKnowledge = meanCurrentKnowledge;
        }

        public Double getMeanNormalizedGain() {
            return meanNormalizedGain;
        }

        public void setMeanNormalizedGain(Double meanNormalizedGain) {
            this.meanNormalizedGain = meanNormalizedGain;
        }

        public AdminCohortAnalyticsDTO.GrowthDistributionDTO getGrowthDistribution() {
            return growthDistribution;
        }

        public void setGrowthDistribution(AdminCohortAnalyticsDTO.GrowthDistributionDTO growthDistribution) {
            this.growthDistribution = growthDistribution;
        }

        public AdminCohortAnalyticsDTO.SatisfactionDTO getSatisfaction() {
            return satisfaction;
        }

        public void setSatisfaction(AdminCohortAnalyticsDTO.SatisfactionDTO satisfaction) {
            this.satisfaction = satisfaction;
        }

        public int getSatisfactionSampleSize() {
            return satisfactionSampleSize;
        }

        public void setSatisfactionSampleSize(int satisfactionSampleSize) {
            this.satisfactionSampleSize = satisfactionSampleSize;
        }
    }

    public static class DifferencesDTO {
        private Integer totalEnrolledDiff;
        private Integer evaluatedCohortSizeDiff;
        private Double baselineKnowledgeDiffPp;
        private Double currentKnowledgeDiffPp;
        private Double normalizedGainDiff;
        private Double satisfactionDiff;

        public DifferencesDTO() {
        }

        public DifferencesDTO(
                Integer totalEnrolledDiff,
                Integer evaluatedCohortSizeDiff,
                Double baselineKnowledgeDiffPp,
                Double currentKnowledgeDiffPp,
                Double normalizedGainDiff,
                Double satisfactionDiff
        ) {
            this.totalEnrolledDiff = totalEnrolledDiff;
            this.evaluatedCohortSizeDiff = evaluatedCohortSizeDiff;
            this.baselineKnowledgeDiffPp = baselineKnowledgeDiffPp;
            this.currentKnowledgeDiffPp = currentKnowledgeDiffPp;
            this.normalizedGainDiff = normalizedGainDiff;
            this.satisfactionDiff = satisfactionDiff;
        }

        public Integer getTotalEnrolledDiff() {
            return totalEnrolledDiff;
        }

        public void setTotalEnrolledDiff(Integer totalEnrolledDiff) {
            this.totalEnrolledDiff = totalEnrolledDiff;
        }

        public Integer getEvaluatedCohortSizeDiff() {
            return evaluatedCohortSizeDiff;
        }

        public void setEvaluatedCohortSizeDiff(Integer evaluatedCohortSizeDiff) {
            this.evaluatedCohortSizeDiff = evaluatedCohortSizeDiff;
        }

        public Double getBaselineKnowledgeDiffPp() {
            return baselineKnowledgeDiffPp;
        }

        public void setBaselineKnowledgeDiffPp(Double baselineKnowledgeDiffPp) {
            this.baselineKnowledgeDiffPp = baselineKnowledgeDiffPp;
        }

        public Double getCurrentKnowledgeDiffPp() {
            return currentKnowledgeDiffPp;
        }

        public void setCurrentKnowledgeDiffPp(Double currentKnowledgeDiffPp) {
            this.currentKnowledgeDiffPp = currentKnowledgeDiffPp;
        }

        public Double getNormalizedGainDiff() {
            return normalizedGainDiff;
        }

        public void setNormalizedGainDiff(Double normalizedGainDiff) {
            this.normalizedGainDiff = normalizedGainDiff;
        }

        public Double getSatisfactionDiff() {
            return satisfactionDiff;
        }

        public void setSatisfactionDiff(Double satisfactionDiff) {
            this.satisfactionDiff = satisfactionDiff;
        }
    }

    public static class DataSufficiencyDTO {
        private boolean isComparisonValid;
        private List<String> warnings;

        public DataSufficiencyDTO() {
        }

        public DataSufficiencyDTO(boolean isComparisonValid, List<String> warnings) {
            this.isComparisonValid = isComparisonValid;
            this.warnings = warnings;
        }

        public boolean isComparisonValid() {
            return isComparisonValid;
        }

        public void setComparisonValid(boolean comparisonValid) {
            isComparisonValid = comparisonValid;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }
    }

    public CohortDataDTO getCohortA() {
        return cohortA;
    }

    public void setCohortA(CohortDataDTO cohortA) {
        this.cohortA = cohortA;
    }

    public CohortDataDTO getCohortB() {
        return cohortB;
    }

    public void setCohortB(CohortDataDTO cohortB) {
        this.cohortB = cohortB;
    }

    public DifferencesDTO getDifferences() {
        return differences;
    }

    public void setDifferences(DifferencesDTO differences) {
        this.differences = differences;
    }

    public DataSufficiencyDTO getDataSufficiency() {
        return dataSufficiency;
    }

    public void setDataSufficiency(DataSufficiencyDTO dataSufficiency) {
        this.dataSufficiency = dataSufficiency;
    }

    public String getMethodologyNote() {
        return methodologyNote;
    }

    public void setMethodologyNote(String methodologyNote) {
        this.methodologyNote = methodologyNote;
    }
}
