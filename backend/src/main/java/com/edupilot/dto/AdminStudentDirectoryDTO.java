package com.edupilot.dto;

public class AdminStudentDirectoryDTO {

    private String userId;
    private String fullName;
    private String email;
    private String branch;
    private Integer semester;
    private String activityStatus;
    private boolean hasAuthenticBaseline;
    private Double baselineKnowledge;
    private Double currentKnowledge;
    private Double growthPp;

    public AdminStudentDirectoryDTO() {
    }

    public AdminStudentDirectoryDTO(String userId, String fullName, String email, String branch, Integer semester,
                                   String activityStatus, boolean hasAuthenticBaseline,
                                   Double baselineKnowledge, Double currentKnowledge, Double growthPp) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.branch = branch;
        this.semester = semester;
        this.activityStatus = activityStatus;
        this.hasAuthenticBaseline = hasAuthenticBaseline;
        this.baselineKnowledge = baselineKnowledge;
        this.currentKnowledge = currentKnowledge;
        this.growthPp = growthPp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(String activityStatus) {
        this.activityStatus = activityStatus;
    }

    public boolean isHasAuthenticBaseline() {
        return hasAuthenticBaseline;
    }

    public void setHasAuthenticBaseline(boolean hasAuthenticBaseline) {
        this.hasAuthenticBaseline = hasAuthenticBaseline;
    }

    public Double getBaselineKnowledge() {
        return baselineKnowledge;
    }

    public void setBaselineKnowledge(Double baselineKnowledge) {
        this.baselineKnowledge = baselineKnowledge;
    }

    public Double getCurrentKnowledge() {
        return currentKnowledge;
    }

    public void setCurrentKnowledge(Double currentKnowledge) {
        this.currentKnowledge = currentKnowledge;
    }

    public Double getGrowthPp() {
        return growthPp;
    }

    public void setGrowthPp(Double growthPp) {
        this.growthPp = growthPp;
    }
}
