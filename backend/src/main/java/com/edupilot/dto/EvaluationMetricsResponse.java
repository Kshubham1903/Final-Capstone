package com.edupilot.dto;

import java.util.List;

public class EvaluationMetricsResponse {
    private String studentId;
    private double completionRate;
    private int totalRecommendations;
    private int completedRecommendations;
    private Double averageTimeToMasteryMinutes;
    private List<TopicTimeToMasteryDTO> topicTimeToMastery;
    private Double overallRetention;
    private List<TopicRetentionDTO> topicRetention;
    private Double averageSatisfaction;
    private int satisfactionResponseCount;
    private String lastActivityAt;
    private Long daysSinceLastActivity;
    private String activityStatus;
    private Double dropoutRisk;
    private Double dropoutRate;

    public EvaluationMetricsResponse() {
    }

    public EvaluationMetricsResponse(String studentId, double completionRate, int totalRecommendations, int completedRecommendations, 
                                     Double averageTimeToMasteryMinutes, List<TopicTimeToMasteryDTO> topicTimeToMastery, 
                                     Double overallRetention, List<TopicRetentionDTO> topicRetention,
                                     Double averageSatisfaction, int satisfactionResponseCount,
                                     String lastActivityAt, Long daysSinceLastActivity, String activityStatus,
                                     Double dropoutRisk, Double dropoutRate) {
        this.studentId = studentId;
        this.completionRate = completionRate;
        this.totalRecommendations = totalRecommendations;
        this.completedRecommendations = completedRecommendations;
        this.averageTimeToMasteryMinutes = averageTimeToMasteryMinutes;
        this.topicTimeToMastery = topicTimeToMastery;
        this.overallRetention = overallRetention;
        this.topicRetention = topicRetention;
        this.averageSatisfaction = averageSatisfaction;
        this.satisfactionResponseCount = satisfactionResponseCount;
        this.lastActivityAt = lastActivityAt;
        this.daysSinceLastActivity = daysSinceLastActivity;
        this.activityStatus = activityStatus;
        this.dropoutRisk = dropoutRisk;
        this.dropoutRate = dropoutRate;
    }

    public static class TopicTimeToMasteryDTO {
        private String topic;
        private double masteryThreshold = 0.80;
        private boolean masteryAchieved;
        private Long timeToMasteryMinutes;
        private int attempts;
        private String status;

        public TopicTimeToMasteryDTO() {
        }

