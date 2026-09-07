package com.edupilot.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class StudentStateHistoryDTO {
    private LocalDateTime timestamp;
    private double knowledge;
    private double engagement;
    private Map<String, Double> topicMastery;

    public StudentStateHistoryDTO() {
    }

    public StudentStateHistoryDTO(LocalDateTime timestamp, double knowledge, double engagement, Map<String, Double> topicMastery) {
        this.timestamp = timestamp;
        this.knowledge = knowledge;
        this.engagement = engagement;
        this.topicMastery = topicMastery;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public double getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(double knowledge) {
        this.knowledge = knowledge;
    }

    public double getEngagement() {
        return engagement;
    }

    public void setEngagement(double engagement) {
        this.engagement = engagement;
    }

    public Map<String, Double> getTopicMastery() {
        return topicMastery;
    }

    public void setTopicMastery(Map<String, Double> topicMastery) {
        this.topicMastery = topicMastery;
    }
}
