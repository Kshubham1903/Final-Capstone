package com.edupilot.dto;

import java.util.List;

public class AdaptiveAssessmentDTOs {

    public static class AdaptiveStartRequest {
        private String diagnosticSessionId;
        private String subjectCode;
        private String subjectName;
        private String userId;

        public AdaptiveStartRequest() {}

        public AdaptiveStartRequest(String diagnosticSessionId, String subjectCode, String userId) {
            this.diagnosticSessionId = diagnosticSessionId;
            this.subjectCode = subjectCode;
            this.userId = userId;
        }

        public AdaptiveStartRequest(String diagnosticSessionId, String subjectCode, String subjectName, String userId) {
            this.diagnosticSessionId = diagnosticSessionId;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.userId = userId;
        }

        public String getDiagnosticSessionId() { return diagnosticSessionId; }
        public void setDiagnosticSessionId(String diagnosticSessionId) { this.diagnosticSessionId = diagnosticSessionId; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class AdaptiveStartResponse {
        private String adaptiveSessionId;
        private String subjectCode;
        private List<String> targetConcepts;
        private int maxQuestions;
        private int totalTargetConcepts;
        private boolean completed;

        public AdaptiveStartResponse() {}

        public AdaptiveStartResponse(String adaptiveSessionId, String subjectCode, List<String> targetConcepts, int maxQuestions, int totalTargetConcepts, boolean completed) {
            this.adaptiveSessionId = adaptiveSessionId;
            this.subjectCode = subjectCode;
            this.targetConcepts = targetConcepts;
            this.maxQuestions = maxQuestions;
            this.totalTargetConcepts = totalTargetConcepts;
            this.completed = completed;
        }

        public String getAdaptiveSessionId() { return adaptiveSessionId; }
        public void setAdaptiveSessionId(String adaptiveSessionId) { this.adaptiveSessionId = adaptiveSessionId; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public List<String> getTargetConcepts() { return targetConcepts; }
        public void setTargetConcepts(List<String> targetConcepts) { this.targetConcepts = targetConcepts; }
        public int getMaxQuestions() { return maxQuestions; }
        public void setMaxQuestions(int maxQuestions) { this.maxQuestions = maxQuestions; }
        public int getTotalTargetConcepts() { return totalTargetConcepts; }
        public void setTotalTargetConcepts(int totalTargetConcepts) { this.totalTargetConcepts = totalTargetConcepts; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
    }

    public static class AdaptiveNextRequest {
        private String adaptiveSessionId;

        public AdaptiveNextRequest() {}
        public AdaptiveNextRequest(String adaptiveSessionId) { this.adaptiveSessionId = adaptiveSessionId; }
        public String getAdaptiveSessionId() { return adaptiveSessionId; }
        public void setAdaptiveSessionId(String adaptiveSessionId) { this.adaptiveSessionId = adaptiveSessionId; }
    }

    public static class QuestionItemDTO {
        private String questionId;
        private String subject;
        private String concept;
        private String difficulty;
        private String questionText;
        private List<String> options;
        private Integer correctOptionIndex;
        private String conceptualExplanation;

        public QuestionItemDTO() {}

        public QuestionItemDTO(String questionId, String subject, String concept, String difficulty, String questionText, List<String> options) {
            this.questionId = questionId;
            this.subject = subject;
            this.concept = concept;
            this.difficulty = difficulty;
            this.questionText = questionText;
            this.options = options;
        }

        public QuestionItemDTO(String questionId, String subject, String concept, String difficulty, String questionText, List<String> options, Integer correctOptionIndex, String conceptualExplanation) {
            this.questionId = questionId;
            this.subject = subject;
            this.concept = concept;
            this.difficulty = difficulty;
            this.questionText = questionText;
            this.options = options;
            this.correctOptionIndex = correctOptionIndex;
            this.conceptualExplanation = conceptualExplanation;
        }

        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getConcept() { return concept; }
        public void setConcept(String concept) { this.concept = concept; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public String getQuestionText() { return questionText; }
        public void setQuestionText(String questionText) { this.questionText = questionText; }
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        public Integer getCorrectOptionIndex() { return correctOptionIndex; }
        public void setCorrectOptionIndex(Integer correctOptionIndex) { this.correctOptionIndex = correctOptionIndex; }
        public String getConceptualExplanation() { return conceptualExplanation; }
        public void setConceptualExplanation(String conceptualExplanation) { this.conceptualExplanation = conceptualExplanation; }
    }

    public static class AdaptiveNextResponse {
        private String adaptiveSessionId;
        private boolean completed;
        private QuestionItemDTO question;
        private int questionNumber;
        private int maxQuestions;
        private String currentConcept;
        private String targetDifficulty;

        public AdaptiveNextResponse() {}

        public AdaptiveNextResponse(String adaptiveSessionId, boolean completed, QuestionItemDTO question, int questionNumber, int maxQuestions, String currentConcept, String targetDifficulty) {
            this.adaptiveSessionId = adaptiveSessionId;
            this.completed = completed;
            this.question = question;
            this.questionNumber = questionNumber;
            this.maxQuestions = maxQuestions;
            this.currentConcept = currentConcept;
            this.targetDifficulty = targetDifficulty;
        }

        public String getAdaptiveSessionId() { return adaptiveSessionId; }
        public void setAdaptiveSessionId(String adaptiveSessionId) { this.adaptiveSessionId = adaptiveSessionId; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public QuestionItemDTO getQuestion() { return question; }
        public void setQuestion(QuestionItemDTO question) { this.question = question; }
        public int getQuestionNumber() { return questionNumber; }
        public void setQuestionNumber(int questionNumber) { this.questionNumber = questionNumber; }
        public int getMaxQuestions() { return maxQuestions; }
        public void setMaxQuestions(int maxQuestions) { this.maxQuestions = maxQuestions; }
        public String getCurrentConcept() { return currentConcept; }
        public void setCurrentConcept(String currentConcept) { this.currentConcept = currentConcept; }
        public String getTargetDifficulty() { return targetDifficulty; }
        public void setTargetDifficulty(String targetDifficulty) { this.targetDifficulty = targetDifficulty; }
    }

    public static class AdaptiveSubmitRequest {
        private String adaptiveSessionId;
        private String questionId;
        private int selectedOption;
        private double responseTimeSeconds;

        public AdaptiveSubmitRequest() {}

        public AdaptiveSubmitRequest(String adaptiveSessionId, String questionId, int selectedOption, double responseTimeSeconds) {
            this.adaptiveSessionId = adaptiveSessionId;
            this.questionId = questionId;
            this.selectedOption = selectedOption;
            this.responseTimeSeconds = responseTimeSeconds;
        }

        public String getAdaptiveSessionId() { return adaptiveSessionId; }
        public void setAdaptiveSessionId(String adaptiveSessionId) { this.adaptiveSessionId = adaptiveSessionId; }
        public String getQuestionId() { return questionId; }
        public void setQuestionId(String questionId) { this.questionId = questionId; }
        public int getSelectedOption() { return selectedOption; }
        public void setSelectedOption(int selectedOption) { this.selectedOption = selectedOption; }
        public double getResponseTimeSeconds() { return responseTimeSeconds; }
        public void setResponseTimeSeconds(double responseTimeSeconds) { this.responseTimeSeconds = responseTimeSeconds; }
    }

    public static class AdaptiveSubmitResponse {
        private String adaptiveSessionId;
        private boolean isCorrect;
        private Integer correctOptionIndex;
        private String explanation;
        private boolean completed;
        private String updatedConceptStatus;
        private double updatedConceptConfidence;
        private String nextDifficulty;

        public AdaptiveSubmitResponse() {}

        public AdaptiveSubmitResponse(String adaptiveSessionId, boolean isCorrect, String explanation, boolean completed, String updatedConceptStatus, double updatedConceptConfidence, String nextDifficulty) {
            this.adaptiveSessionId = adaptiveSessionId;
            this.isCorrect = isCorrect;
            this.explanation = explanation;
            this.completed = completed;
            this.updatedConceptStatus = updatedConceptStatus;
            this.updatedConceptConfidence = updatedConceptConfidence;
            this.nextDifficulty = nextDifficulty;
        }

        public AdaptiveSubmitResponse(String adaptiveSessionId, boolean isCorrect, Integer correctOptionIndex, String explanation, boolean completed, String updatedConceptStatus, double updatedConceptConfidence, String nextDifficulty) {
            this.adaptiveSessionId = adaptiveSessionId;
            this.isCorrect = isCorrect;
            this.correctOptionIndex = correctOptionIndex;
            this.explanation = explanation;
            this.completed = completed;
            this.updatedConceptStatus = updatedConceptStatus;
            this.updatedConceptConfidence = updatedConceptConfidence;
            this.nextDifficulty = nextDifficulty;
        }

        public String getAdaptiveSessionId() { return adaptiveSessionId; }
        public void setAdaptiveSessionId(String adaptiveSessionId) { this.adaptiveSessionId = adaptiveSessionId; }
        public boolean isIsCorrect() { return isCorrect; }
        public void setIsCorrect(boolean isCorrect) { this.isCorrect = isCorrect; }
        public Integer getCorrectOptionIndex() { return correctOptionIndex; }
        public void setCorrectOptionIndex(Integer correctOptionIndex) { this.correctOptionIndex = correctOptionIndex; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
        public boolean isCompleted() { return completed; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public String getUpdatedConceptStatus() { return updatedConceptStatus; }
        public void setUpdatedConceptStatus(String updatedConceptStatus) { this.updatedConceptStatus = updatedConceptStatus; }
        public double getUpdatedConceptConfidence() { return updatedConceptConfidence; }
        public void setUpdatedConceptConfidence(double updatedConceptConfidence) { this.updatedConceptConfidence = updatedConceptConfidence; }
        public String getNextDifficulty() { return nextDifficulty; }
        public void setNextDifficulty(String nextDifficulty) { this.nextDifficulty = nextDifficulty; }
    }
}
