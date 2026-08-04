package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lifestyle_questionnaires")
public class LifestyleQuestionnaire {
    @Id
    private String id;
    private String studentProfileId;

    private double hoursStudied;
    private double attendance;
    private String parentalInvolvement;
    private String accessToResources;
    private String extracurricularActivities;
    private double sleepHours;
    private double previousScores;
    private String motivationLevel;
    private String internetAccess;
    private double tutoringSessions;
    private String familyIncome;
    private String teacherQuality;
    private String schoolType;
    private String peerInfluence;
    private double physicalActivity;
    private String learningDisabilities;
    private String parentalEducationLevel;
    private String distanceFromHome;
    private String gender;

    public LifestyleQuestionnaire() {
    }

    public LifestyleQuestionnaire(String id, String studentProfileId, double hoursStudied, double attendance, String parentalInvolvement, String accessToResources, String extracurricularActivities, double sleepHours, double previousScores, String motivationLevel, String internetAccess, double tutoringSessions, String familyIncome, String teacherQuality, String schoolType, String peerInfluence, double physicalActivity, String learningDisabilities, String parentalEducationLevel, String distanceFromHome, String gender) {
        this.id = id;
        this.studentProfileId = studentProfileId;
        this.hoursStudied = hoursStudied;
        this.attendance = attendance;
        this.parentalInvolvement = parentalInvolvement;
        this.accessToResources = accessToResources;
        this.extracurricularActivities = extracurricularActivities;
        this.sleepHours = sleepHours;
        this.previousScores = previousScores;
        this.motivationLevel = motivationLevel;
        this.internetAccess = internetAccess;
        this.tutoringSessions = tutoringSessions;
        this.familyIncome = familyIncome;
        this.teacherQuality = teacherQuality;
        this.schoolType = schoolType;
        this.peerInfluence = peerInfluence;
        this.physicalActivity = physicalActivity;
        this.learningDisabilities = learningDisabilities;
        this.parentalEducationLevel = parentalEducationLevel;
        this.distanceFromHome = distanceFromHome;
        this.gender = gender;
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

    public double getHoursStudied() {
        return hoursStudied;
    }

    public void setHoursStudied(double hoursStudied) {
        this.hoursStudied = hoursStudied;
    }

    public double getAttendance() {
        return attendance;
    }

    public void setAttendance(double attendance) {
        this.attendance = attendance;
    }

    public String getParentalInvolvement() {
        return parentalInvolvement;
    }

    public void setParentalInvolvement(String parentalInvolvement) {
        this.parentalInvolvement = parentalInvolvement;
    }

    public String getAccessToResources() {
        return accessToResources;
    }

    public void setAccessToResources(String accessToResources) {
        this.accessToResources = accessToResources;
    }

    public String getExtracurricularActivities() {
        return extracurricularActivities;
    }

    public void setExtracurricularActivities(String extracurricularActivities) {
        this.extracurricularActivities = extracurricularActivities;
    }

    public double getSleepHours() {
        return sleepHours;
    }

    public void setSleepHours(double sleepHours) {
        this.sleepHours = sleepHours;
    }

    public double getPreviousScores() {
        return previousScores;
    }

    public void setPreviousScores(double previousScores) {
        this.previousScores = previousScores;
    }

    public String getMotivationLevel() {
        return motivationLevel;
    }

    public void setMotivationLevel(String motivationLevel) {
        this.motivationLevel = motivationLevel;
    }

    public String getInternetAccess() {
        return internetAccess;
    }

    public void setInternetAccess(String internetAccess) {
        this.internetAccess = internetAccess;
    }

    public double getTutoringSessions() {
        return tutoringSessions;
    }

    public void setTutoringSessions(double tutoringSessions) {
        this.tutoringSessions = tutoringSessions;
    }

    public String getFamilyIncome() {
        return familyIncome;
    }

    public void setFamilyIncome(String familyIncome) {
        this.familyIncome = familyIncome;
    }

    public String getTeacherQuality() {
        return teacherQuality;
    }

    public void setTeacherQuality(String teacherQuality) {
        this.teacherQuality = teacherQuality;
    }

    public String getSchoolType() {
        return schoolType;
    }

    public void setSchoolType(String schoolType) {
        this.schoolType = schoolType;
    }

    public String getPeerInfluence() {
        return peerInfluence;
    }

    public void setPeerInfluence(String peerInfluence) {
        this.peerInfluence = peerInfluence;
    }

    public double getPhysicalActivity() {
        return physicalActivity;
    }

    public void setPhysicalActivity(double physicalActivity) {
        this.physicalActivity = physicalActivity;
    }

    public String getLearningDisabilities() {
        return learningDisabilities;
    }

    public void setLearningDisabilities(String learningDisabilities) {
        this.learningDisabilities = learningDisabilities;
    }

    public String getParentalEducationLevel() {
        return parentalEducationLevel;
    }

    public void setParentalEducationLevel(String parentalEducationLevel) {
        this.parentalEducationLevel = parentalEducationLevel;
    }

    public String getDistanceFromHome() {
        return distanceFromHome;
    }

    public void setDistanceFromHome(String distanceFromHome) {
        this.distanceFromHome = distanceFromHome;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public static LifestyleQuestionnaireBuilder builder() {
        return new LifestyleQuestionnaireBuilder();
    }

    public static class LifestyleQuestionnaireBuilder {
        private String id;
        private String studentProfileId;
        private double hoursStudied;
        private double attendance;
        private String parentalInvolvement;
        private String accessToResources;
        private String extracurricularActivities;
        private double sleepHours;
        private double previousScores;
        private String motivationLevel;
        private String internetAccess;
        private double tutoringSessions;
        private String familyIncome;
        private String teacherQuality;
        private String schoolType;
        private String peerInfluence;
        private double physicalActivity;
        private String learningDisabilities;
        private String parentalEducationLevel;
        private String distanceFromHome;
        private String gender;

        public LifestyleQuestionnaireBuilder id(String id) {
            this.id = id;
            return this;
        }

        public LifestyleQuestionnaireBuilder studentProfileId(String studentProfileId) {
            this.studentProfileId = studentProfileId;
            return this;
        }

        public LifestyleQuestionnaireBuilder hoursStudied(double hoursStudied) {
            this.hoursStudied = hoursStudied;
            return this;
        }

        public LifestyleQuestionnaireBuilder attendance(double attendance) {
            this.attendance = attendance;
            return this;
        }

        public LifestyleQuestionnaireBuilder parentalInvolvement(String parentalInvolvement) {
            this.parentalInvolvement = parentalInvolvement;
            return this;
        }

        public LifestyleQuestionnaireBuilder accessToResources(String accessToResources) {
            this.accessToResources = accessToResources;
            return this;
        }

        public LifestyleQuestionnaireBuilder extracurricularActivities(String extracurricularActivities) {
            this.extracurricularActivities = extracurricularActivities;
            return this;
        }

        public LifestyleQuestionnaireBuilder sleepHours(double sleepHours) {
            this.sleepHours = sleepHours;
            return this;
        }

        public LifestyleQuestionnaireBuilder previousScores(double previousScores) {
            this.previousScores = previousScores;
            return this;
        }

        public LifestyleQuestionnaireBuilder motivationLevel(String motivationLevel) {
            this.motivationLevel = motivationLevel;
            return this;
        }

        public LifestyleQuestionnaireBuilder internetAccess(String internetAccess) {
            this.internetAccess = internetAccess;
            return this;
        }

        public LifestyleQuestionnaireBuilder tutoringSessions(double tutoringSessions) {
            this.tutoringSessions = tutoringSessions;
            return this;
        }

        public LifestyleQuestionnaireBuilder familyIncome(String familyIncome) {
            this.familyIncome = familyIncome;
            return this;
        }

        public LifestyleQuestionnaireBuilder teacherQuality(String teacherQuality) {
            this.teacherQuality = teacherQuality;
            return this;
        }

        public LifestyleQuestionnaireBuilder schoolType(String schoolType) {
            this.schoolType = schoolType;
            return this;
        }

        public LifestyleQuestionnaireBuilder peerInfluence(String peerInfluence) {
            this.peerInfluence = peerInfluence;
            return this;
        }

        public LifestyleQuestionnaireBuilder physicalActivity(double physicalActivity) {
            this.physicalActivity = physicalActivity;
            return this;
        }

        public LifestyleQuestionnaireBuilder learningDisabilities(String learningDisabilities) {
            this.learningDisabilities = learningDisabilities;
            return this;
        }

        public LifestyleQuestionnaireBuilder parentalEducationLevel(String parentalEducationLevel) {
            this.parentalEducationLevel = parentalEducationLevel;
            return this;
        }

        public LifestyleQuestionnaireBuilder distanceFromHome(String distanceFromHome) {
            this.distanceFromHome = distanceFromHome;
            return this;
        }

        public LifestyleQuestionnaireBuilder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public LifestyleQuestionnaire build() {
            return new LifestyleQuestionnaire(id, studentProfileId, hoursStudied, attendance, parentalInvolvement, accessToResources, extracurricularActivities, sleepHours, previousScores, motivationLevel, internetAccess, tutoringSessions, familyIncome, teacherQuality, schoolType, peerInfluence, physicalActivity, learningDisabilities, parentalEducationLevel, distanceFromHome, gender);
        }
    }
}

