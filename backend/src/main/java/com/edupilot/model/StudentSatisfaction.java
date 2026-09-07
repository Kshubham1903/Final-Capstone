package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "student_satisfaction_ratings")
public class StudentSatisfaction {
    @Id
    private String id;
    private String studentId;
    private int rating;
    private FeedbackType feedbackType = FeedbackType.LEARNING_ACTIVITY;
    private String comment;
    private LocalDateTime timestamp = LocalDateTime.now();

    public enum FeedbackType {
        AI_TUTOR,
        RECOMMENDATION,
        LEARNING_ACTIVITY
    }

    public StudentSatisfaction() {
    }

    public StudentSatisfaction(String studentId, int rating, FeedbackType feedbackType, String comment) {
        this.studentId = studentId;
        this.rating = rating;
        this.feedbackType = feedbackType != null ? feedbackType : FeedbackType.LEARNING_ACTIVITY;
        this.comment = comment;
        this.timestamp = LocalDateTime.now();
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

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public FeedbackType getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(FeedbackType feedbackType) {
        this.feedbackType = feedbackType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
