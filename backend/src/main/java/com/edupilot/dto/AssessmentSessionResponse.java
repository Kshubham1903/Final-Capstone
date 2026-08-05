package com.edupilot.dto;

import com.edupilot.model.AssessmentQuestion;

import java.util.List;

public class AssessmentSessionResponse {
    private String sessionId;
    private String branch;
    private int semester;
    private String subjectCode;
    private String subjectName;
    private int totalQuestions;
    private int totalMarks;
    private List<QuestionItemDTO> questions;

    public static class QuestionItemDTO {
        private String questionId;
        private String topic;
        private String questionText;
        private List<String> options;
        private int marks;
        private String difficulty;

        public QuestionItemDTO() {
        }

        public QuestionItemDTO(AssessmentQuestion q) {
            if (q != null) {
                this.questionId = q.getId();
                this.topic = q.getTopic();
                this.questionText = q.getQuestionText();
                this.options = q.getOptions();
                this.marks = q.getMarks();
                this.difficulty = q.getDifficulty() != null ? q.getDifficulty().name() : "MEDIUM";
            }
        }

        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }

        public int getMarks() {
            return marks;
        }

        public void setMarks(int marks) {
            this.marks = marks;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }
    }

    public AssessmentSessionResponse() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getTotalMarks() {
        return totalMarks;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public List<QuestionItemDTO> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionItemDTO> questions) {
        this.questions = questions;
    }
}
