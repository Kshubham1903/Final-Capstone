package com.edupilot.dto;

import com.edupilot.model.LearningMode;

public class ChatRequest {
    private String studentId;
    private String conversationId;
    private String message;
    private String learningPlanTaskId;
    private String referencedConcept;
    private LearningMode learningMode;

    public ChatRequest() {
    }

    public ChatRequest(String studentId, String conversationId, String message, String learningPlanTaskId, String referencedConcept, LearningMode learningMode) {
        this.studentId = studentId;
        this.conversationId = conversationId;
        this.message = message;
        this.learningPlanTaskId = learningPlanTaskId;
        this.referencedConcept = referencedConcept;
        this.learningMode = learningMode;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLearningPlanTaskId() {
        return learningPlanTaskId;
    }

    public void setLearningPlanTaskId(String learningPlanTaskId) {
        this.learningPlanTaskId = learningPlanTaskId;
    }

    public String getReferencedConcept() {
        return referencedConcept;
    }

    public void setReferencedConcept(String referencedConcept) {
        this.referencedConcept = referencedConcept;
    }

    public LearningMode getLearningMode() {
        return learningMode;
    }

    public void setLearningMode(LearningMode learningMode) {
        this.learningMode = learningMode;
    }
}
