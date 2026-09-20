package com.edupilot.dto;

import java.util.List;

public class DashboardTestQuestionDTO {
    private String questionId;
    private String subject;
    private String concept;
    private String questionText;
    private List<String> options;
    private int correctOptionIndex;
    private String conceptualExplanation;

    public DashboardTestQuestionDTO() {
    }

    public DashboardTestQuestionDTO(String questionId, String subject, String concept, String questionText, List<String> options) {
        this(questionId, subject, concept, questionText, options, 0, null);
    }

    public DashboardTestQuestionDTO(String questionId, String subject, String concept, String questionText, List<String> options, String conceptualExplanation) {
        this(questionId, subject, concept, questionText, options, 0, conceptualExplanation);
    }

    public DashboardTestQuestionDTO(String questionId, String subject, String concept, String questionText, List<String> options, int correctOptionIndex, String conceptualExplanation) {
        this.questionId = questionId;
        this.subject = subject;
        this.concept = concept;
        this.questionText = questionText;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.conceptualExplanation = conceptualExplanation;
    }

    public String getConceptualExplanation() {
        return conceptualExplanation;
    }

    public void setConceptualExplanation(String conceptualExplanation) {
        this.conceptualExplanation = conceptualExplanation;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
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

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public void setCorrectOptionIndex(int correctOptionIndex) {
        this.correctOptionIndex = correctOptionIndex;
    }
}
