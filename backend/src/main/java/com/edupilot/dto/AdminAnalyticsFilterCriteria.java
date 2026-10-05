package com.edupilot.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminAnalyticsFilterCriteria {

    private LocalDate startDate;
    private LocalDate endDate;
    private String branch;
    private Integer semester;
    private String subjectCode;
    private String activityStatus; // "ACTIVE", "AT_RISK", "INACTIVE", "NO_ACTIVITY"
    private Boolean hasAuthenticBaseline;

    public AdminAnalyticsFilterCriteria() {
    }

    public AdminAnalyticsFilterCriteria(
            LocalDate startDate,
            LocalDate endDate,
            String branch,
            Integer semester,
            String subjectCode,
            String activityStatus,
            Boolean hasAuthenticBaseline
    ) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.branch = branch;
        this.semester = semester;
        this.subjectCode = subjectCode;
        this.activityStatus = activityStatus;
        this.hasAuthenticBaseline = hasAuthenticBaseline;
        validate();
    }

    public void validate() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate (" + startDate + ") cannot be after endDate (" + endDate + ")");
        }
        if (activityStatus != null && !activityStatus.trim().isEmpty()) {
            String norm = activityStatus.trim().toUpperCase();
            if (!norm.equals("ACTIVE") && !norm.equals("AT_RISK") && !norm.equals("INACTIVE") && !norm.equals("NO_ACTIVITY")) {
                throw new IllegalArgumentException("Invalid activityStatus: " + activityStatus + ". Allowed values: ACTIVE, AT_RISK, INACTIVE, NO_ACTIVITY");
            }
        }
    }

    public boolean hasDateFilter() {
        return startDate != null || endDate != null;
    }

    public boolean isDateInRange(LocalDate date) {
        if (date == null) {
            return false;
        }
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && date.isAfter(endDate)) {
            return false;
        }
        return true;
    }

    public boolean isDateTimeInRange(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return isDateInRange(dateTime.toLocalDate());
    }

    public boolean hasPopulationFilter() {
        return (branch != null && !branch.trim().isEmpty()) ||
                semester != null ||
                (activityStatus != null && !activityStatus.trim().isEmpty()) ||
                hasAuthenticBaseline != null;
    }

    public boolean hasSubjectFilter() {
        return subjectCode != null && !subjectCode.trim().isEmpty();
    }

    public boolean isEmpty() {
        return !hasDateFilter() && !hasPopulationFilter() && !hasSubjectFilter();
    }

    // Getters and Setters

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        validate();
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        validate();
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

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getActivityStatus() {
        return activityStatus;
    }

    public void setActivityStatus(String activityStatus) {
        this.activityStatus = activityStatus;
        validate();
    }

    public Boolean getHasAuthenticBaseline() {
        return hasAuthenticBaseline;
    }

    public void setHasAuthenticBaseline(Boolean hasAuthenticBaseline) {
        this.hasAuthenticBaseline = hasAuthenticBaseline;
    }
}
