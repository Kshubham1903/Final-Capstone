package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "learning_preference_questionnaires")
public class LearningPreferenceQuestionnaire {
    @Id
    private String id;
    private String userId;
    private String questionnaireId = "Form_B";
    private Map<String, Integer> responses;
    private LocalDateTime updatedAt;

    public LearningPreferenceQuestionnaire() {
    }

    public LearningPreferenceQuestionnaire(String id, String userId, String questionnaireId, Map<String, Integer> responses, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.questionnaireId = questionnaireId;
        this.responses = responses;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getQuestionnaireId() {
        return questionnaireId;
    }

    public void setQuestionnaireId(String questionnaireId) {
        this.questionnaireId = questionnaireId;
    }

    public Map<String, Integer> getResponses() {
        return responses;
    }

    public void setResponses(Map<String, Integer> responses) {
        this.responses = responses;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
