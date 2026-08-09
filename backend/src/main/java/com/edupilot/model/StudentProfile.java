package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Document(collection = "student_profiles")
public class StudentProfile {
    @Id
    private String id;
    private String userId; // Reference to User
    private String fullName;
    private String email;
    private boolean isCompleted = false;

    // Academic Profile & Identity
    private String institution = "EduPilot Academy";
    private String degree = "B.Tech";
    private String branch = "Computer Science & Engineering";
    private String course = "Computer Science & Engineering";
    private int semester = 1;
    private double currentCgpa = 8.0;
    private double targetCgpa = 8.5;

    private List<String> subjects;
    private List<String> careerGoals;
    private double preferredStudyHoursPerDay;

    // Diagnostics & Streaks
    private int consistencyScore; // 0-100
    private int productivityScore; // 0-100
    private int lifestyleScore; // 0-100
    private String learningStyle; // Visual, Auditory, Reading, Kinesthetic
    private int currentStreakCount;
    private double studentGrowthIndex; // SGI: 0.0 - 10.0
    private java.time.LocalDate lastStreakUpdate;

    // Mastery Map: e.g. "Math" -> 85.5, "Algorithms" -> 62.0
    private Map<String, Double> conceptMastery;

    // Weak and Strong tags: e.g., "Math" -> ["Calculus", "Linear Algebra"]
    private Map<String, List<String>> weakConcepts;
    private Map<String, List<String>> strongConcepts;

    private int completedQuizzesCount;

    private double predictedCgpa = 3.0;
    private String academicRiskLevel = "LOW";

    // Demographic & Lifestyle attributes for background ML
    private String parentalInvolvement = "Medium";
    private String accessToResources = "High";
    private String extracurricularActivities = "Yes";
    private String motivationLevel = "High";
    private String internetAccess = "Yes";
    private int tutoringSessions = 1;
    private String familyIncome = "Medium";
    private String teacherQuality = "Medium";
    private String schoolType = "Public";
    private String peerInfluence = "Positive";
    private String learningDisabilities = "No";
    private String parentalEducationLevel = "College";
    private String distanceFromHome = "Near";
    private String gender = "Male";

    // Historical Lifestyle Logs attached for Dashboard visualization
    private List<Map<String, Object>> lifestyleHistory;

    public StudentProfile() {
    }

