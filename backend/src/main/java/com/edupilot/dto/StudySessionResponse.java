package com.edupilot.dto;

import com.edupilot.model.StudySession;

import java.time.LocalDateTime;

public class StudySessionResponse {
    private String id;
    private String userId;
    private String taskId;
    private String subjectCode;
    private String conceptName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int actualDurationMinutes;
    private int pausedDurationMinutes;
    private String status;
    private String completionNotes;

    public StudySessionResponse() {
    }

    public StudySessionResponse(StudySession s) {
        if (s != null) {
            this.id = s.getId();
            this.userId = s.getUserId();
            this.taskId = s.getTaskId();
            this.subjectCode = s.getSubjectCode();
            this.conceptName = s.getConceptName();
            this.startTime = s.getStartTime();
            this.endTime = s.getEndTime();
            this.actualDurationMinutes = s.getActualDurationMinutes();
            this.pausedDurationMinutes = s.getPausedDurationMinutes();
            this.status = s.getStatus() != null ? s.getStatus().name() : "ACTIVE";
            this.completionNotes = s.getCompletionNotes();
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

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getConceptName() {
        return conceptName;
    }

    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getActualDurationMinutes() {
        return actualDurationMinutes;
    }

    public void setActualDurationMinutes(int actualDurationMinutes) {
        this.actualDurationMinutes = actualDurationMinutes;
    }

    public int getPausedDurationMinutes() {
        return pausedDurationMinutes;
    }

    public void setPausedDurationMinutes(int pausedDurationMinutes) {
        this.pausedDurationMinutes = pausedDurationMinutes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCompletionNotes() {
        return completionNotes;
    }

    public void setCompletionNotes(String completionNotes) {
        this.completionNotes = completionNotes;
    }
}
