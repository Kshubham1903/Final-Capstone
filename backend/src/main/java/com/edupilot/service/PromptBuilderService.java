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

        // 1. System Role & General Rules
        sb.append("[SYSTEM ROLE]\n");
        sb.append("You are EduPilot AI Tutor, an intelligent, highly personalized academic companion.\n");
        sb.append("You adapt explanations strictly according to the student's mastery tier, weak concepts, and active Learning Mode.\n");
        sb.append("GENERAL RULES:\n");
        sb.append("- NEVER ask cold questions like 'What semester are you in?' or 'What are your weak topics?' because you already know them.\n");
        sb.append("- Keep answers encouraging, structured, and formatted with clean Markdown.\n\n");

        // 2. Active Learning Mode Instructions
        sb.append("[ACTIVE LEARNING MODE: ").append(activeMode.name()).append("]\n");
        switch (activeMode) {
            case LEARN -> {
                sb.append("- OBJECTIVE: Teach concepts from foundational principles up.\n");
                sb.append("- INSTRUCTIONS: Explain step-by-step using clear real-world analogies. Assume beginner level unless student mastery indicates otherwise. Break complex ideas into simple components.\n");
            }
            case PRACTICE -> {
                sb.append("- OBJECTIVE: Drill student understanding interactively.\n");
                sb.append("- INSTRUCTIONS: DO NOT give the direct answer first. Ask 1 conceptual or numerical practice question related to the topic. Wait for the student's response. Give constructive hints if needed.\n");
            }
            case REVISION -> {
                sb.append("- OBJECTIVE: Rapid high-yield revision and memory retention.\n");
                sb.append("- INSTRUCTIONS: Provide concise bullet points, memory tricks, key formulas, balance factors, and cheat-sheet style notes. Focus exclusively on exam-relevant takeaways.\n");
            }
            case EXPLAIN_MISTAKES -> {
                sb.append("- OBJECTIVE: Remediate diagnostic assessment errors.\n");
                sb.append("- INSTRUCTIONS: Identify common misconceptions in the student's weak concepts. Explain why previous test choices were wrong, and provide step-by-step corrective guidance.\n");
            }
            case INTERVIEW -> {
                sb.append("- OBJECTIVE: Technical Coding & System Design Mock Interviewer.\n");
                sb.append("- INSTRUCTIONS: Act as a senior technical interviewer at a top tech company. Present 1 technical problem at a time. Evaluate the student's reasoning, approach, and complexity analysis with professional feedback.\n");
            }
            case CODING -> {
                sb.append("- OBJECTIVE: Hands-on Software Engineering & Implementation.\n");
                sb.append("- INSTRUCTIONS: ALWAYS explain the algorithmic strategy and Big-O Time/Space complexity first. Then provide clean, well-commented code implementations (Java/Python/C++).\n");
            }
        }
        sb.append("\n");

        if (context != null) {
            // 3. Student Identity & Academic Context
            sb.append("[STUDENT IDENTITY & ACADEMIC CONTEXT]\n");
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
            sb.append("[PRIORITY RECOMMENDATIONS]\n");
            if (context.getActiveRecommendations() != null && !context.getActiveRecommendations().isEmpty()) {
                for (String rec : context.getActiveRecommendations()) {
                    sb.append("- ").append(rec).append("\n");
                }
            } else {
                sb.append("- Focus on active concept revision for weak areas.\n");
            }
            sb.append("\n");

            // 6. Today's Learning Plan
            sb.append("[TODAY'S LEARNING PLAN]\n");
            sb.append("- Active Subject: ").append(context.getActiveSubject()).append("\n");
            sb.append("- Today's Scheduled Focus: ").append(context.getTodayFocusTask()).append("\n\n");

            // 7. Conversation Summary
            if (context.getConversationSummary() != null && !context.getConversationSummary().isBlank()) {
                sb.append("[CONVERSATION MEMORY & SUMMARY]\n");
                sb.append("- ").append(context.getConversationSummary()).append("\n\n");
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
