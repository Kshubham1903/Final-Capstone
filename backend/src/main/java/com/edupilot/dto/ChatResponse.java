package com.edupilot.dto;

import java.time.LocalDateTime;

public class ChatResponse {
    private String conversationId;
    private String messageId;
    private String role;
    private String content;
    private LocalDateTime timestamp;

    public ChatResponse() {
    }

    public ChatResponse(String conversationId, String messageId, String role, String content, LocalDateTime timestamp) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
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