        public TopicTimeToMasteryDTO(String topic, double masteryThreshold, boolean masteryAchieved, Long timeToMasteryMinutes, int attempts, String status) {
            this.topic = topic;
            this.masteryThreshold = masteryThreshold;
            this.masteryAchieved = masteryAchieved;
            this.timeToMasteryMinutes = timeToMasteryMinutes;
            this.attempts = attempts;
            this.status = status;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public double getMasteryThreshold() {
            return masteryThreshold;
        }

        public void setMasteryThreshold(double masteryThreshold) {
            this.masteryThreshold = masteryThreshold;
        }

        public boolean isMasteryAchieved() {
            return masteryAchieved;
        }

        public void setMasteryAchieved(boolean masteryAchieved) {
            this.masteryAchieved = masteryAchieved;
        }

        public Long getTimeToMasteryMinutes() {
            return timeToMasteryMinutes;
        }

        public void setTimeToMasteryMinutes(Long timeToMasteryMinutes) {
            this.timeToMasteryMinutes = timeToMasteryMinutes;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class TopicRetentionDTO {
        private String topic;
        private String masteryAchievedAt;
        private Double masteryAtAchievement;
        private String reassessmentAt;
        private Double reassessmentMastery;
        private Double retentionScore;
        private String status;

        public TopicRetentionDTO() {
        }

        public TopicRetentionDTO(String topic, String masteryAchievedAt, Double masteryAtAchievement, 
                                 String reassessmentAt, Double reassessmentMastery, Double retentionScore, String status) {
            this.topic = topic;
            this.masteryAchievedAt = masteryAchievedAt;
            this.masteryAtAchievement = masteryAtAchievement;
            this.reassessmentAt = reassessmentAt;
            this.reassessmentMastery = reassessmentMastery;
            this.retentionScore = retentionScore;
            this.status = status;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getMasteryAchievedAt() {
            return masteryAchievedAt;
        }

        public void setMasteryAchievedAt(String masteryAchievedAt) {
            this.masteryAchievedAt = masteryAchievedAt;
        }

        public Double getMasteryAtAchievement() {
            return masteryAtAchievement;
        }

        public void setMasteryAtAchievement(Double masteryAtAchievement) {
            this.masteryAtAchievement = masteryAtAchievement;
        }

        public String getReassessmentAt() {
            return reassessmentAt;
        }

        public void setReassessmentAt(String reassessmentAt) {
            this.reassessmentAt = reassessmentAt;
        }

        public Double getReassessmentMastery() {
            return reassessmentMastery;
        }

        public void setReassessmentMastery(Double reassessmentMastery) {
            this.reassessmentMastery = reassessmentMastery;
        }

        public Double getRetentionScore() {
            return retentionScore;
        }

        public void setRetentionScore(Double retentionScore) {
            this.retentionScore = retentionScore;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public double getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(double completionRate) {
        this.completionRate = completionRate;
    }

    public int getTotalRecommendations() {
        return totalRecommendations;
    }

    public void setTotalRecommendations(int totalRecommendations) {
        this.totalRecommendations = totalRecommendations;
    }

    public int getCompletedRecommendations() {
        return completedRecommendations;
    }

    public void setCompletedRecommendations(int completedRecommendations) {
        this.completedRecommendations = completedRecommendations;
    }

    public Double getAverageTimeToMasteryMinutes() {
        return averageTimeToMasteryMinutes;
    }

    public void setAverageTimeToMasteryMinutes(Double averageTimeToMasteryMinutes) {
        this.averageTimeToMasteryMinutes = averageTimeToMasteryMinutes;
    }

    public List<TopicTimeToMasteryDTO> getTopicTimeToMastery() {
        return topicTimeToMastery;
    }

    public void setTopicTimeToMastery(List<TopicTimeToMasteryDTO> topicTimeToMastery) {
        this.topicTimeToMastery = topicTimeToMastery;
    }

    public Double getOverallRetention() {
        return overallRetention;
    }

    public void setOverallRetention(Double overallRetention) {
        this.overallRetention = overallRetention;
    }

    public List<TopicRetentionDTO> getTopicRetention() {
        return topicRetention;
    }

    public void setTopicRetention(List<TopicRetentionDTO> topicRetention) {
        this.topicRetention = topicRetention;
    }

    public Double getAverageSatisfaction() {
        return averageSatisfaction;
    }

    public void setAverageSatisfaction(Double averageSatisfaction) {
        this.averageSatisfaction = averageSatisfaction;
    }

    public int getSatisfactionResponseCount() {
        return satisfactionResponseCount;
    }

    public void setSatisfactionResponseCount(int satisfactionResponseCount) {
        this.satisfactionResponseCount = satisfactionResponseCount;
    }

    public String getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(String lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Long getDaysSinceLastActivity() {
        return daysSinceLastActivity;
    }

    public void setDaysSinceLastActivity(Long daysSinceLastActivity) {
        this.daysSinceLastActivity = daysSinceLastActivity;
    }

    public String getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(String activityStatus) {
        this.activityStatus = activityStatus;
    }

    public Double getDropoutRisk() {
        return dropoutRisk;
    }

    public void setDropoutRisk(Double dropoutRisk) {
        this.dropoutRisk = dropoutRisk;
    }

    public Double getDropoutRate() {
        return dropoutRate;
    }

    public void setDropoutRate(Double dropoutRate) {
        this.dropoutRate = dropoutRate;
    }
}
