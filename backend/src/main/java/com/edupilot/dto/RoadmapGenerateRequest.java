package com.edupilot.dto;

public class RoadmapGenerateRequest {
    private String userId;
    private String subjectCode;
    private String subjectName;

    public RoadmapGenerateRequest() {
    }

    public RoadmapGenerateRequest(String userId, String subjectCode, String subjectName) {
        this.userId = userId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
}
