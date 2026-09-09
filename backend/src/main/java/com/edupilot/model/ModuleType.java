package com.edupilot.model;

/**
 * Explicit Module Identity enum for Quiz & Assessment isolation.
 * Used to isolate sessions, questions, and mastery updates across EduPilot modules.
 */
public enum ModuleType {
    DIAGNOSTIC,   // Onboarding Form D — Diagnostic Assessment
    ADAPTIVE,     // Adaptive Quiz
    PRACTICE,     // Standard Practice Quiz
    REMEDIATION,  // Concept Remediation / Verification Test
    BASELINE      // Subject Baseline Knowledge Test
}
