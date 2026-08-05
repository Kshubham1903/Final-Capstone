package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "study_sessions")
public class StudySession {
    @Id
    private String id;
    private String userId;
    private String studentProfileId;
    private String taskId;
    private String subjectCode;
    private String conceptName;
    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime endTime;
    private int actualDurationMinutes = 0;
    private int pausedDurationMinutes = 0;
    private SessionStatus status = SessionStatus.ACTIVE;
    private String completionNotes;

    public enum SessionStatus {
        ACTIVE,
        PAUSED,
        COMPLETED
    }

    public StudySession() {
    }

    public StudySession(String id, String userId, String studentProfileId, String taskId, String subjectCode, 
                        String conceptName, LocalDateTime startTime, LocalDateTime endTime, int actualDurationMinutes, 
                        int pausedDurationMinutes, SessionStatus status, String completionNotes) {
        this.id = id;
        this.userId = userId;
        this.studentProfileId = studentProfileId;
        this.taskId = taskId;
        this.subjectCode = subjectCode;
        this.conceptName = conceptName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.actualDurationMinutes = actualDurationMinutes;
        this.pausedDurationMinutes = pausedDurationMinutes;
        this.status = status;
        this.completionNotes = completionNotes;
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

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getCompletionNotes() {
        return completionNotes;
    }

    public void setCompletionNotes(String completionNotes) {
        this.completionNotes = completionNotes;
    }
}
