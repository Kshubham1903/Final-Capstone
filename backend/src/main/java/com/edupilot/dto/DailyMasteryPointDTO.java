package com.edupilot.dto;

import java.time.LocalDate;

public class DailyMasteryPointDTO {
    private LocalDate date;
    private double masteryPercentage;
    private int questionsAnsweredThatDay;
    private int correctThatDay;

    public DailyMasteryPointDTO() {
    }

    public DailyMasteryPointDTO(LocalDate date, double masteryPercentage, int questionsAnsweredThatDay, int correctThatDay) {
        this.date = date;
        this.masteryPercentage = masteryPercentage;
        this.questionsAnsweredThatDay = questionsAnsweredThatDay;
        this.correctThatDay = correctThatDay;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getMasteryPercentage() {
        return masteryPercentage;
    }

    public void setMasteryPercentage(double masteryPercentage) {
        this.masteryPercentage = masteryPercentage;
    }

    public int getQuestionsAnsweredThatDay() {
        return questionsAnsweredThatDay;
    }

    public void setQuestionsAnsweredThatDay(int questionsAnsweredThatDay) {
        this.questionsAnsweredThatDay = questionsAnsweredThatDay;
    }

    public int getCorrectThatDay() {
        return correctThatDay;
    }

    public void setCorrectThatDay(int correctThatDay) {
        this.correctThatDay = correctThatDay;
    }
}
