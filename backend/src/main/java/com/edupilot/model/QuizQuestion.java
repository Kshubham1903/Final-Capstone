package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "quiz_questions")
public class QuizQuestion {
    @Id
    private String id;
    private String subject;
    private String concept;
    private Difficulty difficulty; // EASY, MEDIUM, HARD
    private String questionText;
    private List<String> options;
    private int correctOptionIndex;
    private String conceptualExplanation;

    private int generationVersion = 2;
    private String questionSource = "DYNAMIC_V2";
    private String conceptId;
    private String templateFamilyId;
    private String questionFingerprint;
    private boolean qualityValidated = true;
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }

    public QuizQuestion() {
    }

    public QuizQuestion(String id, String subject, String concept, Difficulty difficulty, String questionText, List<String> options, int correctOptionIndex, String conceptualExplanation) {
        this.id = id;
        this.subject = subject;
        this.concept = concept;
        this.difficulty = difficulty;
        this.questionText = questionText;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.conceptualExplanation = conceptualExplanation;
    }

    public int getGenerationVersion() {
        return generationVersion;
    }

    public void setGenerationVersion(int generationVersion) {
        this.generationVersion = generationVersion;
    }

    public String getQuestionSource() {
        return questionSource;
    }

    public void setQuestionSource(String questionSource) {
        this.questionSource = questionSource;
    }

    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    public String getTemplateFamilyId() {
        return templateFamilyId;
    }

    public void setTemplateFamilyId(String templateFamilyId) {
        this.templateFamilyId = templateFamilyId;
    }

    public String getQuestionFingerprint() {
        return questionFingerprint;
    }

    public void setQuestionFingerprint(String questionFingerprint) {
        this.questionFingerprint = questionFingerprint;
    }

    public boolean isQualityValidated() {
        return qualityValidated;
    }

    public void setQualityValidated(boolean qualityValidated) {
        this.qualityValidated = qualityValidated;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public void setCorrectOptionIndex(int correctOptionIndex) {
        this.correctOptionIndex = correctOptionIndex;
    }

    public String getConceptualExplanation() {
        return conceptualExplanation;
    }

    public void setConceptualExplanation(String conceptualExplanation) {
        this.conceptualExplanation = conceptualExplanation;
    }

    public static QuizQuestionBuilder builder() {
        return new QuizQuestionBuilder();
    }

    public static class QuizQuestionBuilder {
        private String id;
        private String subject;
        private String concept;
        private Difficulty difficulty;
        private String questionText;
        private List<String> options;
        private int correctOptionIndex;
        private String conceptualExplanation;

        public QuizQuestionBuilder id(String id) {
            this.id = id;
            return this;
        }

        public QuizQuestionBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public QuizQuestionBuilder concept(String concept) {
            this.concept = concept;
            return this;
        }

        public QuizQuestionBuilder difficulty(Difficulty difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public QuizQuestionBuilder questionText(String questionText) {
            this.questionText = questionText;
            return this;
        }

        public QuizQuestionBuilder options(List<String> options) {
            this.options = options;
            return this;
        }

        public QuizQuestionBuilder correctOptionIndex(int correctOptionIndex) {
            this.correctOptionIndex = correctOptionIndex;
            return this;
        }

        public QuizQuestionBuilder conceptualExplanation(String conceptualExplanation) {
            this.conceptualExplanation = conceptualExplanation;
            return this;
        }

        public QuizQuestion build() {
            return new QuizQuestion(id, subject, concept, difficulty, questionText, options, correctOptionIndex, conceptualExplanation);
        }
    }
}

