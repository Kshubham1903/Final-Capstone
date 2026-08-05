package com.edupilot.dto;

public class StudySessionEndRequest {
    private String sessionId;
    private int actualDurationMinutes;
    private int pausedDurationMinutes;
    private String completionNotes;

    public StudySessionEndRequest() {
    }

    public StudySessionEndRequest(String sessionId, int actualDurationMinutes, int pausedDurationMinutes, String completionNotes) {
        this.sessionId = sessionId;
        this.actualDurationMinutes = actualDurationMinutes;
        this.pausedDurationMinutes = pausedDurationMinutes;
        this.completionNotes = completionNotes;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getActualDurationMinutes() {
        return actualDurationMinutes;
    }

    public void setActualDurationMinutes(int actualDurationMinutes) {
        this.actualDurationMinutes = actualDurationMinutes;
    }

    public int getPausedDurationMinutes() {
        return pausedDurationMinutes;
    }

    public void setPausedDurationMinutes(int pausedDurationMinutes) {
        this.pausedDurationMinutes = pausedDurationMinutes;
    }

    public String getCompletionNotes() {
        return completionNotes;
    }

    public void setCompletionNotes(String completionNotes) {
        this.completionNotes = completionNotes;
    }
}
