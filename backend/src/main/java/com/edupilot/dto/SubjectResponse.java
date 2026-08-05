package com.edupilot.dto;

import com.edupilot.model.Subject;

public class SubjectResponse {
    private String id;
    private String institution;
    private String degree;
    private String branch;
    private int semester;
    private String subjectCode;
    private String subjectName;
    private int credits;
    private boolean isActive;

    public SubjectResponse() {
    }

    public SubjectResponse(Subject subject) {
        if (subject != null) {
            this.id = subject.getId();
            this.institution = subject.getInstitution();
            this.degree = subject.getDegree();
            this.branch = subject.getBranch();
            this.semester = subject.getSemester();
            this.subjectCode = subject.getSubjectCode();
            this.subjectName = subject.getSubjectName();
            this.credits = subject.getCredits();
            this.isActive = subject.isActive();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
