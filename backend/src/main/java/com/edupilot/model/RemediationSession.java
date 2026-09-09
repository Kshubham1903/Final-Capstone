package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "remediation_sessions")
public class RemediationSession {
    @Id
    private String id;
    private String studentId;
    private String subject;
    private String concept;
    private List<String> questionIds = new ArrayList<>();
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean completed = false;
    private ModuleType moduleType = ModuleType.REMEDIATION;

    public RemediationSession() {
    }

    public RemediationSession(String id, String studentId, String subject, String concept, List<String> questionIds, LocalDateTime createdAt, boolean completed) {
        this.id = id;
        this.studentId = studentId;
        this.subject = subject;
        this.concept = concept;
        this.questionIds = questionIds != null ? questionIds : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.completed = completed;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
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

    public List<String> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(List<String> questionIds) {
        this.questionIds = questionIds;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public ModuleType getModuleType() {
        return moduleType != null ? moduleType : ModuleType.REMEDIATION;
    }

    public void setModuleType(ModuleType moduleType) {
        this.moduleType = moduleType;
    }
}
