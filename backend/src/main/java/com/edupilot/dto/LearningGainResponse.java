package com.edupilot.dto;

import java.util.List;

public class LearningGainResponse {
    private String studentId;
    private double overallLearningGain;
    private List<TopicGainDTO> topics;

    public LearningGainResponse() {
    }

    public LearningGainResponse(String studentId, double overallLearningGain, List<TopicGainDTO> topics) {
        this.studentId = studentId;
        this.overallLearningGain = overallLearningGain;
        this.topics = topics;
    }

    public static class TopicGainDTO {
        private String topic;
        private double preScore;
        private double postScore;
        private double learningGain;

        public TopicGainDTO() {
        }

        public TopicGainDTO(String topic, double preScore, double postScore, double learningGain) {
            this.topic = topic;
            this.preScore = preScore;
            this.postScore = postScore;
            this.learningGain = learningGain;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public double getPreScore() {
            return preScore;
        }

        public void setPreScore(double preScore) {
            this.preScore = preScore;
        }

        public double getPostScore() {
            return postScore;
        }

        public void setPostScore(double postScore) {
            this.postScore = postScore;
        }

        public double getLearningGain() {
            return learningGain;
        }

        public void setLearningGain(double learningGain) {
            this.learningGain = learningGain;
        }
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public double getOverallLearningGain() {
        return overallLearningGain;
    }

    public void setOverallLearningGain(double overallLearningGain) {
        this.overallLearningGain = overallLearningGain;
    }

    public List<TopicGainDTO> getTopics() {
        return topics;
    }

    public void setTopics(List<TopicGainDTO> topics) {
        this.topics = topics;
    }
}
