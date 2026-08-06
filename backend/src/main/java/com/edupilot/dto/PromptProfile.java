package com.edupilot.dto;

import com.edupilot.model.LearningMode;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory Prompt Profile representing the dynamically determined configuration
 * for AI prompt generation.
 *
 * <p>This object is NOT persisted in MongoDB. It is constructed on-the-fly during
 * the prompt orchestration pipeline:
 * <pre>
 * Learning Mode → Intent Detection → Student Mastery Analysis → Response Length → Output Profile → Prompt Builder
 * </pre>
 */
public class PromptProfile {

    private LearningMode learningMode;
    private String intent;
    private String masteryLevel;      // BEGINNER, INTERMEDIATE, MASTER
    private String responseLength;    // SHORT, MEDIUM, LONG
    private String outputStyle;
    private boolean includeCode;
    private boolean includeComplexity;
    private boolean includeExample;
    private boolean includeAnalogy;
    private boolean includeDiagram;
    private boolean includeInterviewTips;
    private boolean includeRevisionNotes;
    private String markdownStyle;
    private List<String> sectionList = new ArrayList<>();
    private boolean isContinuation;

    public PromptProfile() {
    }

    public LearningMode getLearningMode() {
        return learningMode;
    }

    public void setLearningMode(LearningMode learningMode) {
        this.learningMode = learningMode;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getMasteryLevel() {
        return masteryLevel;
    }

    public void setMasteryLevel(String masteryLevel) {
        this.masteryLevel = masteryLevel;
    }

    public String getResponseLength() {
        return responseLength;
    }

    public void setResponseLength(String responseLength) {
        this.responseLength = responseLength;
    }

    public String getOutputStyle() {
        return outputStyle;
    }

    public void setOutputStyle(String outputStyle) {
        this.outputStyle = outputStyle;
    }

    public boolean isIncludeCode() {
        return includeCode;
    }

    public void setIncludeCode(boolean includeCode) {
        this.includeCode = includeCode;
    }

    public boolean isIncludeComplexity() {
        return includeComplexity;
    }

    public void setIncludeComplexity(boolean includeComplexity) {
        this.includeComplexity = includeComplexity;
    }

    public boolean isIncludeExample() {
        return includeExample;
    }

    public void setIncludeExample(boolean includeExample) {
        this.includeExample = includeExample;
    }

    public boolean isIncludeAnalogy() {
        return includeAnalogy;
    }

    public void setIncludeAnalogy(boolean includeAnalogy) {
        this.includeAnalogy = includeAnalogy;
    }

    public boolean isIncludeDiagram() {
        return includeDiagram;
    }

    public void setIncludeDiagram(boolean includeDiagram) {
        this.includeDiagram = includeDiagram;
    }

    public boolean isIncludeInterviewTips() {
        return includeInterviewTips;
    }

    public void setIncludeInterviewTips(boolean includeInterviewTips) {
        this.includeInterviewTips = includeInterviewTips;
    }

    public boolean isIncludeRevisionNotes() {
        return includeRevisionNotes;
    }

    public void setIncludeRevisionNotes(boolean includeRevisionNotes) {
        this.includeRevisionNotes = includeRevisionNotes;
    }

    public String getMarkdownStyle() {
        return markdownStyle;
    }

    public void setMarkdownStyle(String markdownStyle) {
        this.markdownStyle = markdownStyle;
    }

    public List<String> getSectionList() {
        return sectionList;
    }

    public void setSectionList(List<String> sectionList) {
        this.sectionList = sectionList;
    }

    public boolean isContinuation() {
        return isContinuation;
    }

    public void setContinuation(boolean continuation) {
        isContinuation = continuation;
    }
}
