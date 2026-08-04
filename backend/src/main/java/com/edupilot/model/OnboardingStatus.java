package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "onboarding_statuses")
public class OnboardingStatus {
    @Id
    private String id;
    private String userId;

    private boolean personalCompleted = false;
    private boolean academicCompleted = false;
    private boolean questionnaireCompleted = false;
    private boolean isCompleted = false;

    private int completionPercentage = 0;
    private int currentStep = 1; // 1: Personal, 2: Academic, 3: Lifestyle S1, 4: S2, 5: S3, 6: S4, 7: S5
    
    private LocalDateTime lastSavedAt;
    private LocalDateTime completedAt;

    public OnboardingStatus() {
    }

    public OnboardingStatus(String id, String userId, boolean personalCompleted, boolean academicCompleted, boolean questionnaireCompleted, boolean isCompleted, int completionPercentage, int currentStep, LocalDateTime lastSavedAt, LocalDateTime completedAt) {
        this.id = id;
        this.userId = userId;
        this.personalCompleted = personalCompleted;
        this.academicCompleted = academicCompleted;
        this.questionnaireCompleted = questionnaireCompleted;
        this.isCompleted = isCompleted;
        this.completionPercentage = completionPercentage;
        this.currentStep = currentStep;
        this.lastSavedAt = lastSavedAt;
        this.completedAt = completedAt;
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

    public boolean isPersonalCompleted() {
        return personalCompleted;
    }

    public void setPersonalCompleted(boolean personalCompleted) {
        this.personalCompleted = personalCompleted;
    }

    public boolean isAcademicCompleted() {
        return academicCompleted;
    }

    public void setAcademicCompleted(boolean academicCompleted) {
        this.academicCompleted = academicCompleted;
    }

    public boolean isQuestionnaireCompleted() {
        return questionnaireCompleted;
    }

    public void setQuestionnaireCompleted(boolean questionnaireCompleted) {
        this.questionnaireCompleted = questionnaireCompleted;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public int getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(int completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public LocalDateTime getLastSavedAt() {
        return lastSavedAt;
    }

    public void setLastSavedAt(LocalDateTime lastSavedAt) {
        this.lastSavedAt = lastSavedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
