package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "learning_plans")
public class LearningPlan {
    @Id
    private String id;
    private String userId;
    private String studentProfileId;
    private LocalDate planDate = LocalDate.now();
    private String dayLabel = "TODAY";
    private List<LearningTask> tasks = new ArrayList<>();
    private int totalTasks = 0;
    private int completedTasks = 0;
    private int totalEstimatedMinutes = 0;
    private double completionPercentage = 0.0;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static class LearningTask {
        private String taskId;
        private String subjectCode;
        private String subjectName;
        private String topic;
        private String conceptName;
        private Recommendation.Priority priority = Recommendation.Priority.MEDIUM;
        private int estimatedStudyTimeMinutes = 20;
        private int recommendedOrder = 1;
        private String reason; // Explainability text
        private String recommendedAction;
        private TaskStatus status = TaskStatus.PENDING;
        private String generatedFromRecommendationId;

        public enum TaskStatus {
            PENDING,
            IN_PROGRESS,
            COMPLETED,
            SKIPPED
        }

        public LearningTask() {
        }

        public LearningTask(String taskId, String subjectCode, String subjectName, String topic, String conceptName, 
                            Recommendation.Priority priority, int estimatedStudyTimeMinutes, int recommendedOrder, 
                            String reason, String recommendedAction, TaskStatus status, String generatedFromRecommendationId) {
            this.taskId = taskId;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.topic = topic;
            this.conceptName = conceptName;
            this.priority = priority;
            this.estimatedStudyTimeMinutes = estimatedStudyTimeMinutes;
            this.recommendedOrder = recommendedOrder;
            this.reason = reason;
            this.recommendedAction = recommendedAction;
            this.status = status;
            this.generatedFromRecommendationId = generatedFromRecommendationId;
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

        public String getSubjectName() {
            return subjectName;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getConceptName() {
            return conceptName;
        }

        public void setConceptName(String conceptName) {
            this.conceptName = conceptName;
        }

        public Recommendation.Priority getPriority() {
            return priority;
        }

        public void setPriority(Recommendation.Priority priority) {
            this.priority = priority;
        }

        public int getEstimatedStudyTimeMinutes() {
            return estimatedStudyTimeMinutes;
        }

        public void setEstimatedStudyTimeMinutes(int estimatedStudyTimeMinutes) {
            this.estimatedStudyTimeMinutes = estimatedStudyTimeMinutes;
        }

        public int getRecommendedOrder() {
            return recommendedOrder;
        }

        public void setRecommendedOrder(int recommendedOrder) {
            this.recommendedOrder = recommendedOrder;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getRecommendedAction() {
            return recommendedAction;
        }

        public void setRecommendedAction(String recommendedAction) {
            this.recommendedAction = recommendedAction;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public void setStatus(TaskStatus status) {
            this.status = status;
        }

        public String getGeneratedFromRecommendationId() {
            return generatedFromRecommendationId;
        }

        public void setGeneratedFromRecommendationId(String generatedFromRecommendationId) {
            this.generatedFromRecommendationId = generatedFromRecommendationId;
        }
    }

    public LearningPlan() {
    }

    public LearningPlan(String id, String userId, String studentProfileId, LocalDate planDate, String dayLabel, 
                        List<LearningTask> tasks, int totalTasks, int completedTasks, int totalEstimatedMinutes, 
                        double completionPercentage, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.studentProfileId = studentProfileId;
        this.planDate = planDate;
        this.dayLabel = dayLabel;
        this.tasks = tasks;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.totalEstimatedMinutes = totalEstimatedMinutes;
        this.completionPercentage = completionPercentage;
        this.createdAt = createdAt;
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

    public List<LearningTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<LearningTask> tasks) {
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
