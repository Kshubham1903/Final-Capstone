package com.edupilot.exception;

/**
 * Exception thrown when an AI-driven operation (e.g. diagnostic question generation)
 * cannot proceed because no usable personal or system Groq API key is available.
 */
public class GroqKeyRequiredException extends RuntimeException {

    public GroqKeyRequiredException(String message) {
        super(message);
    }

    public GroqKeyRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
