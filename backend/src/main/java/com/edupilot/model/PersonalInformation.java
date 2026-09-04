package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "personal_information")
public class PersonalInformation {
    @Id
    private String id;
    private String userId;

    private String fullName;
    private String email;
    private String phone;
    private String dateOfBirth;
    private String gender;
    private String address;
    private String city;
    private String state;
    private String country;
    private String collegeName;
    private String university;
    private String engineeringBranch;
    private int currentSemester;
    private String rollNumber;
    private int admissionYear;
    private int expectedGraduationYear;
    private String profilePhotoUrl;
    private LocalDateTime updatedAt;

    public PersonalInformation() {
    }

    public PersonalInformation(String id, String userId, String fullName, String email, String phone, String dateOfBirth, String gender, String address, String city, String state, String country, String collegeName, String university, String engineeringBranch, int currentSemester, String rollNumber, int admissionYear, int expectedGraduationYear, String profilePhotoUrl, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.address = address;
        this.city = city;
        this.state = state;
        this.country = country;
        this.collegeName = collegeName;
        this.university = university;
        this.engineeringBranch = engineeringBranch;
        this.currentSemester = currentSemester;
        this.rollNumber = rollNumber;
        this.admissionYear = admissionYear;
        this.expectedGraduationYear = expectedGraduationYear;
        this.profilePhotoUrl = profilePhotoUrl;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getEngineeringBranch() {
        return engineeringBranch;
    }

    public void setEngineeringBranch(String engineeringBranch) {
        this.engineeringBranch = engineeringBranch;
    }

    public int getCurrentSemester() {
        return currentSemester;
    }

    public void setCurrentSemester(int currentSemester) {
        this.currentSemester = currentSemester;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public int getAdmissionYear() {
        return admissionYear;
    }

    public void setAdmissionYear(int admissionYear) {
        this.admissionYear = admissionYear;
    }

    public int getExpectedGraduationYear() {
        return expectedGraduationYear;
    }

    public void setExpectedGraduationYear(int expectedGraduationYear) {
        this.expectedGraduationYear = expectedGraduationYear;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Form A Academic & Student Profile Fields
    private String academicYear;
    private String division;
    private String previousProgrammingExperience;
    private double previousSemesterSgpa;
    private double previousSemesterPercentage;
    private double mathematicsScore;
    private double programmingScore;
    private double dataStructuresScore;
    private double dbmsScore;
    private double attendancePercentage;
    private int numberOfBacklogs;
    private int programmingConfidence;
    private int dataStructuresConfidence;
    private int dbmsConfidence;
    private int mathematicsConfidence;
    private int algorithmsConfidence;

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getPreviousProgrammingExperience() {
        return previousProgrammingExperience;
    }

    public void setPreviousProgrammingExperience(String previousProgrammingExperience) {
        this.previousProgrammingExperience = previousProgrammingExperience;
    }

    public double getPreviousSemesterSgpa() {
        return previousSemesterSgpa;
    }

    public void setPreviousSemesterSgpa(double previousSemesterSgpa) {
        this.previousSemesterSgpa = previousSemesterSgpa;
    }

    public double getPreviousSemesterPercentage() {
        return previousSemesterPercentage;
    }

    public void setPreviousSemesterPercentage(double previousSemesterPercentage) {
        this.previousSemesterPercentage = previousSemesterPercentage;
    }

    public double getMathematicsScore() {
        return mathematicsScore;
    }

    public void setMathematicsScore(double mathematicsScore) {
        this.mathematicsScore = mathematicsScore;
    }

    public double getProgrammingScore() {
        return programmingScore;
    }

    public void setProgrammingScore(double programmingScore) {
        this.programmingScore = programmingScore;
    }

    public double getDataStructuresScore() {
        return dataStructuresScore;
    }

    public void setDataStructuresScore(double dataStructuresScore) {
        this.dataStructuresScore = dataStructuresScore;
    }

    public double getDbmsScore() {
        return dbmsScore;
    }

    public void setDbmsScore(double dbmsScore) {
        this.dbmsScore = dbmsScore;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public int getNumberOfBacklogs() {
        return numberOfBacklogs;
    }

    public void setNumberOfBacklogs(int numberOfBacklogs) {
        this.numberOfBacklogs = numberOfBacklogs;
    }

    public int getProgrammingConfidence() {
        return programmingConfidence;
    }

    public void setProgrammingConfidence(int programmingConfidence) {
        this.programmingConfidence = programmingConfidence;
    }

    public int getDataStructuresConfidence() {
        return dataStructuresConfidence;
    }

    public void setDataStructuresConfidence(int dataStructuresConfidence) {
        this.dataStructuresConfidence = dataStructuresConfidence;
    }

    public int getDbmsConfidence() {
        return dbmsConfidence;
    }

    public void setDbmsConfidence(int dbmsConfidence) {
        this.dbmsConfidence = dbmsConfidence;
    }

    public int getMathematicsConfidence() {
        return mathematicsConfidence;
    }

    public void setMathematicsConfidence(int mathematicsConfidence) {
        this.mathematicsConfidence = mathematicsConfidence;
    }

    public int getAlgorithmsConfidence() {
        return algorithmsConfidence;
    }

    public void setAlgorithmsConfidence(int algorithmsConfidence) {
        this.algorithmsConfidence = algorithmsConfidence;
    }
}
