package com.edupilot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "assessment_questions")
public class AssessmentQuestion {
    @Id
    private String id;
    private String institution = "EduPilot Academy";
    private String degree = "B.Tech";
    private String branch = "Computer Science & Engineering";
    private int semester = 3;
    private String subjectCode = "CS301";
    private String subjectName = "Data Structures & Algorithms";
    private String topic;
    private String questionText;
    private List<String> options;
    private int correctOptionIndex;
    private String explanation;
    private Difficulty difficulty = Difficulty.MEDIUM;
    private int marks = 2;
    private String questionType = "MCQ";
    private boolean isActive = true;

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD
    }

    public AssessmentQuestion() {
    }

    public AssessmentQuestion(String id, String institution, String degree, String branch, int semester, 
                              String subjectCode, String subjectName, String topic, String questionText, 
                              List<String> options, int correctOptionIndex, String explanation, 
                              Difficulty difficulty, int marks, String questionType, boolean isActive) {
        this.id = id;
        this.institution = institution;
        this.degree = degree;
        this.branch = branch;
        this.semester = semester;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.topic = topic;
        this.questionText = questionText;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.marks = marks;
        this.questionType = questionType;
        this.isActive = isActive;
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public int getMarks() {
        return marks;
    }

    public void setMarks(int marks) {
        this.marks = marks;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
