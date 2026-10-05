package com.edupilot.dto;

import java.util.List;
import java.util.Map;

public class AdminCohortSubjectAnalyticsDTO {

    private int totalSubjectsCount;
    private int totalEnrolledStudents;
    private int evaluatedStudentsWithBaseline;
    private String dataSufficiencyNote;
    private List<SubjectResearchSummaryDTO> subjects;

    public static class SubjectResearchSummaryDTO {
        private String subjectCode;
        private String subjectName;
        private int studentsRepresented;
        private int studentsWithObservedKnowledge;
        private int studentsWithAuthenticBaseline;

        // Knowledge & Growth Metrics
        private Double meanBaselineKnowledge;  // K0 (0.0 - 100.0)
        private Double meanCurrentKnowledge;   // Kt (0.0 - 100.0)
        private Double meanGrowthPp;           // Kt - K0 (percentage points)
        private Double meanNormalizedGain;     // Hake normalized gain (0.0 - 1.0)

        // Concept Mastery Breakdown
        private int conceptCount;
        private int weakConceptCount;
        private Map<String, Integer> masteryDistribution;

        // Quiz Analytics (Segregated metric)
        private int quizSessionsCount;
        private int totalQuizQuestions;
        private int correctQuizAnswers;
        private Double meanQuizAccuracy;

        // Roadmap Analytics (Segregated metric)
        private int studentsWithRoadmap;
        private int totalRoadmapTopics;
        private int completedRoadmapTopics;
        private Double roadmapCompletionPercentage;

        public SubjectResearchSummaryDTO() {
        }

        public SubjectResearchSummaryDTO(String subjectCode, String subjectName,
                                         int studentsRepresented, int studentsWithObservedKnowledge,
                                         int studentsWithAuthenticBaseline,
                                         Double meanBaselineKnowledge, Double meanCurrentKnowledge,
                                         Double meanGrowthPp, Double meanNormalizedGain,
                                         int conceptCount, int weakConceptCount,
                                         Map<String, Integer> masteryDistribution,
                                         int quizSessionsCount, int totalQuizQuestions,
                                         int correctQuizAnswers, Double meanQuizAccuracy,
                                         int studentsWithRoadmap, int totalRoadmapTopics,
                                         int completedRoadmapTopics, Double roadmapCompletionPercentage) {
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.studentsRepresented = studentsRepresented;
            this.studentsWithObservedKnowledge = studentsWithObservedKnowledge;
            this.studentsWithAuthenticBaseline = studentsWithAuthenticBaseline;
            this.meanBaselineKnowledge = meanBaselineKnowledge;
            this.meanCurrentKnowledge = meanCurrentKnowledge;
            this.meanGrowthPp = meanGrowthPp;
            this.meanNormalizedGain = meanNormalizedGain;
            this.conceptCount = conceptCount;
            this.weakConceptCount = weakConceptCount;
            this.masteryDistribution = masteryDistribution;
            this.quizSessionsCount = quizSessionsCount;
            this.totalQuizQuestions = totalQuizQuestions;
            this.correctQuizAnswers = correctQuizAnswers;
            this.meanQuizAccuracy = meanQuizAccuracy;
            this.studentsWithRoadmap = studentsWithRoadmap;
            this.totalRoadmapTopics = totalRoadmapTopics;
            this.completedRoadmapTopics = completedRoadmapTopics;
            this.roadmapCompletionPercentage = roadmapCompletionPercentage;
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

        public int getStudentsRepresented() {
            return studentsRepresented;
        }

        public void setStudentsRepresented(int studentsRepresented) {
            this.studentsRepresented = studentsRepresented;
        }

        public int getStudentsWithObservedKnowledge() {
            return studentsWithObservedKnowledge;
        }

        public void setStudentsWithObservedKnowledge(int studentsWithObservedKnowledge) {
            this.studentsWithObservedKnowledge = studentsWithObservedKnowledge;
        }

        public int getStudentsWithAuthenticBaseline() {
            return studentsWithAuthenticBaseline;
        }

        public void setStudentsWithAuthenticBaseline(int studentsWithAuthenticBaseline) {
            this.studentsWithAuthenticBaseline = studentsWithAuthenticBaseline;
        }

        public Double getMeanBaselineKnowledge() {
            return meanBaselineKnowledge;
        }

        public void setMeanBaselineKnowledge(Double meanBaselineKnowledge) {
            this.meanBaselineKnowledge = meanBaselineKnowledge;
        }

        public Double getMeanCurrentKnowledge() {
            return meanCurrentKnowledge;
        }

        public void setMeanCurrentKnowledge(Double meanCurrentKnowledge) {
            this.meanCurrentKnowledge = meanCurrentKnowledge;
        }

        public Double getMeanGrowthPp() {
            return meanGrowthPp;
        }

        public void setMeanGrowthPp(Double meanGrowthPp) {
            this.meanGrowthPp = meanGrowthPp;
        }

        public Double getMeanNormalizedGain() {
            return meanNormalizedGain;
        }

        public void setMeanNormalizedGain(Double meanNormalizedGain) {
            this.meanNormalizedGain = meanNormalizedGain;
        }

        public int getConceptCount() {
            return conceptCount;
        }

        public void setConceptCount(int conceptCount) {
            this.conceptCount = conceptCount;
        }

        public int getWeakConceptCount() {
            return weakConceptCount;
        }

        public void setWeakConceptCount(int weakConceptCount) {
            this.weakConceptCount = weakConceptCount;
        }

        public Map<String, Integer> getMasteryDistribution() {
            return masteryDistribution;
        }

        public void setMasteryDistribution(Map<String, Integer> masteryDistribution) {
            this.masteryDistribution = masteryDistribution;
        }

        public int getQuizSessionsCount() {
            return quizSessionsCount;
        }

        public void setQuizSessionsCount(int quizSessionsCount) {
            this.quizSessionsCount = quizSessionsCount;
        }

        public int getTotalQuizQuestions() {
            return totalQuizQuestions;
        }

        public void setTotalQuizQuestions(int totalQuizQuestions) {
            this.totalQuizQuestions = totalQuizQuestions;
        }

        public int getCorrectQuizAnswers() {
            return correctQuizAnswers;
        }

        public void setCorrectQuizAnswers(int correctQuizAnswers) {
            this.correctQuizAnswers = correctQuizAnswers;
        }

        public Double getMeanQuizAccuracy() {
            return meanQuizAccuracy;
        }

        public void setMeanQuizAccuracy(Double meanQuizAccuracy) {
            this.meanQuizAccuracy = meanQuizAccuracy;
        }

        public int getStudentsWithRoadmap() {
            return studentsWithRoadmap;
        }

        public void setStudentsWithRoadmap(int studentsWithRoadmap) {
            this.studentsWithRoadmap = studentsWithRoadmap;
        }

        public int getTotalRoadmapTopics() {
            return totalRoadmapTopics;
        }

        public void setTotalRoadmapTopics(int totalRoadmapTopics) {
            this.totalRoadmapTopics = totalRoadmapTopics;
        }

        public int getCompletedRoadmapTopics() {
            return completedRoadmapTopics;
        }

        public void setCompletedRoadmapTopics(int completedRoadmapTopics) {
            this.completedRoadmapTopics = completedRoadmapTopics;
        }

        public Double getRoadmapCompletionPercentage() {
            return roadmapCompletionPercentage;
        }

        public void setRoadmapCompletionPercentage(Double roadmapCompletionPercentage) {
            this.roadmapCompletionPercentage = roadmapCompletionPercentage;
        }
    }

