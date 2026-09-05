package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "dashboard_test_sessions")
public class DashboardTestSession {
    @Id
    private String id;
    private String studentId;
    private List<String> subjects;
    private List<String> questionIds;
    private LocalDateTime createdAt = LocalDateTime.now();
    private boolean completed = false;

    public DashboardTestSession() {
    }

    public DashboardTestSession(String id, String studentId, List<String> subjects, List<String> questionIds, LocalDateTime createdAt, boolean completed) {
        this.id = id;
        this.studentId = studentId;
        this.subjects = subjects;
        this.questionIds = questionIds;
        this.createdAt = createdAt;
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

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
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
}
