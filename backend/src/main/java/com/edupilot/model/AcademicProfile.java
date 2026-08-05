package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "academic_profiles")
public class AcademicProfile {
    @Id
    private String id;
    private String userId;

    private String institution = "EduPilot Academy";
    private String degree = "B.Tech";
    private String stream = "ENGINEERING"; // Default ENGINEERING, extendable for MEDICAL, COMMERCE, ARTS, LAW, MBA
    private String engineeringBranch;
    private int semester;
    private double currentCgpa;
    private double targetCgpa;
    
    private List<String> currentSubjects;
    private List<String> weakSubjects;
    private List<String> strongSubjects;
    
    private String careerGoal;
    private String dreamCompany;
    
    private List<String> programmingLanguages;
    private List<String> frameworks;
    private List<String> technologies;
    private List<String> projects;
    private List<String> internships;
    private List<String> hackathons;
    
    private String githubUrl;
    private String linkedInUrl;
    private String leetcodeUrl;
    
    private double weeklyCodingHours;
    private String preferredLearningStyle;
    private List<String> academicAchievements;
    private List<String> certifications;
    private LocalDateTime updatedAt;

    public AcademicProfile() {
    }

    public AcademicProfile(String id, String userId, String institution, String degree, String stream, 
                           String engineeringBranch, int semester, double currentCgpa, double targetCgpa, 
                           List<String> currentSubjects, List<String> weakSubjects, List<String> strongSubjects, 
                           String careerGoal, String dreamCompany, List<String> programmingLanguages, 
                           List<String> frameworks, List<String> technologies, List<String> projects, 
                           List<String> internships, List<String> hackathons, String githubUrl, 
                           String linkedInUrl, String leetcodeUrl, double weeklyCodingHours, 
                           String preferredLearningStyle, List<String> academicAchievements, 
                           List<String> certifications, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.institution = institution;
        this.degree = degree;
        this.stream = stream;
        this.engineeringBranch = engineeringBranch;
        this.semester = semester;
        this.currentCgpa = currentCgpa;
        this.targetCgpa = targetCgpa;
        this.currentSubjects = currentSubjects;
        this.weakSubjects = weakSubjects;
        this.strongSubjects = strongSubjects;
        this.careerGoal = careerGoal;
        this.dreamCompany = dreamCompany;
        this.programmingLanguages = programmingLanguages;
        this.frameworks = frameworks;
        this.technologies = technologies;
        this.projects = projects;
        this.internships = internships;
        this.hackathons = hackathons;
        this.githubUrl = githubUrl;
        this.linkedInUrl = linkedInUrl;
        this.leetcodeUrl = leetcodeUrl;
        this.weeklyCodingHours = weeklyCodingHours;
        this.preferredLearningStyle = preferredLearningStyle;
        this.academicAchievements = academicAchievements;
        this.certifications = certifications;
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

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getEngineeringBranch() {
        return engineeringBranch;
    }

    public void setEngineeringBranch(String engineeringBranch) {
        this.engineeringBranch = engineeringBranch;
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

    public List<String> getCurrentSubjects() {
        return currentSubjects;
    }

    public void setCurrentSubjects(List<String> currentSubjects) {
        this.currentSubjects = currentSubjects;
    }

    public List<String> getWeakSubjects() {
        return weakSubjects;
    }

    public void setWeakSubjects(List<String> weakSubjects) {
        this.weakSubjects = weakSubjects;
    }

    public List<String> getStrongSubjects() {
        return strongSubjects;
    }

    public void setStrongSubjects(List<String> strongSubjects) {
        this.strongSubjects = strongSubjects;
    }

    public String getCareerGoal() {
        return careerGoal;
    }

    public void setCareerGoal(String careerGoal) {
        this.careerGoal = careerGoal;
    }

    public String getDreamCompany() {
        return dreamCompany;
    }

    public void setDreamCompany(String dreamCompany) {
        this.dreamCompany = dreamCompany;
    }

    public List<String> getProgrammingLanguages() {
        return programmingLanguages;
    }

    public void setProgrammingLanguages(List<String> programmingLanguages) {
        this.programmingLanguages = programmingLanguages;
    }

    public List<String> getFrameworks() {
        return frameworks;
    }

    public void setFrameworks(List<String> frameworks) {
        this.frameworks = frameworks;
    }

    public List<String> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<String> technologies) {
        this.technologies = technologies;
    }

    public List<String> getProjects() {
        return projects;
    }

    public void setProjects(List<String> projects) {
        this.projects = projects;
    }

    public List<String> getInternships() {
        return internships;
    }

    public void setInternships(List<String> internships) {
        this.internships = internships;
    }

    public List<String> getHackathons() {
        return hackathons;
    }

    public void setHackathons(List<String> hackathons) {
        this.hackathons = hackathons;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getLinkedInUrl() {
        return linkedInUrl;
    }

    public void setLinkedInUrl(String linkedInUrl) {
        this.linkedInUrl = linkedInUrl;
    }

    public String getLeetcodeUrl() {
        return leetcodeUrl;
    }

    public void setLeetcodeUrl(String leetcodeUrl) {
        this.leetcodeUrl = leetcodeUrl;
    }

    public double getWeeklyCodingHours() {
        return weeklyCodingHours;
    }

    public void setWeeklyCodingHours(double weeklyCodingHours) {
        this.weeklyCodingHours = weeklyCodingHours;
    }

    public String getPreferredLearningStyle() {
        return preferredLearningStyle;
    }

    public void setPreferredLearningStyle(String preferredLearningStyle) {
        this.preferredLearningStyle = preferredLearningStyle;
    }

    public List<String> getAcademicAchievements() {
        return academicAchievements;
    }

    public void setAcademicAchievements(List<String> academicAchievements) {
        this.academicAchievements = academicAchievements;
    }

    public List<String> getCertifications() {
        return certifications;
    }

    public void setCertifications(List<String> certifications) {
        this.certifications = certifications;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
