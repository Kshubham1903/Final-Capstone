package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "adaptive_sessions")
public class AdaptiveSession {
    @Id
    private String id;
    private String diagnosticSessionId;
    private String userId;
    private String studentProfileId;
    private String subjectCode;
    private String subjectName;
    private List<String> targetConcepts = new ArrayList<>();
    private String currentConcept;
    private QuizQuestion.Difficulty currentDifficulty = QuizQuestion.Difficulty.MEDIUM;
    private String currentQuestionId;
    private boolean activeQuestionSubmitted = false;
    private int questionCount = 0;
    private int maxQuestions = 15;
    private List<String> usedQuestionIds = new ArrayList<>();
    private List<String> usedQuestionFingerprints = new ArrayList<>();
    private Status status = Status.IN_PROGRESS;
    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime endTime;

    public enum Status {
        IN_PROGRESS,
        COMPLETED
    }

    public AdaptiveSession() {}

    public AdaptiveSession(String id, String diagnosticSessionId, String userId, String studentProfileId, String subjectCode, String subjectName, List<String> targetConcepts) {
        this.id = id;
        this.diagnosticSessionId = diagnosticSessionId;
        this.userId = userId;
        this.studentProfileId = studentProfileId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.targetConcepts = targetConcepts != null ? targetConcepts : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDiagnosticSessionId() { return diagnosticSessionId; }
    public void setDiagnosticSessionId(String diagnosticSessionId) { this.diagnosticSessionId = diagnosticSessionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStudentProfileId() { return studentProfileId; }
    public void setStudentProfileId(String studentProfileId) { this.studentProfileId = studentProfileId; }
    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public List<String> getTargetConcepts() { return targetConcepts; }
    public void setTargetConcepts(List<String> targetConcepts) { this.targetConcepts = targetConcepts; }
    public String getCurrentConcept() { return currentConcept; }
    public void setCurrentConcept(String currentConcept) { this.currentConcept = currentConcept; }
    public QuizQuestion.Difficulty getCurrentDifficulty() { return currentDifficulty; }
    public void setCurrentDifficulty(QuizQuestion.Difficulty currentDifficulty) { this.currentDifficulty = currentDifficulty; }
    public String getCurrentQuestionId() { return currentQuestionId; }
    public void setCurrentQuestionId(String currentQuestionId) { this.currentQuestionId = currentQuestionId; }
    public boolean isActiveQuestionSubmitted() { return activeQuestionSubmitted; }
    public void setActiveQuestionSubmitted(boolean activeQuestionSubmitted) { this.activeQuestionSubmitted = activeQuestionSubmitted; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    public int getMaxQuestions() { return maxQuestions; }
    public void setMaxQuestions(int maxQuestions) { this.maxQuestions = maxQuestions; }
    public List<String> getUsedQuestionIds() { return usedQuestionIds; }
    public void setUsedQuestionIds(List<String> usedQuestionIds) { this.usedQuestionIds = usedQuestionIds; }
    public List<String> getUsedQuestionFingerprints() { return usedQuestionFingerprints; }
    public void setUsedQuestionFingerprints(List<String> usedQuestionFingerprints) { this.usedQuestionFingerprints = usedQuestionFingerprints; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
