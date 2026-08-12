package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "quiz_sessions")
public class QuizSession {
    @Id
    private String id;
    private String userId;
    private String studentProfileId;
    private String subjectCode;
    private String subjectName;
    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime lastAnswerTime = LocalDateTime.now();
    private Status status = Status.IN_PROGRESS;
    private boolean isVerificationQuiz = false;
    private String targetConcept;

    private int totalQuestions = 0;
    private int correctCount = 0;
    private int incorrectCount = 0;

    private List<QuizAnswerRecord> answers = new ArrayList<>();

    public enum Status {
        IN_PROGRESS,
        COMPLETED
    }

    public static class QuizAnswerRecord {
        private String questionId;
        private String questionText;
        private String concept;
        private String difficulty;
        private boolean correct;
        private double responseTimeSeconds;
        private LocalDateTime timestamp = LocalDateTime.now();

        public QuizAnswerRecord() {
        }

        public QuizAnswerRecord(String concept, String difficulty, boolean correct, double responseTimeSeconds) {
            this.concept = concept;
            this.difficulty = difficulty;
            this.correct = correct;
            this.responseTimeSeconds = responseTimeSeconds;
            this.timestamp = LocalDateTime.now();
        }

        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
        }

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public String getConcept() {
            return concept;
        }

        public void setConcept(String concept) {
            this.concept = concept;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }

        public boolean isCorrect() {
            return correct;
        }

        public void setCorrect(boolean correct) {
            this.correct = correct;
        }

        public double getResponseTimeSeconds() {
            return responseTimeSeconds;
        }

        public void setResponseTimeSeconds(double responseTimeSeconds) {
            this.responseTimeSeconds = responseTimeSeconds;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }

    public QuizSession() {
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

    public String getStudentProfileId() {
        return studentProfileId;
    }

    public void setStudentProfileId(String studentProfileId) {
        this.studentProfileId = studentProfileId;
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getLastAnswerTime() {
        return lastAnswerTime;
    }

    public void setLastAnswerTime(LocalDateTime lastAnswerTime) {
        this.lastAnswerTime = lastAnswerTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(int correctCount) {
        this.correctCount = correctCount;
    }

    public int getIncorrectCount() {
        return incorrectCount;
    }

    public void setIncorrectCount(int incorrectCount) {
        this.incorrectCount = incorrectCount;
    }

    public List<QuizAnswerRecord> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuizAnswerRecord> answers) {
        this.answers = answers;
    }

    public boolean isVerificationQuiz() {
        return isVerificationQuiz;
    }

    public void setVerificationQuiz(boolean verificationQuiz) {
        isVerificationQuiz = verificationQuiz;
    }

    public String getTargetConcept() {
        return targetConcept;
    }

    public void setTargetConcept(String targetConcept) {
        this.targetConcept = targetConcept;
    }
}
