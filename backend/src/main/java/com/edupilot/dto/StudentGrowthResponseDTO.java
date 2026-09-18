package com.edupilot.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class StudentGrowthResponseDTO {

    private String userId;
    private Double baselineKnowledge; // K0 (%)
    private Double currentKnowledge;  // Kt (%)
    private Double cumulativeGrowth;  // Kt - K0 (percentage points, pp)
    private Double recentGain;        // Kt - Kt-1 (percentage points, pp)
    private Integer conceptsImproved;
    private Integer weakConceptsRemaining;
    private Map<String, SubjectGrowthDTO> subjectGrowth;
    private List<GrowthTrajectoryPointDTO> trajectory;
    private List<ConceptImprovementDTO> conceptImprovements;
    private String dataSufficiencyNote;

    public StudentGrowthResponseDTO() {}

    public StudentGrowthResponseDTO(
            String userId,
            Double baselineKnowledge,
            Double currentKnowledge,
            Double cumulativeGrowth,
            Double recentGain,
            Integer conceptsImproved,
            Integer weakConceptsRemaining,
            Map<String, SubjectGrowthDTO> subjectGrowth,
            List<GrowthTrajectoryPointDTO> trajectory,
            List<ConceptImprovementDTO> conceptImprovements,
            String dataSufficiencyNote) {
        this.userId = userId;
        this.baselineKnowledge = baselineKnowledge;
        this.currentKnowledge = currentKnowledge;
        this.cumulativeGrowth = cumulativeGrowth;
        this.recentGain = recentGain;
        this.conceptsImproved = conceptsImproved;
        this.weakConceptsRemaining = weakConceptsRemaining;
        this.subjectGrowth = subjectGrowth;
        this.trajectory = trajectory;
        this.conceptImprovements = conceptImprovements;
        this.dataSufficiencyNote = dataSufficiencyNote;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Double getBaselineKnowledge() { return baselineKnowledge; }
    public void setBaselineKnowledge(Double baselineKnowledge) { this.baselineKnowledge = baselineKnowledge; }

    public Double getCurrentKnowledge() { return currentKnowledge; }
    public void setCurrentKnowledge(Double currentKnowledge) { this.currentKnowledge = currentKnowledge; }

    public Double getCumulativeGrowth() { return cumulativeGrowth; }
    public void setCumulativeGrowth(Double cumulativeGrowth) { this.cumulativeGrowth = cumulativeGrowth; }

    public Double getRecentGain() { return recentGain; }
    public void setRecentGain(Double recentGain) { this.recentGain = recentGain; }

    public Integer getConceptsImproved() { return conceptsImproved; }
    public void setConceptsImproved(Integer conceptsImproved) { this.conceptsImproved = conceptsImproved; }

    public Integer getWeakConceptsRemaining() { return weakConceptsRemaining; }
    public void setWeakConceptsRemaining(Integer weakConceptsRemaining) { this.weakConceptsRemaining = weakConceptsRemaining; }

    public Map<String, SubjectGrowthDTO> getSubjectGrowth() { return subjectGrowth; }
    public void setSubjectGrowth(Map<String, SubjectGrowthDTO> subjectGrowth) { this.subjectGrowth = subjectGrowth; }

    public List<GrowthTrajectoryPointDTO> getTrajectory() { return trajectory; }
    public void setTrajectory(List<GrowthTrajectoryPointDTO> trajectory) { this.trajectory = trajectory; }

    public List<ConceptImprovementDTO> getConceptImprovements() { return conceptImprovements; }
    public void setConceptImprovements(List<ConceptImprovementDTO> conceptImprovements) { this.conceptImprovements = conceptImprovements; }

    public String getDataSufficiencyNote() { return dataSufficiencyNote; }
    public void setDataSufficiencyNote(String dataSufficiencyNote) { this.dataSufficiencyNote = dataSufficiencyNote; }

    // Inner DTO for Subject Growth
    public static class SubjectGrowthDTO {
        private String subjectName;
        private Double baselineKnowledge;
        private Double currentKnowledge;
        private Double cumulativeGrowth;
        private Integer totalConcepts;
        private Integer weakConcepts;

        public SubjectGrowthDTO() {}

        public SubjectGrowthDTO(String subjectName, Double baselineKnowledge, Double currentKnowledge, Double cumulativeGrowth, Integer totalConcepts, Integer weakConcepts) {
            this.subjectName = subjectName;
            this.baselineKnowledge = baselineKnowledge;
            this.currentKnowledge = currentKnowledge;
            this.cumulativeGrowth = cumulativeGrowth;
            this.totalConcepts = totalConcepts;
            this.weakConcepts = weakConcepts;
        }

        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

        public Double getBaselineKnowledge() { return baselineKnowledge; }
        public void setBaselineKnowledge(Double baselineKnowledge) { this.baselineKnowledge = baselineKnowledge; }

        public Double getCurrentKnowledge() { return currentKnowledge; }
        public void setCurrentKnowledge(Double currentKnowledge) { this.currentKnowledge = currentKnowledge; }

        public Double getCumulativeGrowth() { return cumulativeGrowth; }
        public void setCumulativeGrowth(Double cumulativeGrowth) { this.cumulativeGrowth = cumulativeGrowth; }

        public Integer getTotalConcepts() { return totalConcepts; }
        public void setTotalConcepts(Integer totalConcepts) { this.totalConcepts = totalConcepts; }

        public Integer getWeakConcepts() { return weakConcepts; }
        public void setWeakConcepts(Integer weakConcepts) { this.weakConcepts = weakConcepts; }
    }

    // Inner DTO for Trajectory Point
    public static class GrowthTrajectoryPointDTO {
        private String eventId;
        private LocalDateTime timestamp;
        private String assessmentType; // DIAGNOSTIC, ADAPTIVE_QUIZ, PRACTICE_QUIZ, VERIFICATION_QUIZ, REMEDIATION_ASSESSMENT
        private String subjectName;
        private Double knowledgeScore; // K(t) (%)
        private Double baselineKnowledge; // K0 (%)
        private Double cumulativeGrowth;  // (pp)
        private Double recentGain;        // (pp)
        private String description;

        public GrowthTrajectoryPointDTO() {}

        public GrowthTrajectoryPointDTO(String eventId, LocalDateTime timestamp, String assessmentType, String subjectName, Double knowledgeScore, Double baselineKnowledge, Double cumulativeGrowth, Double recentGain, String description) {
            this.eventId = eventId;
            this.timestamp = timestamp;
            this.assessmentType = assessmentType;
            this.subjectName = subjectName;
            this.knowledgeScore = knowledgeScore;
            this.baselineKnowledge = baselineKnowledge;
            this.cumulativeGrowth = cumulativeGrowth;
            this.recentGain = recentGain;
            this.description = description;
        }

        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public String getAssessmentType() { return assessmentType; }
        public void setAssessmentType(String assessmentType) { this.assessmentType = assessmentType; }

        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

        public Double getKnowledgeScore() { return knowledgeScore; }
        public void setKnowledgeScore(Double knowledgeScore) { this.knowledgeScore = knowledgeScore; }

        public Double getBaselineKnowledge() { return baselineKnowledge; }
        public void setBaselineKnowledge(Double baselineKnowledge) { this.baselineKnowledge = baselineKnowledge; }

        public Double getCumulativeGrowth() { return cumulativeGrowth; }
        public void setCumulativeGrowth(Double cumulativeGrowth) { this.cumulativeGrowth = cumulativeGrowth; }

        public Double getRecentGain() { return recentGain; }
        public void setRecentGain(Double recentGain) { this.recentGain = recentGain; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // Inner DTO for Concept Improvement Tracking
    public static class ConceptImprovementDTO {
        private String conceptName;
        private String subjectName;
        private Double baselineAccuracy;
        private Double currentAccuracy;
        private Double growthPoints; // pp
        private Boolean isImproved;  // true if growthPoints >= 5.0 pp

        public ConceptImprovementDTO() {}

        public ConceptImprovementDTO(String conceptName, String subjectName, Double baselineAccuracy, Double currentAccuracy, Double growthPoints, Boolean isImproved) {
            this.conceptName = conceptName;
            this.subjectName = subjectName;
            this.baselineAccuracy = baselineAccuracy;
            this.currentAccuracy = currentAccuracy;
            this.growthPoints = growthPoints;
            this.isImproved = isImproved;
        }

        public String getConceptName() { return conceptName; }
        public void setConceptName(String conceptName) { this.conceptName = conceptName; }

        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

        public Double getBaselineAccuracy() { return baselineAccuracy; }
        public void setBaselineAccuracy(Double baselineAccuracy) { this.baselineAccuracy = baselineAccuracy; }

        public Double getCurrentAccuracy() { return currentAccuracy; }
        public void setCurrentAccuracy(Double currentAccuracy) { this.currentAccuracy = currentAccuracy; }

        public Double getGrowthPoints() { return growthPoints; }
        public void setGrowthPoints(Double growthPoints) { this.growthPoints = growthPoints; }

        public Boolean getIsImproved() { return isImproved; }
        public void setIsImproved(Boolean isImproved) { this.isImproved = isImproved; }
    }

    // Inner DTO for Leak-Free ML Features
    public static class MLFeaturePointDTO {
        private String studentId;
        private LocalDateTime timestamp;
        private String subject;
        private String concept;
        private Double baselineKnowledgeAtT;
        private Double currentKnowledgeAtT;
        private Double cumulativeGrowthAtT;
        private Double recentGainAtT;
        private Integer attemptCountAtT;
        private Double accuracyAtT;
        private Double confidenceAtT;
        private String activityType;
        private Double targetKnowledgeAtNextT; // Outcome at T+1

        public MLFeaturePointDTO() {}

        public MLFeaturePointDTO(String studentId, LocalDateTime timestamp, String subject, String concept, Double baselineKnowledgeAtT, Double currentKnowledgeAtT, Double cumulativeGrowthAtT, Double recentGainAtT, Integer attemptCountAtT, Double accuracyAtT, Double confidenceAtT, String activityType, Double targetKnowledgeAtNextT) {
            this.studentId = studentId;
            this.timestamp = timestamp;
            this.subject = subject;
            this.concept = concept;
            this.baselineKnowledgeAtT = baselineKnowledgeAtT;
            this.currentKnowledgeAtT = currentKnowledgeAtT;
            this.cumulativeGrowthAtT = cumulativeGrowthAtT;
            this.recentGainAtT = recentGainAtT;
            this.attemptCountAtT = attemptCountAtT;
            this.accuracyAtT = accuracyAtT;
            this.confidenceAtT = confidenceAtT;
            this.activityType = activityType;
            this.targetKnowledgeAtNextT = targetKnowledgeAtNextT;
        }

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getConcept() { return concept; }
        public void setConcept(String concept) { this.concept = concept; }

        public Double getBaselineKnowledgeAtT() { return baselineKnowledgeAtT; }
        public void setBaselineKnowledgeAtT(Double baselineKnowledgeAtT) { this.baselineKnowledgeAtT = baselineKnowledgeAtT; }

        public Double getCurrentKnowledgeAtT() { return currentKnowledgeAtT; }
        public void setCurrentKnowledgeAtT(Double currentKnowledgeAtT) { this.currentKnowledgeAtT = currentKnowledgeAtT; }

        public Double getCumulativeGrowthAtT() { return cumulativeGrowthAtT; }
        public void setCumulativeGrowthAtT(Double cumulativeGrowthAtT) { this.cumulativeGrowthAtT = cumulativeGrowthAtT; }

        public Double getRecentGainAtT() { return recentGainAtT; }
        public void setRecentGainAtT(Double recentGainAtT) { this.recentGainAtT = recentGainAtT; }

        public Integer getAttemptCountAtT() { return attemptCountAtT; }
        public void setAttemptCountAtT(Integer attemptCountAtT) { this.attemptCountAtT = attemptCountAtT; }

        public Double getAccuracyAtT() { return accuracyAtT; }
        public void setAccuracyAtT(Double accuracyAtT) { this.accuracyAtT = accuracyAtT; }

        public Double getConfidenceAtT() { return confidenceAtT; }
        public void setConfidenceAtT(Double confidenceAtT) { this.confidenceAtT = confidenceAtT; }

        public String getActivityType() { return activityType; }
        public void setActivityType(String activityType) { this.activityType = activityType; }

        public Double getTargetKnowledgeAtNextT() { return targetKnowledgeAtNextT; }
        public void setTargetKnowledgeAtNextT(Double targetKnowledgeAtNextT) { this.targetKnowledgeAtNextT = targetKnowledgeAtNextT; }
    }
}