    public AdminCohortSubjectAnalyticsDTO() {
    }

    public AdminCohortSubjectAnalyticsDTO(int totalSubjectsCount, int totalEnrolledStudents,
                                          int evaluatedStudentsWithBaseline,
                                          String dataSufficiencyNote,
                                          List<SubjectResearchSummaryDTO> subjects) {
        this.totalSubjectsCount = totalSubjectsCount;
        this.totalEnrolledStudents = totalEnrolledStudents;
        this.evaluatedStudentsWithBaseline = evaluatedStudentsWithBaseline;
        this.dataSufficiencyNote = dataSufficiencyNote;
        this.subjects = subjects;
    }

    public int getTotalSubjectsCount() {
        return totalSubjectsCount;
    }

    public void setTotalSubjectsCount(int totalSubjectsCount) {
        this.totalSubjectsCount = totalSubjectsCount;
    }

    public int getTotalEnrolledStudents() {
        return totalEnrolledStudents;
    }

    public void setTotalEnrolledStudents(int totalEnrolledStudents) {
        this.totalEnrolledStudents = totalEnrolledStudents;
    }

    public int getEvaluatedStudentsWithBaseline() {
        return evaluatedStudentsWithBaseline;
    }

    public void setEvaluatedStudentsWithBaseline(int evaluatedStudentsWithBaseline) {
        this.evaluatedStudentsWithBaseline = evaluatedStudentsWithBaseline;
    }

    public String getDataSufficiencyNote() {
        return dataSufficiencyNote;
    }

    public void setDataSufficiencyNote(String dataSufficiencyNote) {
        this.dataSufficiencyNote = dataSufficiencyNote;
    }

    public List<SubjectResearchSummaryDTO> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<SubjectResearchSummaryDTO> subjects) {
        this.subjects = subjects;
    }
}
