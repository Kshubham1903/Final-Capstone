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
}
