package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "well_being_learning_contexts")
public class WellBeingLearningContext {
    @Id
    private String id;
    private String userId;
    private String questionnaireId = "Form_C";
    private boolean consent;

    // C2 fields (can be null if consent = false)
    private Integer motivation;
    private Integer focus;
    private Integer mentalFatigue;
    private Integer academicStress;
    private Integer currentLearningConfidence;
    private Integer workloadComfort;
    private Integer learningSatisfaction;

    // C3 fields (can be null if consent = false)
    private String sleepDuration;
    private String preferredStudyTime;
    private String studyEnvironment;
    private String currentAcademicWorkload;
    private String pendingAssignments;
    private String majorAcademicDifficulty;
    private Boolean needsAcademicSupport;

    private LocalDateTime updatedAt;

    public WellBeingLearningContext() {
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

    public boolean isConsent() {
        return consent;
    }

    public void setConsent(boolean consent) {
        this.consent = consent;
    }

    public Integer getMotivation() {
        return motivation;
    }

    public void setMotivation(Integer motivation) {
        this.motivation = motivation;
    }

    public Integer getFocus() {
        return focus;
    }

    public void setFocus(Integer focus) {
        this.focus = focus;
    }

    public Integer getMentalFatigue() {
        return mentalFatigue;
    }

    public void setMentalFatigue(Integer mentalFatigue) {
        this.mentalFatigue = mentalFatigue;
    }

    public Integer getAcademicStress() {
        return academicStress;
    }

    public void setAcademicStress(Integer academicStress) {
        this.academicStress = academicStress;
    }

    public Integer getCurrentLearningConfidence() {
        return currentLearningConfidence;
    }

    public void setCurrentLearningConfidence(Integer currentLearningConfidence) {
        this.currentLearningConfidence = currentLearningConfidence;
    }

    public Integer getWorkloadComfort() {
        return workloadComfort;
    }

    public void setWorkloadComfort(Integer workloadComfort) {
        this.workloadComfort = workloadComfort;
    }

    public Integer getLearningSatisfaction() {
        return learningSatisfaction;
    }

    public void setLearningSatisfaction(Integer learningSatisfaction) {
        this.learningSatisfaction = learningSatisfaction;
    }

    public String getSleepDuration() {
        return sleepDuration;
    }

    public void setSleepDuration(String sleepDuration) {
        this.sleepDuration = sleepDuration;
    }

    public String getPreferredStudyTime() {
        return preferredStudyTime;
    }

    public void setPreferredStudyTime(String preferredStudyTime) {
        this.preferredStudyTime = preferredStudyTime;
    }

    public String getStudyEnvironment() {
        return studyEnvironment;
    }

    public void setStudyEnvironment(String studyEnvironment) {
        this.studyEnvironment = studyEnvironment;
    }

    public String getCurrentAcademicWorkload() {
        return currentAcademicWorkload;
    }

    public void setCurrentAcademicWorkload(String currentAcademicWorkload) {
        this.currentAcademicWorkload = currentAcademicWorkload;
    }

    public String getPendingAssignments() {
        return pendingAssignments;
    }

    public void setPendingAssignments(String pendingAssignments) {
        this.pendingAssignments = pendingAssignments;
    }

    public String getMajorAcademicDifficulty() {
        return majorAcademicDifficulty;
    }

    public void setMajorAcademicDifficulty(String majorAcademicDifficulty) {
        this.majorAcademicDifficulty = majorAcademicDifficulty;
    }

    public Boolean getNeedsAcademicSupport() {
        return needsAcademicSupport;
    }

    public void setNeedsAcademicSupport(Boolean needsAcademicSupport) {
        this.needsAcademicSupport = needsAcademicSupport;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
