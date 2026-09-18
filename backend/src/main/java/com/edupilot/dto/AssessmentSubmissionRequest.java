package com.edupilot.dto;

import java.util.List;

public class AssessmentSubmissionRequest {
    private String sessionId;
    private String userId;
    private int timeTakenSeconds;
    private List<AnswerItem> answers;

    public static class AnswerItem {
        private String questionId;
        private int selectedOption; // -1 if skipped
        private Double responseTimeSeconds;

        public AnswerItem() {
        }

        public AnswerItem(String questionId, int selectedOption) {
            this(questionId, selectedOption, null);
        }

        public AnswerItem(String questionId, int selectedOption, Double responseTimeSeconds) {
            this.questionId = questionId;
            this.selectedOption = selectedOption;
            this.responseTimeSeconds = responseTimeSeconds;
        }

        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
        }

        public int getSelectedOption() {
            return selectedOption;
        }

        public void setSelectedOption(int selectedOption) {
            this.selectedOption = selectedOption;
        }

        public Double getResponseTimeSeconds() {
            return responseTimeSeconds;
        }

        public void setResponseTimeSeconds(Double responseTimeSeconds) {
            this.responseTimeSeconds = responseTimeSeconds;
        }
    }

    public AssessmentSubmissionRequest() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getTimeTakenSeconds() {
        return timeTakenSeconds;
    }

    public void setTimeTakenSeconds(int timeTakenSeconds) {
        this.timeTakenSeconds = timeTakenSeconds;
    }

    public List<AnswerItem> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerItem> answers) {
        this.answers = answers;
    }
}