    public StudentProfile(String id, String userId, String fullName, String email, boolean isCompleted, 
                          String institution, String degree, String branch, String course, int semester, 
                          double currentCgpa, double targetCgpa, List<String> subjects, List<String> careerGoals, 
                          double preferredStudyHoursPerDay, int consistencyScore, int productivityScore, 
                          int lifestyleScore, String learningStyle, int currentStreakCount, 
                          double studentGrowthIndex, Map<String, Double> conceptMastery, 
                          Map<String, List<String>> weakConcepts, Map<String, List<String>> strongConcepts, 
                          int completedQuizzesCount, double predictedCgpa, String academicRiskLevel, 
                          String parentalInvolvement, String accessToResources, String extracurricularActivities, 
                          String motivationLevel, String internetAccess, int tutoringSessions, String familyIncome, 
                          String teacherQuality, String schoolType, String peerInfluence, String learningDisabilities, 
                          String parentalEducationLevel, String distanceFromHome, String gender, 
                          List<Map<String, Object>> lifestyleHistory) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.isCompleted = isCompleted;
        this.institution = institution;
        this.degree = degree;
        this.branch = branch;
        this.course = course;
        this.semester = semester;
        this.currentCgpa = currentCgpa;
        this.targetCgpa = targetCgpa;
        this.subjects = subjects;
        this.careerGoals = careerGoals;
        this.preferredStudyHoursPerDay = preferredStudyHoursPerDay;
        this.consistencyScore = consistencyScore;
        this.productivityScore = productivityScore;
        this.lifestyleScore = lifestyleScore;
        this.learningStyle = learningStyle;
        this.currentStreakCount = currentStreakCount;
        this.studentGrowthIndex = studentGrowthIndex;
        this.conceptMastery = conceptMastery;
        this.weakConcepts = weakConcepts;
        this.strongConcepts = strongConcepts;
        this.completedQuizzesCount = completedQuizzesCount;
        this.predictedCgpa = predictedCgpa;
        this.academicRiskLevel = academicRiskLevel;
        this.parentalInvolvement = parentalInvolvement;
        this.accessToResources = accessToResources;
        this.extracurricularActivities = extracurricularActivities;
        this.motivationLevel = motivationLevel;
        this.internetAccess = internetAccess;
        this.tutoringSessions = tutoringSessions;
        this.familyIncome = familyIncome;
        this.teacherQuality = teacherQuality;
        this.schoolType = schoolType;
        this.peerInfluence = peerInfluence;
        this.learningDisabilities = learningDisabilities;
        this.parentalEducationLevel = parentalEducationLevel;
        this.distanceFromHome = distanceFromHome;
        this.gender = gender;
        this.lifestyleHistory = lifestyleHistory;
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

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public String getInstitution() {
        return institution != null ? institution : "EduPilot Academy";
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getDegree() {
        return degree != null ? degree : "B.Tech";
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getBranch() {
        return branch != null ? branch : (course != null ? course : "Computer Science & Engineering");
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public double getCurrentCgpa() {
        return currentCgpa;
    }

    public void setCurrentCgpa(double currentCgpa) {
        this.currentCgpa = currentCgpa;
    }

    public double getTargetCgpa() {
        return targetCgpa;
    }

    public void setTargetCgpa(double targetCgpa) {
        this.targetCgpa = targetCgpa;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public List<String> getCareerGoals() {
        return careerGoals;
    }

    public void setCareerGoals(List<String> careerGoals) {
        this.careerGoals = careerGoals;
    }

    public double getPreferredStudyHoursPerDay() {
        return preferredStudyHoursPerDay;
    }

    public void setPreferredStudyHoursPerDay(double preferredStudyHoursPerDay) {
        this.preferredStudyHoursPerDay = preferredStudyHoursPerDay;
    }

    public int getConsistencyScore() {
        return consistencyScore;
    }

    public void setConsistencyScore(int consistencyScore) {
        this.consistencyScore = consistencyScore;
    }

    public int getProductivityScore() {
        return productivityScore;
    }

    public void setProductivityScore(int productivityScore) {
        this.productivityScore = productivityScore;
    }

    public int getLifestyleScore() {
        return lifestyleScore;
    }

    public void setLifestyleScore(int lifestyleScore) {
        this.lifestyleScore = lifestyleScore;
    }

    public String getLearningStyle() {
        return learningStyle;
    }

    public void setLearningStyle(String learningStyle) {
        this.learningStyle = learningStyle;
    }

    public int getCurrentStreakCount() {
        return currentStreakCount;
    }

    public void setCurrentStreakCount(int currentStreakCount) {
        this.currentStreakCount = currentStreakCount;
    }

    public java.time.LocalDate getLastStreakUpdate() {
        return lastStreakUpdate;
    }

    public void setLastStreakUpdate(java.time.LocalDate lastStreakUpdate) {
        this.lastStreakUpdate = lastStreakUpdate;
    }

    public double getStudentGrowthIndex() {
        return studentGrowthIndex;
    }

    public void setStudentGrowthIndex(double studentGrowthIndex) {
        this.studentGrowthIndex = studentGrowthIndex;
    }

    public Map<String, Double> getConceptMastery() {
        return conceptMastery;
    }

    public void setConceptMastery(Map<String, Double> conceptMastery) {
        this.conceptMastery = conceptMastery;
    }

    public Map<String, List<String>> getWeakConcepts() {
        return weakConcepts;
    }

    public void setWeakConcepts(Map<String, List<String>> weakConcepts) {
        this.weakConcepts = weakConcepts;
    }

    public Map<String, List<String>> getStrongConcepts() {
        return strongConcepts;
    }

    public void setStrongConcepts(Map<String, List<String>> strongConcepts) {
        this.strongConcepts = strongConcepts;
    }

    public int getCompletedQuizzesCount() {
        return completedQuizzesCount;
    }

    public void setCompletedQuizzesCount(int completedQuizzesCount) {
        this.completedQuizzesCount = completedQuizzesCount;
    }

    public double getPredictedCgpa() {
        return predictedCgpa;
    }

    public void setPredictedCgpa(double predictedCgpa) {
        this.predictedCgpa = predictedCgpa;
    }

    public String getAcademicRiskLevel() {
        return academicRiskLevel;
    }

    public void setAcademicRiskLevel(String academicRiskLevel) {
        this.academicRiskLevel = academicRiskLevel;
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

    public int getTutoringSessions() {
        return tutoringSessions;
    }

    public void setTutoringSessions(int tutoringSessions) {
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

    public List<Map<String, Object>> getLifestyleHistory() {
        return lifestyleHistory;
    }

    public void setLifestyleHistory(List<Map<String, Object>> lifestyleHistory) {
        this.lifestyleHistory = lifestyleHistory;
    }

    public static StudentProfileBuilder builder() {
        return new StudentProfileBuilder();
    }

    public static class StudentProfileBuilder {
        private String id;
        private String userId;
        private String fullName;
        private String email;
        private boolean isCompleted = false;
        private String institution = "EduPilot Academy";
        private String degree = "B.Tech";
        private String branch = "Computer Science & Engineering";
        private String course = "Computer Science & Engineering";
        private int semester = 1;
        private double currentCgpa = 8.0;
        private double targetCgpa = 8.5;
        private List<String> subjects;
        private List<String> careerGoals;
        private double preferredStudyHoursPerDay;
        private int consistencyScore;
        private int productivityScore;
        private int lifestyleScore;
        private String learningStyle;
        private int currentStreakCount;
        private double studentGrowthIndex;
        private Map<String, Double> conceptMastery;
        private Map<String, List<String>> weakConcepts;
        private Map<String, List<String>> strongConcepts;
        private int completedQuizzesCount;
        private double predictedCgpa = 3.0;
        private String academicRiskLevel = "LOW";
        private String parentalInvolvement = "Medium";
        private String accessToResources = "High";
        private String extracurricularActivities = "Yes";
        private String motivationLevel = "High";
        private String internetAccess = "Yes";
        private int tutoringSessions = 1;
        private String familyIncome = "Medium";
        private String teacherQuality = "Medium";
        private String schoolType = "Public";
        private String peerInfluence = "Positive";
        private String learningDisabilities = "No";
        private String parentalEducationLevel = "College";
        private String distanceFromHome = "Near";
        private String gender = "Male";
        private List<Map<String, Object>> lifestyleHistory;

        public StudentProfileBuilder id(String id) {
            this.id = id;
            return this;
        }

        public StudentProfileBuilder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public StudentProfileBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public StudentProfileBuilder email(String email) {
            this.email = email;
            return this;
        }

        public StudentProfileBuilder isCompleted(boolean isCompleted) {
            this.isCompleted = isCompleted;
            return this;
        }

        public StudentProfileBuilder institution(String institution) {
            this.institution = institution;
            return this;
        }

        public StudentProfileBuilder degree(String degree) {
            this.degree = degree;
            return this;
        }

        public StudentProfileBuilder branch(String branch) {
            this.branch = branch;
            return this;
        }

        public StudentProfileBuilder course(String course) {
            this.course = course;
            return this;
        }

        public StudentProfileBuilder semester(int semester) {
            this.semester = semester;
            return this;
        }

        public StudentProfileBuilder currentCgpa(double currentCgpa) {
            this.currentCgpa = currentCgpa;
            return this;
        }

        public StudentProfileBuilder targetCgpa(double targetCgpa) {
            this.targetCgpa = targetCgpa;
            return this;
        }

        public StudentProfileBuilder subjects(List<String> subjects) {
            this.subjects = subjects;
            return this;
        }

        public StudentProfileBuilder careerGoals(List<String> careerGoals) {
            this.careerGoals = careerGoals;
            return this;
        }

        public StudentProfileBuilder preferredStudyHoursPerDay(double preferredStudyHoursPerDay) {
            this.preferredStudyHoursPerDay = preferredStudyHoursPerDay;
            return this;
        }

        public StudentProfileBuilder consistencyScore(int consistencyScore) {
            this.consistencyScore = consistencyScore;
            return this;
        }

        public StudentProfileBuilder productivityScore(int productivityScore) {
            this.productivityScore = productivityScore;
            return this;
        }

        public StudentProfileBuilder lifestyleScore(int lifestyleScore) {
            this.lifestyleScore = lifestyleScore;
            return this;
        }

        public StudentProfileBuilder learningStyle(String learningStyle) {
            this.learningStyle = learningStyle;
            return this;
        }

        public StudentProfileBuilder currentStreakCount(int currentStreakCount) {
            this.currentStreakCount = currentStreakCount;
            return this;
        }

        public StudentProfileBuilder studentGrowthIndex(double studentGrowthIndex) {
            this.studentGrowthIndex = studentGrowthIndex;
            return this;
        }

        public StudentProfileBuilder conceptMastery(Map<String, Double> conceptMastery) {
            this.conceptMastery = conceptMastery;
            return this;
        }

        public StudentProfileBuilder weakConcepts(Map<String, List<String>> weakConcepts) {
            this.weakConcepts = weakConcepts;
            return this;
        }

        public StudentProfileBuilder strongConcepts(Map<String, List<String>> strongConcepts) {
            this.strongConcepts = strongConcepts;
            return this;
        }

        public StudentProfileBuilder completedQuizzesCount(int completedQuizzesCount) {
            this.completedQuizzesCount = completedQuizzesCount;
            return this;
        }

        public StudentProfileBuilder predictedCgpa(double predictedCgpa) {
            this.predictedCgpa = predictedCgpa;
            return this;
        }

        public StudentProfileBuilder academicRiskLevel(String academicRiskLevel) {
            this.academicRiskLevel = academicRiskLevel;
            return this;
        }

        public StudentProfileBuilder parentalInvolvement(String parentalInvolvement) {
            this.parentalInvolvement = parentalInvolvement;
            return this;
        }

        public StudentProfileBuilder accessToResources(String accessToResources) {
            this.accessToResources = accessToResources;
            return this;
        }

        public StudentProfileBuilder extracurricularActivities(String extracurricularActivities) {
            this.extracurricularActivities = extracurricularActivities;
            return this;
        }

        public StudentProfileBuilder motivationLevel(String motivationLevel) {
            this.motivationLevel = motivationLevel;
            return this;
        }

        public StudentProfileBuilder internetAccess(String internetAccess) {
            this.internetAccess = internetAccess;
            return this;
        }

        public StudentProfileBuilder tutoringSessions(int tutoringSessions) {
            this.tutoringSessions = tutoringSessions;
            return this;
        }

        public StudentProfileBuilder familyIncome(String familyIncome) {
            this.familyIncome = familyIncome;
            return this;
        }

        public StudentProfileBuilder teacherQuality(String teacherQuality) {
            this.teacherQuality = teacherQuality;
            return this;
        }

        public StudentProfileBuilder schoolType(String schoolType) {
            this.schoolType = schoolType;
            return this;
        }

        public StudentProfileBuilder peerInfluence(String peerInfluence) {
            this.peerInfluence = peerInfluence;
            return this;
        }

        public StudentProfileBuilder learningDisabilities(String learningDisabilities) {
            this.learningDisabilities = learningDisabilities;
            return this;
        }

        public StudentProfileBuilder parentalEducationLevel(String parentalEducationLevel) {
            this.parentalEducationLevel = parentalEducationLevel;
            return this;
        }

        public StudentProfileBuilder distanceFromHome(String distanceFromHome) {
            this.distanceFromHome = distanceFromHome;
            return this;
        }

        public StudentProfileBuilder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public StudentProfileBuilder lifestyleHistory(List<Map<String, Object>> lifestyleHistory) {
            this.lifestyleHistory = lifestyleHistory;
            return this;
        }

        public StudentProfile build() {
            return new StudentProfile(id, userId, fullName, email, isCompleted, institution, degree, branch, course, 
                                      semester, currentCgpa, targetCgpa, subjects, careerGoals, preferredStudyHoursPerDay, 
                                      consistencyScore, productivityScore, lifestyleScore, learningStyle, currentStreakCount, 
                                      studentGrowthIndex, conceptMastery, weakConcepts, strongConcepts, completedQuizzesCount, 
                                      predictedCgpa, academicRiskLevel, parentalInvolvement, accessToResources, 
                                      extracurricularActivities, motivationLevel, internetAccess, tutoringSessions, 
                                      familyIncome, teacherQuality, schoolType, peerInfluence, learningDisabilities, 
                                      parentalEducationLevel, distanceFromHome, gender, lifestyleHistory);
        }
    }
}
