package com.edupilot.dto;

import com.edupilot.model.StudentSatisfaction;

public class SatisfactionRequest {
    private int rating;
    private StudentSatisfaction.FeedbackType feedbackType = StudentSatisfaction.FeedbackType.LEARNING_ACTIVITY;
    private String comment;

    public SatisfactionRequest() {
    }

    public SatisfactionRequest(int rating, StudentSatisfaction.FeedbackType feedbackType, String comment) {
        this.rating = rating;
        this.feedbackType = feedbackType;
        this.comment = comment;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public StudentSatisfaction.FeedbackType getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(StudentSatisfaction.FeedbackType feedbackType) {
        this.feedbackType = feedbackType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
