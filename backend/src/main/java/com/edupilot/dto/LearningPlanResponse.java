package com.edupilot.dto;

import com.edupilot.model.LearningPlan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class LearningPlanResponse {
    private String id;
    private String userId;
    private LocalDate planDate;
    private String dayLabel;
    private List<LearningPlan.LearningTask> tasks;
    private int totalTasks;
    private int completedTasks;
    private int totalEstimatedMinutes;
    private double completionPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public LearningPlanResponse() {
    }

    public LearningPlanResponse(LearningPlan plan) {
        if (plan != null) {
            this.id = plan.getId();
            this.userId = plan.getUserId();
            this.planDate = plan.getPlanDate();
            this.dayLabel = plan.getDayLabel();
            this.tasks = plan.getTasks();
            this.totalTasks = plan.getTotalTasks();
            this.completedTasks = plan.getCompletedTasks();
            this.totalEstimatedMinutes = plan.getTotalEstimatedMinutes();
            this.completionPercentage = plan.getCompletionPercentage();
            this.createdAt = plan.getCreatedAt();
            this.updatedAt = plan.getUpdatedAt();
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

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public void setDayLabel(String dayLabel) {
        this.dayLabel = dayLabel;
    }

    public List<LearningPlan.LearningTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<LearningPlan.LearningTask> tasks) {
        this.tasks = tasks;
    }

    public int getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(int totalTasks) {
        this.totalTasks = totalTasks;
    }

    public int getCompletedTasks() {
        return completedTasks;
    }

    public void setCompletedTasks(int completedTasks) {
        this.completedTasks = completedTasks;
    }

    public int getTotalEstimatedMinutes() {
        return totalEstimatedMinutes;
    }

    public void setTotalEstimatedMinutes(int totalEstimatedMinutes) {
        this.totalEstimatedMinutes = totalEstimatedMinutes;
    }

    public double getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
