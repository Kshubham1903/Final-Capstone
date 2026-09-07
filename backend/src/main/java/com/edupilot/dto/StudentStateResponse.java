package com.edupilot.dto;

import java.util.Map;

public class StudentStateResponse {
    private String studentId;
    private Map<String, Object> knowledge;
    private Map<String, Double> preference;
    private Double engagement;
    private Map<String, Object> wellbeing;

    public StudentStateResponse() {
    }

    public StudentStateResponse(String studentId, Map<String, Object> knowledge, Map<String, Double> preference, Double engagement, Map<String, Object> wellbeing) {
        this.studentId = studentId;
        this.knowledge = knowledge;
        this.preference = preference;
        this.engagement = engagement;
        this.wellbeing = wellbeing;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Map<String, Object> getKnowledge() {
        return knowledge;
    }

    public void setKnowledge(Map<String, Object> knowledge) {
        this.knowledge = knowledge;
    }

    public Map<String, Double> getPreference() {
        return preference;
    }

    public void setPreference(Map<String, Double> preference) {
        this.preference = preference;
    }

    public Double getEngagement() {
        return engagement;
    }

    public void setEngagement(Double engagement) {
        this.engagement = engagement;
    }

    public Map<String, Object> getWellbeing() {
        return wellbeing;
    }

    public void setWellbeing(Map<String, Object> wellbeing) {
        this.wellbeing = wellbeing;
    }
}
