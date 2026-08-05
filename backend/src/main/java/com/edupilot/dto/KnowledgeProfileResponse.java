package com.edupilot.dto;

import com.edupilot.model.KnowledgeProfile;

import java.time.LocalDateTime;
import java.util.List;

public class KnowledgeProfileResponse {
    private String id;
    private String userId;
    private double learningHealthScore;
    private int totalConceptsTracked;
    private int masteredCount;
    private int proficientCount;
    private int intermediateCount;
    private int beginnerCount;
    private List<String> strongConcepts;
    private List<String> weakConcepts;
    private List<ConceptMasteryResponse> conceptEntries;
    private LocalDateTime updatedAt;

    public KnowledgeProfileResponse() {
    }

    public KnowledgeProfileResponse(KnowledgeProfile kp, List<ConceptMasteryResponse> conceptEntries) {
        if (kp != null) {
            this.id = kp.getId();
            this.userId = kp.getUserId();
            this.learningHealthScore = kp.getLearningHealthScore();
            this.totalConceptsTracked = kp.getTotalConceptsTracked();
            this.masteredCount = kp.getMasteredCount();
            this.proficientCount = kp.getProficientCount();
            this.intermediateCount = kp.getIntermediateCount();
            this.beginnerCount = kp.getBeginnerCount();
            this.strongConcepts = kp.getStrongConcepts();
            this.weakConcepts = kp.getWeakConcepts();
            this.conceptEntries = conceptEntries;
            this.updatedAt = kp.getUpdatedAt();
        }
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

    public double getLearningHealthScore() {
        return learningHealthScore;
    }

    public void setLearningHealthScore(double learningHealthScore) {
        this.learningHealthScore = learningHealthScore;
    }

    public int getTotalConceptsTracked() {
        return totalConceptsTracked;
    }

    public void setTotalConceptsTracked(int totalConceptsTracked) {
        this.totalConceptsTracked = totalConceptsTracked;
    }

    public int getMasteredCount() {
        return masteredCount;
    }

    public void setMasteredCount(int masteredCount) {
        this.masteredCount = masteredCount;
    }

    public int getProficientCount() {
        return proficientCount;
    }

    public void setProficientCount(int proficientCount) {
        this.proficientCount = proficientCount;
    }

    public int getIntermediateCount() {
        return intermediateCount;
    }

    public void setIntermediateCount(int intermediateCount) {
        this.intermediateCount = intermediateCount;
    }

    public int getBeginnerCount() {
        return beginnerCount;
    }

    public void setBeginnerCount(int beginnerCount) {
        this.beginnerCount = beginnerCount;
    }

    public List<String> getStrongConcepts() {
        return strongConcepts;
    }

    public void setStrongConcepts(List<String> strongConcepts) {
        this.strongConcepts = strongConcepts;
    }

    public List<String> getWeakConcepts() {
        return weakConcepts;
    }

    public void setWeakConcepts(List<String> weakConcepts) {
        this.weakConcepts = weakConcepts;
    }

    public List<ConceptMasteryResponse> getConceptEntries() {
        return conceptEntries;
    }

    public void setConceptEntries(List<ConceptMasteryResponse> conceptEntries) {
        this.conceptEntries = conceptEntries;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
