package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "ai_conversations")
public class AiConversation {
    @Id
    private String id;
    private String conversationId;
    private String studentId;
    private String title = "New AI Tutor Discussion";
    private String learningPlanTaskId;
    private String referencedConcept;
    private LearningMode learningMode = LearningMode.LEARN;
    private List<ChatMessage> messages = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static class ChatMessage {
        private String messageId;
        private String role; // "user", "assistant", "system"
        private String content;
        private LocalDateTime timestamp = LocalDateTime.now();

        public ChatMessage() {
        }

        public ChatMessage(String messageId, String role, String content, LocalDateTime timestamp) {
            this.messageId = messageId;
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }

    public AiConversation() {
    }

    public AiConversation(String id, String conversationId, String studentId, String title, String learningPlanTaskId, 
                          String referencedConcept, List<ChatMessage> messages, Map<String, Object> metadata, 
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.studentId = studentId;
        this.title = title;
        this.learningPlanTaskId = learningPlanTaskId;
        this.referencedConcept = referencedConcept;
        this.messages = messages;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LearningMode getLearningMode() {
        return learningMode;
    }

    public void setLearningMode(LearningMode learningMode) {
        this.learningMode = learningMode;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
