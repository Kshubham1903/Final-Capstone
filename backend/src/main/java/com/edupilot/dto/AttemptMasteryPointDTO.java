package com.edupilot.dto;

import java.time.LocalDateTime;

public class AttemptMasteryPointDTO {
    private String attemptId;
    private LocalDateTime dateTime;
    private double scorePercentage;
    private int questionsInAttempt;
    private int correctInAttempt;
    private String subject;

    public AttemptMasteryPointDTO() {
    }

    public AttemptMasteryPointDTO(String attemptId, LocalDateTime dateTime, double scorePercentage, 
                                 int questionsInAttempt, int correctInAttempt, String subject) {
        this.attemptId = attemptId;
        this.dateTime = dateTime;
        this.scorePercentage = scorePercentage;
        this.questionsInAttempt = questionsInAttempt;
        this.correctInAttempt = correctInAttempt;
        this.subject = subject;
    }

    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public double getScorePercentage() {
        return scorePercentage;
    }

    public void setScorePercentage(double scorePercentage) {
        this.scorePercentage = scorePercentage;
    }

    public int getQuestionsInAttempt() {
        return questionsInAttempt;
    }

    public void setQuestionsInAttempt(int questionsInAttempt) {
        this.questionsInAttempt = questionsInAttempt;
    }

    public int getCorrectInAttempt() {
        return correctInAttempt;
    }

    public void setCorrectInAttempt(int correctInAttempt) {
        this.correctInAttempt = correctInAttempt;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
