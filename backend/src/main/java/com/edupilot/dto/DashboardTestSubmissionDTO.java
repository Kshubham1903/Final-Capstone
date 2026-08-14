package com.edupilot.dto;

import java.util.List;

public class DashboardTestSubmissionDTO {
    private String studentId;
    private String sessionId;
    private List<AnswerEntry> answers;

    public static class AnswerEntry {
        private String questionId;
        private int selectedOptionIndex;

        public AnswerEntry() {
        }

        public AnswerEntry(String questionId, int selectedOptionIndex) {
            this.questionId = questionId;
            this.selectedOptionIndex = selectedOptionIndex;
        }

        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
        }

        public int getSelectedOptionIndex() {
            return selectedOptionIndex;
        }

        public void setSelectedOptionIndex(int selectedOptionIndex) {
            this.selectedOptionIndex = selectedOptionIndex;
        }
    }

    public DashboardTestSubmissionDTO() {
    }

    public DashboardTestSubmissionDTO(String studentId, String sessionId, List<AnswerEntry> answers) {
        this.studentId = studentId;
        this.sessionId = sessionId;
        this.answers = answers;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<AnswerEntry> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerEntry> answers) {
        this.answers = answers;
    }
}
