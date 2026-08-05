package com.edupilot.service;

import com.edupilot.dto.StudentContextDTO;
import com.edupilot.model.LearningMode;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PromptBuilderService {

    /**
     * Constructs a structured multi-section system prompt incorporating complete Student Context and Active Learning Mode.
     */
    public String buildStructuredSystemPrompt(StudentContextDTO context, LearningMode mode) {
        StringBuilder sb = new StringBuilder();
        LearningMode activeMode = mode != null ? mode : LearningMode.LEARN;

        // 1. System Role & Anti-Repetitive Directives
        sb.append("[SYSTEM ROLE]\n");
        sb.append("You are EduPilot AI Tutor, an intelligent, highly personalized academic mentor.\n");
        sb.append("You adapt explanations strictly according to the student's academic standing, weak concepts, and active Learning Mode.\n\n");
        sb.append("STRICT OUTPUT DIRECTIVES:\n");
        sb.append("- NEVER start responses with generic intros like 'Hi Student', 'Hello [Name]', 'Regarding your question about...', or 'Sure, I can help with that'.\n");
        sb.append("- Jump DIRECTLY into the core explanation, code, question, or analysis.\n");
        sb.append("- DO NOT ask cold setup questions (e.g., 'What branch are you in?') because student context is already provided below.\n");
        sb.append("- Use clean, professional Markdown formatting with headers, bullet points, code blocks, tables, mathematical notation ($...$ and $$...$$), blockquotes, and bold emphasis.\n");
        sb.append("- AUTOMATIC TOPIC EXPANSION & MANDATORY SECTIONS:\n");
        sb.append("  For technical & educational concepts, structure your explanation into comprehensive sections unless explicitly asked to be brief:\n");
        sb.append("  1. Formal Definition & Core Intuition\n");
        sb.append("  2. Real-World Analogy & ASCII Structural Diagram / Flowchart\n");
        sb.append("  3. Step-by-Step Algorithm & Mechanism Walkthrough\n");
        sb.append("  4. Production-Ready, Complete, Syntax-Highlighted Code (Java/Python/C++) with inline comments. NEVER truncate code or use placeholders like '// TODO' or '...'\n");
        sb.append("  5. Step-by-Step Dry Run Execution Trace\n");
        sb.append("  6. Time Complexity & Space Complexity Analysis (Big-O Summary Table)\n");
        sb.append("  7. Edge Cases & Common Misconceptions / Pitfalls\n");
        sb.append("  8. High-Yield Technical Interview Questions & Practice Problems\n");
        sb.append("  9. Key Summary Takeaways\n\n");

        // 2. Active Learning Mode Instructions
        sb.append("[ACTIVE LEARNING MODE: ").append(activeMode.name()).append("]\n");
        switch (activeMode) {
            case LEARN -> {
                sb.append("- OBJECTIVE: Comprehensive conceptual breakdown & intuition building.\n");
                sb.append("- INSTRUCTIONS: Explain step-by-step using clear real-world analogies. Break complex mechanisms into intuitive sub-components. Conclude with a key summary takeaway.\n");
            }
            case PRACTICE -> {
                sb.append("- OBJECTIVE: Interactive problem-solving drill.\n");
                sb.append("- INSTRUCTIONS: DO NOT give direct solution answers immediately. Present 1 targeted practice problem (conceptual or quantitative). Ask the student to attempt it, offering subtle guidance.\n");
            }
            case REVISION -> {
                sb.append("- OBJECTIVE: Rapid high-yield exam revision & memory consolidation.\n");
                sb.append("- INSTRUCTIONS: Provide high-density cheat-sheet bullet points, formulas, balance factors, time/space complexities, and common exam traps. Keep explanation text minimal.\n");
            }
            case EXPLAIN_MISTAKES -> {
                sb.append("- OBJECTIVE: Diagnostic mistake remediation & error analysis.\n");
                sb.append("- INSTRUCTIONS: Address common conceptual pitfalls and misconceptions related to the topic. Explain WHY incorrect choices seem plausible and HOW to systematically avoid them.\n");
            }
            case INTERVIEW -> {
                sb.append("- OBJECTIVE: Technical Coding & System Architecture Mock Interview.\n");
                sb.append("- INSTRUCTIONS: Act as a Principal Technical Interviewer. Present 1 technical interview question. Evaluate student responses for algorithmic efficiency, edge cases, and design trade-offs.\n");
            }
            case CODING -> {
                sb.append("- OBJECTIVE: Production-quality code implementation & algorithm analysis.\n");
                sb.append("- INSTRUCTIONS: Provide algorithm walkthrough, Time/Space Complexity (Big-O analysis), and complete, clean syntax-highlighted code blocks (Java/Python/C++) with inline comments.\n");
            }
        }
        sb.append("\n");

        if (context != null) {
            // 3. Student Identity & Academic Context
            sb.append("[STUDENT PROFILE]\n");
            sb.append("- Student Name: ").append(context.getStudentName()).append("\n");
            sb.append("- Degree & Branch: ").append(context.getDegree()).append(" in ").append(context.getBranch()).append("\n");
            sb.append("- Current Semester: Semester ").append(context.getSemester()).append("\n");
            sb.append("- Academic Standing: CGPA ").append(context.getCurrentCgpa()).append(" / Target CGPA ").append(context.getTargetCgpa()).append("\n");
            sb.append("- Student Growth Index (SGI): ").append(context.getSgi()).append(" / 10.0 | Academic Risk Level: ").append(context.getRiskLevel()).append("\n\n");

            // 4. Knowledge Matrix
            sb.append("[KNOWLEDGE MATRIX]\n");
            sb.append("- Overall Learning Health Score: ").append(context.getLearningHealthScore()).append("%\n");
            sb.append("- Mastered Concepts: ").append(context.getStrongConcepts()).append("\n");
            sb.append("- Weak Concepts Needing Review: ").append(context.getWeakConcepts()).append("\n\n");

            // 5. Priority Recommendations
            if (context.getActiveRecommendations() != null && !context.getActiveRecommendations().isEmpty()) {
                sb.append("[RECOMMENDATIONS]\n");
                for (String rec : context.getActiveRecommendations()) {
                    sb.append("- ").append(rec).append("\n");
                }
                sb.append("\n");
            }

            // 6. Today's Learning Plan
            sb.append("[TODAY'S PLAN]\n");
            sb.append("- Active Subject: ").append(context.getActiveSubject()).append("\n");
            sb.append("- Scheduled Focus Task: ").append(context.getTodayFocusTask()).append("\n\n");

            // 7. Conversation Transcript Memory
            if (context.getConversationSummary() != null && !context.getConversationSummary().isBlank()) {
                sb.append("[CONVERSATION MEMORY & TRANSCRIPT]\n");
                sb.append(context.getConversationSummary()).append("\n");
            }
        }

        return sb.toString();
    }

    public String buildStructuredSystemPrompt(StudentContextDTO context) {
        return buildStructuredSystemPrompt(context, LearningMode.LEARN);
    }

    public String buildSystemPrompt(Map<String, Object> contextMap) {
        StudentContextDTO dto = new StudentContextDTO();
        if (contextMap != null) {
            if (contextMap.containsKey("studentName")) dto.setStudentName((String) contextMap.get("studentName"));
            if (contextMap.containsKey("referencedConcept")) dto.setTodayFocusTask((String) contextMap.get("referencedConcept"));
        }
        return buildStructuredSystemPrompt(dto, LearningMode.LEARN);
    }
}
