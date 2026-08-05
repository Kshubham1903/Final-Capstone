package com.edupilot.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LearningMode {
    LEARN("LEARN"),
    PRACTICE("PRACTICE"),
    REVISION("REVISION"),
    EXPLAIN_MISTAKES("EXPLAIN_MISTAKES"),
    INTERVIEW("INTERVIEW"),
    CODING("CODING");

    private final String value;

    LearningMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static LearningMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return LEARN;
        }
        for (LearningMode mode : LearningMode.values()) {
            if (mode.name().equalsIgnoreCase(value.trim()) || mode.getValue().equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        return LEARN;
    }
}
