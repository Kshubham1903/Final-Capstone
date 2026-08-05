package com.edupilot.dto;

public class StudySessionStartRequest {
    private String userId;
    private String taskId;
    private String subjectCode;
    private String conceptName;

    public StudySessionStartRequest() {
    }

    public StudySessionStartRequest(String userId, String taskId, String subjectCode, String conceptName) {
        this.userId = userId;
        this.taskId = taskId;
        this.subjectCode = subjectCode;
        this.conceptName = conceptName;
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
}
