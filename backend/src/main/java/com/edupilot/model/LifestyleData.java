package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "lifestyle_data")
public class LifestyleData {
    @Id
    private String id;
    private String studentProfileId;
    private LocalDate date;

    private double sleepHours;
    private double screenTimeHours;
    private int stressLevel; // 1 to 10
    private double exerciseMinutes;
    private double studyMinutes;
    private double attendanceRate; // percentage
    private double productivityRating; // 1 to 10

    public LifestyleData() {
    }

    public LifestyleData(String id, String studentProfileId, LocalDate date, double sleepHours, double screenTimeHours, int stressLevel, double exerciseMinutes, double studyMinutes, double attendanceRate, double productivityRating) {
        this.id = id;
        this.studentProfileId = studentProfileId;
        this.date = date;
        this.sleepHours = sleepHours;
        this.screenTimeHours = screenTimeHours;
        this.stressLevel = stressLevel;
        this.exerciseMinutes = exerciseMinutes;
        this.studyMinutes = studyMinutes;
        this.attendanceRate = attendanceRate;
        this.productivityRating = productivityRating;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentProfileId() {
        return studentProfileId;
    }

    public void setStudentProfileId(String studentProfileId) {
        this.studentProfileId = studentProfileId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getSleepHours() {
        return sleepHours;
    }

    public void setSleepHours(double sleepHours) {
        this.sleepHours = sleepHours;
    }

    public double getScreenTimeHours() {
        return screenTimeHours;
    }

    public void setScreenTimeHours(double screenTimeHours) {
        this.screenTimeHours = screenTimeHours;
    }

    public int getStressLevel() {
        return stressLevel;
    }

    public void setStressLevel(int stressLevel) {
        this.stressLevel = stressLevel;
    }

    public double getExerciseMinutes() {
        return exerciseMinutes;
    }

    public void setExerciseMinutes(double exerciseMinutes) {
        this.exerciseMinutes = exerciseMinutes;
    }

    public double getStudyMinutes() {
        return studyMinutes;
    }

    public void setStudyMinutes(double studyMinutes) {
        this.studyMinutes = studyMinutes;
    }

    public double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public double getProductivityRating() {
        return productivityRating;
    }

    public void setProductivityRating(double productivityRating) {
        this.productivityRating = productivityRating;
    }

    public static LifestyleDataBuilder builder() {
        return new LifestyleDataBuilder();
    }

    public static class LifestyleDataBuilder {
        private String id;
        private String studentProfileId;
        private LocalDate date;
        private double sleepHours;
        private double screenTimeHours;
        private int stressLevel;
        private double exerciseMinutes;
        private double studyMinutes;
        private double attendanceRate;
        private double productivityRating;

        public LifestyleDataBuilder id(String id) {
            this.id = id;
            return this;
        }

        public LifestyleDataBuilder studentProfileId(String studentProfileId) {
            this.studentProfileId = studentProfileId;
            return this;
        }

        public LifestyleDataBuilder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public LifestyleDataBuilder sleepHours(double sleepHours) {
            this.sleepHours = sleepHours;
            return this;
        }

        public LifestyleDataBuilder screenTimeHours(double screenTimeHours) {
            this.screenTimeHours = screenTimeHours;
            return this;
        }

        public LifestyleDataBuilder stressLevel(int stressLevel) {
            this.stressLevel = stressLevel;
            return this;
        }

        public LifestyleDataBuilder exerciseMinutes(double exerciseMinutes) {
            this.exerciseMinutes = exerciseMinutes;
            return this;
        }

        public LifestyleDataBuilder studyMinutes(double studyMinutes) {
            this.studyMinutes = studyMinutes;
            return this;
        }

        public LifestyleDataBuilder attendanceRate(double attendanceRate) {
            this.attendanceRate = attendanceRate;
            return this;
        }

        public LifestyleDataBuilder productivityRating(double productivityRating) {
            this.productivityRating = productivityRating;
            return this;
        }

        public LifestyleData build() {
            return new LifestyleData(id, studentProfileId, date, sleepHours, screenTimeHours, stressLevel, exerciseMinutes, studyMinutes, attendanceRate, productivityRating);
        }
    }
}

