package com.edupilot.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AdminResearchTrendsDTO {

    private int totalObservations;
    private int totalAssessments;
    private int totalQuizzes;
    private int totalSnapshots;
    private int uniqueStudentsCount;
    private int evaluatedStudentsWithBaseline;
    private LocalDateTime earliestObservationDate;
    private LocalDateTime latestObservationDate;
    private String dataSufficiencyNote;
    private List<TrendPointDTO> observations;

    public static class TrendPointDTO {
        private String date; // ISO date "YYYY-MM-DD"
        private LocalDateTime timestamp;
        
        // Primary Metric: Derived SOLELY from persisted AssessmentResult records
        private Double meanAssessmentScore; // 0.0 - 100.0 percentage scale
        private Double meanKnowledgeScore;  // Strict alias for meanAssessmentScore (strictly AssessmentResult)
        
        // Secondary Independent Series: Kept strictly separated
        private Double meanQuizAccuracy;           // 0.0 - 100.0 percentage scale from QuizSession only
        private Double meanSnapshotKnowledgeScore; // 0.0 - 100.0 percentage scale from StudentStateSnapshot only
        private Double meanEngagementScore;        // 0.0 - 100.0 percentage scale from StudentStateSnapshot only
        
        // Observation metadata
        private int observationCount;
        private int studentCount;
        private int assessmentCount;
        private int quizCount;
        private int snapshotCount;

        public TrendPointDTO() {
        }

        public TrendPointDTO(String date, LocalDateTime timestamp,
                             Double meanAssessmentScore,
                             Double meanQuizAccuracy,
                             Double meanSnapshotKnowledgeScore,
                             Double meanEngagementScore,
                             int observationCount,
                             int studentCount,
                             int assessmentCount,
                             int quizCount,
                             int snapshotCount) {
            this.date = date;
            this.timestamp = timestamp;
            this.meanAssessmentScore = meanAssessmentScore;
            this.meanKnowledgeScore = meanAssessmentScore; // Strict alias
            this.meanQuizAccuracy = meanQuizAccuracy;
            this.meanSnapshotKnowledgeScore = meanSnapshotKnowledgeScore;
            this.meanEngagementScore = meanEngagementScore;
            this.observationCount = observationCount;
            this.studentCount = studentCount;
            this.assessmentCount = assessmentCount;
            this.quizCount = quizCount;
            this.snapshotCount = snapshotCount;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public Double getMeanAssessmentScore() {
            return meanAssessmentScore;
        }

        public void setMeanAssessmentScore(Double meanAssessmentScore) {
            this.meanAssessmentScore = meanAssessmentScore;
            this.meanKnowledgeScore = meanAssessmentScore;
        }

        public Double getMeanKnowledgeScore() {
            return meanKnowledgeScore;
        }

        public void setMeanKnowledgeScore(Double meanKnowledgeScore) {
            this.meanKnowledgeScore = meanKnowledgeScore;
            this.meanAssessmentScore = meanKnowledgeScore;
        }

        public Double getMeanQuizAccuracy() {
            return meanQuizAccuracy;
        }

        public void setMeanQuizAccuracy(Double meanQuizAccuracy) {
            this.meanQuizAccuracy = meanQuizAccuracy;
        }

        public Double getMeanSnapshotKnowledgeScore() {
            return meanSnapshotKnowledgeScore;
        }

        public void setMeanSnapshotKnowledgeScore(Double meanSnapshotKnowledgeScore) {
            this.meanSnapshotKnowledgeScore = meanSnapshotKnowledgeScore;
        }

        public Double getMeanEngagementScore() {
            return meanEngagementScore;
        }

        public void setMeanEngagementScore(Double meanEngagementScore) {
            this.meanEngagementScore = meanEngagementScore;
        }

        public int getObservationCount() {
            return observationCount;
        }

        public void setObservationCount(int observationCount) {
            this.observationCount = observationCount;
        }

        public int getStudentCount() {
            return studentCount;
        }

        public void setStudentCount(int studentCount) {
            this.studentCount = studentCount;
        }

        public int getAssessmentCount() {
            return assessmentCount;
        }

        public void setAssessmentCount(int assessmentCount) {
            this.assessmentCount = assessmentCount;
        }

        public int getQuizCount() {
            return quizCount;
        }

        public void setQuizCount(int quizCount) {
            this.quizCount = quizCount;
        }

        public int getSnapshotCount() {
            return snapshotCount;
        }

        public void setSnapshotCount(int snapshotCount) {
            this.snapshotCount = snapshotCount;
        }
    }

    public AdminResearchTrendsDTO() {
    }

    public AdminResearchTrendsDTO(int totalObservations,
                                  int totalAssessments,
                                  int totalQuizzes,
                                  int totalSnapshots,
                                  int uniqueStudentsCount,
                                  int evaluatedStudentsWithBaseline,
                                  LocalDateTime earliestObservationDate,
                                  LocalDateTime latestObservationDate,
                                  String dataSufficiencyNote,
                                  List<TrendPointDTO> observations) {
        this.totalObservations = totalObservations;
        this.totalAssessments = totalAssessments;
        this.totalQuizzes = totalQuizzes;
        this.totalSnapshots = totalSnapshots;
        this.uniqueStudentsCount = uniqueStudentsCount;
        this.evaluatedStudentsWithBaseline = evaluatedStudentsWithBaseline;
        this.earliestObservationDate = earliestObservationDate;
        this.latestObservationDate = latestObservationDate;
        this.dataSufficiencyNote = dataSufficiencyNote;
        this.observations = observations;
    }

    public int getTotalObservations() {
        return totalObservations;
    }

    public void setTotalObservations(int totalObservations) {
        this.totalObservations = totalObservations;
    }

    public int getTotalAssessments() {
        return totalAssessments;
    }

    public void setTotalAssessments(int totalAssessments) {
        this.totalAssessments = totalAssessments;
    }

    public int getTotalQuizzes() {
        return totalQuizzes;
    }

    public void setTotalQuizzes(int totalQuizzes) {
        this.totalQuizzes = totalQuizzes;
    }

    public int getTotalSnapshots() {
        return totalSnapshots;
    }

    public void setTotalSnapshots(int totalSnapshots) {
        this.totalSnapshots = totalSnapshots;
    }

    public int getUniqueStudentsCount() {
        return uniqueStudentsCount;
    }

    public void setUniqueStudentsCount(int uniqueStudentsCount) {
        this.uniqueStudentsCount = uniqueStudentsCount;
    }

    public int getEvaluatedStudentsWithBaseline() {
        return evaluatedStudentsWithBaseline;
    }

    public void setEvaluatedStudentsWithBaseline(int evaluatedStudentsWithBaseline) {
        this.evaluatedStudentsWithBaseline = evaluatedStudentsWithBaseline;
    }

    public LocalDateTime getEarliestObservationDate() {
        return earliestObservationDate;
    }

    public void setEarliestObservationDate(LocalDateTime earliestObservationDate) {
        this.earliestObservationDate = earliestObservationDate;
    }

    public LocalDateTime getLatestObservationDate() {
        return latestObservationDate;
    }

    public void setLatestObservationDate(LocalDateTime latestObservationDate) {
        this.latestObservationDate = latestObservationDate;
    }

    public String getDataSufficiencyNote() {
        return dataSufficiencyNote;
    }

    public void setDataSufficiencyNote(String dataSufficiencyNote) {
        this.dataSufficiencyNote = dataSufficiencyNote;
    }

    public List<TrendPointDTO> getObservations() {
        return observations;
    }

    public void setObservations(List<TrendPointDTO> observations) {
        this.observations = observations;
    }
}
