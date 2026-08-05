package com.edupilot.dto;

import com.edupilot.model.AiConversation;
import com.edupilot.model.LearningMode;

import java.time.LocalDateTime;
import java.util.List;

public class ConversationResponse {
    private String id;
    private String conversationId;
    private String studentId;
    private String title;
    private String learningPlanTaskId;
    private String referencedConcept;
    private LearningMode learningMode = LearningMode.LEARN;
    private List<AiConversation.ChatMessage> messages;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConversationResponse() {
    }

    public ConversationResponse(AiConversation conv) {
        if (conv != null) {
            this.id = conv.getId();
            this.conversationId = conv.getConversationId();
            this.studentId = conv.getStudentId();
            this.title = conv.getTitle();
            this.learningPlanTaskId = conv.getLearningPlanTaskId();
            this.referencedConcept = conv.getReferencedConcept();
            this.learningMode = conv.getLearningMode() != null ? conv.getLearningMode() : LearningMode.LEARN;
            this.messages = conv.getMessages();
            this.createdAt = conv.getCreatedAt();
            this.updatedAt = conv.getUpdatedAt();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public List<AiConversation.ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<AiConversation.ChatMessage> messages) {
        this.messages = messages;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
