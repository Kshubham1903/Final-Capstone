package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "knowledge_profiles")
public class KnowledgeProfile {
    @Id
    private String id;
    private String userId;
    private String studentProfileId;
    private double learningHealthScore = 0.0;
    private int totalConceptsTracked = 0;
    private int masteredCount = 0;
    private int proficientCount = 0;
    private int intermediateCount = 0;
    private int beginnerCount = 0;
    private List<String> strongConcepts = new ArrayList<>();
    private List<String> weakConcepts = new ArrayList<>();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public KnowledgeProfile() {
    }

    public KnowledgeProfile(String id, String userId, String studentProfileId, double learningHealthScore, 
                            int totalConceptsTracked, int masteredCount, int proficientCount, int intermediateCount, 
                            int beginnerCount, List<String> strongConcepts, List<String> weakConcepts, 
                            LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.studentProfileId = studentProfileId;
        this.learningHealthScore = learningHealthScore;
        this.totalConceptsTracked = totalConceptsTracked;
        this.masteredCount = masteredCount;
        this.proficientCount = proficientCount;
        this.intermediateCount = intermediateCount;
        this.beginnerCount = beginnerCount;
        this.strongConcepts = strongConcepts;
        this.weakConcepts = weakConcepts;
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

    public String getStudentProfileId() {
        return studentProfileId;
    }

    public void setStudentProfileId(String studentProfileId) {
        this.studentProfileId = studentProfileId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
