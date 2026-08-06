package com.edupilot.service;

import com.edupilot.dto.PromptProfile;
import com.edupilot.dto.StudentContextDTO;
import com.edupilot.model.LearningMode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Modular Prompt Profile Pipeline Service for EduPilot AI Tutor.
 *
 * <p>Pipeline Architecture:
 * <pre>
 * Learning Mode → Intent Detection → Student Mastery Analysis → Response Length → Output Profile → Prompt Builder → Gemini
 * </pre>
 */
@Service
public class PromptBuilderService {

    // ══════════════════════════════════════════════════════════════════════════
    //  PIPELINE STEP 1 & 2: INTENT & CONTINUATION DETECTION
    // ══════════════════════════════════════════════════════════════════════════

    private enum IntentType {
        DEFINITION,
        EXPLANATION,
        DETAILED_TEACH,
        CODE,
        QUIZ,
        INTERVIEW,
        COMPARISON,
        REVISION,
        SUMMARY,
        GENERAL,
        // Continuation / Follow-up Intents
        CONTINUATION_EXPAND,
        CONTINUATION_SIMPLIFY,
        CONTINUATION_CODE,
        CONTINUATION_EXAMPLE,
        CONTINUATION_WHY,
        CONTINUATION_SUMMARY
    }

    private IntentType detectIntent(String userMessage, boolean hasMemory) {
        if (userMessage == null || userMessage.isBlank()) return IntentType.GENERAL;
        String m = userMessage.toLowerCase().trim();

        // ── 1. Continuation / Follow-up Detection ────────────────────────────
        if (hasMemory || isExplicitFollowupPhrase(m)) {
            if (containsAny(m, "give java code", "give python code", "give code", "show code", "code for this", "implement this", "write code for it", "java code for this")) {
                return IntentType.CONTINUATION_CODE;
            }
            if (containsAny(m, "show example", "give an example", "give example", "example for this", "show an example", "another example", "more examples")) {
                return IntentType.CONTINUATION_EXAMPLE;
            }
            if (containsAny(m, "can you simplify?", "can you simplify", "explain simpler", "simplify this", "explain again", "explain like i'm 5", "eli5", "in simple terms", "make it simpler")) {
                return IntentType.CONTINUATION_SIMPLIFY;
            }
            if (containsAny(m, "explain more", "continue", "expand on this", "expand", "go on", "tell me more", "elaborate more", "more details", "what else")) {
                return IntentType.CONTINUATION_EXPAND;
            }
            if (containsAny(m, "why?", "why is that", "why does this happen", "why is it", "why so")) {
                return IntentType.CONTINUATION_WHY;
            }
            if (containsAny(m, "summarize it", "summarise this", "summary of this", "summarize what you said", "summarise it")) {
                return IntentType.CONTINUATION_SUMMARY;
            }
        }

        // ── 2. Primary Intents ───────────────────────────────────────────────
        if (isGreetingOrSimple(m)) return IntentType.GENERAL;

        if (containsAny(m, "quiz me", "quiz", "test me", "test my", "give me mcq", "give me questions", "practice questions", "practice problem", "mcq")) {
            return IntentType.QUIZ;
        }

        if (containsAny(m, "mock interview", "interview question", "interview prep", "how would i answer", "interview tip")) {
            return IntentType.INTERVIEW;
        }

        if (containsAny(m, " vs ", " vs.", "versus", "compare ", "comparison", "difference between", "similarities")) {
            return IntentType.COMPARISON;
        }

        if (containsAny(m, "code", "implement", "implementation", "write a program", "write a function", "algorithm for", "program for", "code for", "java code", "python code", "c++ code")) {
            if (!containsAny(m, "explain", "what is", "define", "how does")) {
                return IntentType.CODE;
            }
        }

        if (containsAny(m, "revise ", "revision", "cheatsheet", "cheat sheet", "key points", "quick notes", "revision notes")) {
            return IntentType.REVISION;
        }

        if (containsAny(m, "summarise", "summarize", "key takeaway", "main points", "tldr", "tl;dr")) {
            return IntentType.SUMMARY;
        }

        if (containsAny(m, "teach me", "from scratch", "from the beginning", "deep dive", "comprehensive", "full guide", "in detail", "in-depth", "step by step", "step-by-step", "walk me through", "explain everything about")) {
            return IntentType.DETAILED_TEACH;
        }

        if (Pattern.compile("^(what is|what's|what are|who is|who's|define |definition of |meaning of |what does .+ mean)").matcher(m).find()) {
            return IntentType.DEFINITION;
        }

        if (containsAny(m, "explain ", "how does ", "how do ", "how is ", "why does ", "why do ", "why is ", "how it works", "what makes")) {
            return IntentType.EXPLANATION;
        }

        return IntentType.EXPLANATION;
    }

    private boolean isExplicitFollowupPhrase(String m) {
        return containsAny(m, "explain more", "continue", "can you simplify", "give java code", "show example", "why?", "explain again", "summarize it", "summarise it");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PIPELINE STEP 3: STUDENT MASTERY ANALYSIS
    // ══════════════════════════════════════════════════════════════════════════

    private String analyzeMasteryLevel(StudentContextDTO context, String userMessage) {
        if (context == null) return "INTERMEDIATE";

        double health = context.getLearningHealthScore();
        double sgi = context.getSgi();
        double cgpa = context.getCurrentCgpa();
        String risk = context.getRiskLevel() != null ? context.getRiskLevel() : "LOW";
        String msgLower = userMessage != null ? userMessage.toLowerCase() : "";

        // Check if query matches weak concepts or student is at academic risk
        boolean isWeakTopic = false;
        if (context.getWeakConcepts() != null) {
            for (String weak : context.getWeakConcepts()) {
                if (msgLower.contains(weak.toLowerCase())) {
                    isWeakTopic = true;
                    break;
                }
            }
        }

        if (isWeakTopic || "HIGH".equalsIgnoreCase(risk) || health < 60.0 || sgi < 6.0) {
            return "BEGINNER";
        }

        boolean isStrongTopic = false;
        if (context.getStrongConcepts() != null) {
            for (String strong : context.getStrongConcepts()) {
                if (msgLower.contains(strong.toLowerCase())) {
                    isStrongTopic = true;
                    break;
                }
            }
        }

        if (isStrongTopic || (health >= 85.0 && cgpa >= 8.5 && sgi >= 8.0)) {
            return "MASTER";
        }

        return "INTERMEDIATE";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PIPELINE STEP 4: RESPONSE LENGTH DETERMINATION
    // ══════════════════════════════════════════════════════════════════════════

    private String determineResponseLength(String userMessage, IntentType intent) {
        if (userMessage == null || userMessage.isBlank()) return "MEDIUM";
        String m = userMessage.toLowerCase().trim();

        // 1. Explicit SHORT keywords
        if (containsAny(m, "in short", "be short", "in brief", "be brief", "briefly", "short answer", "one line", "one-liner", "few words", "concise", "quick", "quickly", "just tell me", "tl;dr", "tldr", "summary")) {
            return "SHORT";
        }

        // 2. Explicit LONG keywords
        if (containsAny(m, "in detail", "detailed", "in-depth", "in depth", "step by step", "step-by-step", "walk me through", "teach me", "from scratch", "from the beginning", "deep dive", "thorough", "comprehensive", "full explanation")) {
            return "LONG";
        }

        // 3. Intent defaults
        return switch (intent) {
            case DEFINITION, SUMMARY, GENERAL, CONTINUATION_SIMPLIFY, CONTINUATION_SUMMARY, CONTINUATION_WHY -> "SHORT";
            case EXPLANATION, REVISION, COMPARISON, QUIZ, INTERVIEW, CONTINUATION_EXPAND, CONTINUATION_CODE, CONTINUATION_EXAMPLE -> "MEDIUM";
            case CODE, DETAILED_TEACH -> "LONG";
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PIPELINE STEP 5: OUTPUT PROFILE CONSTRUCTOR
    // ══════════════════════════════════════════════════════════════════════════

    private PromptProfile buildPromptProfile(StudentContextDTO context, LearningMode mode, String userMessage) {
        PromptProfile profile = new PromptProfile();
        boolean hasMemory = context != null && context.getConversationSummary() != null && !context.getConversationSummary().isBlank();

        // Step 1: Learning Mode
        LearningMode activeMode = mode != null ? mode : LearningMode.LEARN;
        profile.setLearningMode(activeMode);

        // Step 2: Intent Detection
        IntentType intent = detectIntent(userMessage, hasMemory);
        profile.setIntent(intent.name());

        boolean isContinuation = intent.name().startsWith("CONTINUATION_");
        profile.setContinuation(isContinuation);

        // Step 3: Student Mastery Analysis
        String mastery = analyzeMasteryLevel(context, userMessage);
        profile.setMasteryLevel(mastery);

        // Step 4: Response Length Determination
        String length = determineResponseLength(userMessage, intent);
        profile.setResponseLength(length);

        // Step 5: Output Style & Flags Configuration
        profile.setMarkdownStyle("Clean GitHub Flavored Markdown with headers (##), bold terms, code blocks, tables");
        profile.setOutputStyle("Adaptive Academic Mentor");

        boolean isShort = "SHORT".equals(length);
        boolean isLong = "LONG".equals(length);

        profile.setIncludeAnalogy("BEGINNER".equals(mastery) || isLong);
        profile.setIncludeExample(!isShort || intent == IntentType.CONTINUATION_EXAMPLE);
        profile.setIncludeComplexity((intent == IntentType.CODE || intent == IntentType.DETAILED_TEACH || activeMode == LearningMode.CODING) && !isShort);
        profile.setIncludeCode((intent == IntentType.CODE || intent == IntentType.DETAILED_TEACH || activeMode == LearningMode.CODING || intent == IntentType.CONTINUATION_CODE) && !isShort);
        profile.setIncludeInterviewTips(activeMode == LearningMode.INTERVIEW || intent == IntentType.INTERVIEW);
        profile.setIncludeRevisionNotes(activeMode == LearningMode.REVISION || intent == IntentType.REVISION);
        profile.setIncludeDiagram(isLong);

        // Build Section List dynamically based on Intent, Mode, and Length
        List<String> sections = new ArrayList<>();

        if (isContinuation) {
            switch (intent) {
                case CONTINUATION_CODE -> sections.add("Java Code & Explanation");
                case CONTINUATION_EXAMPLE -> sections.add("Illustrative Example");
                case CONTINUATION_SIMPLIFY -> {
                    sections.add("Simplified Intuition");
                    sections.add("Analogy");
                }
                case CONTINUATION_EXPAND -> sections.add("Further Elaboration & Next Steps");
                case CONTINUATION_WHY -> sections.add("Core Rationale & Cause");
                case CONTINUATION_SUMMARY -> sections.add("Key Takeaways Summary");
                default -> sections.add("Follow-Up Explanation");
            }
        } else {
            switch (intent) {
                case DEFINITION -> {
                    sections.add("Definition");
                    if (!isShort) sections.add("Key Characteristics");
                    if (!isShort) sections.add("Example");
                }
                case EXPLANATION -> {
                    if (isShort) {
                        sections.add("Direct Explanation");
                        sections.add("Example");
                    } else if (isLong) {
                        sections.add("Definition & Intuition");
                        sections.add("Working Mechanism");
                        sections.add("Analogy");
                        sections.add("Example");
                        sections.add("Complexity");
                        sections.add("Edge Cases");
                    } else { // MEDIUM
                        sections.add("Intuition");
                        sections.add("Working");
                        sections.add("Example");
                    }
                }
                case DETAILED_TEACH -> {
                    sections.add("Definition");
                    sections.add("Intuition");
                    sections.add("Working");
                    sections.add("Analogy");
                    sections.add("Example");
                    sections.add("Java Code");
                    sections.add("Dry Run");
                    sections.add("Complexity");
                    sections.add("Edge Cases");
                }
                case CODE -> {
                    if (isShort) {
                        sections.add("Java Code");
                        sections.add("Complexity");
                    } else {
                        sections.add("Working");
                        sections.add("Java Code");
                        sections.add("Example");
                        sections.add("Complexity");
                        if (isLong) sections.add("Edge Cases");
                    }
                }
                case QUIZ -> {
                    sections.add("Practice Questions");
                    sections.add("Answer Key & Explanations");
                }
                case REVISION -> {
                    sections.add("Definition");
                    sections.add("Revision Notes");
                    sections.add("Complexity");
                    sections.add("Common Mistakes");
                }
                case COMPARISON -> {
                    sections.add("Comparison Table");
                    sections.add("Key Trade-offs");
                    sections.add("When to Use Which");
                }
                case INTERVIEW -> {
                    sections.add("Interview Question");
                    sections.add("Model Answer");
                    sections.add("Follow-Up Questions");
                    sections.add("Interview Tips");
                }
                case SUMMARY -> {
                    sections.add("Definition");
                    sections.add("Working");
                    sections.add("Revision Notes");
                }
                case GENERAL -> sections.add("Conversational Response");
            }
        }

        // ── LEARNING MODE OVERRIDES (HIGHEST PRIORITY) ──────────────────────
        switch (activeMode) {
            case PRACTICE -> {
                sections.clear();
                sections.add("Interactive Practice Question");
                sections.add("Guided Hint");
                sections.add("Student Attempt Prompt");
                profile.setIncludeCode(false);
            }
            case REVISION -> {
                sections.clear();
                sections.add("Definition");
                sections.add("Revision Notes");
                sections.add("Complexity");
                sections.add("Common Mistakes");
                profile.setIncludeRevisionNotes(true);
            }
            case INTERVIEW -> {
                sections.clear();
                sections.add("Interview Question");
                sections.add("Model Answer");
                sections.add("Follow-Up Questions");
                sections.add("Interview Tips");
                profile.setIncludeInterviewTips(true);
            }
            case CODING -> {
                profile.setIncludeCode(true);
                profile.setIncludeComplexity(true);
                if (!sections.contains("Java Code")) {
                    sections.add("Java Code");
                }
            }
            case EXPLAIN_MISTAKES -> {
                sections.clear();
                sections.add("Common Mistakes");
                sections.add("Diagnostic Analysis");
                sections.add("Correct Reasoning");
            }
            case LEARN -> { /* Default concept teaching */ }
        }

        profile.setSectionList(sections);
        return profile;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PIPELINE STEP 6: PROMPT BUILDER
    // ══════════════════════════════════════════════════════════════════════════

    public String buildStructuredSystemPrompt(StudentContextDTO context, LearningMode mode, String userMessage) {
        // Step 1 - 5: Execute Prompt Profile Pipeline
        PromptProfile profile = buildPromptProfile(context, mode, userMessage);

        StringBuilder sb = new StringBuilder();

        // 1. Output Profile Directives
        sb.append("[PROMPT PROFILE & OUTPUT SPECIFICATION]\n");
        sb.append("- Active Learning Mode: ").append(profile.getLearningMode()).append("\n");
        sb.append("- Detected Intent: ").append(profile.getIntent()).append("\n");
        sb.append("- Student Mastery Level: ").append(profile.getMasteryLevel()).append("\n");
        sb.append("- Response Length: ").append(profile.getResponseLength()).append("\n");
        sb.append("- Is Follow-Up Continuation: ").append(profile.isContinuation()).append("\n");
        sb.append("- Markdown Style: ").append(profile.getMarkdownStyle()).append("\n\n");

        sb.append("[MANDATORY SECTIONS TO GENERATE]\n");
        sb.append("You MUST structure your output using ONLY the following section headers:\n");
        for (String section : profile.getSectionList()) {
            sb.append("  - ").append(section).append("\n");
        }
        sb.append("STRICT RULE: Do NOT generate unlisted sections. Keep response length proportional to ").append(profile.getResponseLength()).append(".\n\n");

        // 2. Length & Content Directives
        sb.append("[RESPONSE LENGTH CONSTRAINTS: ").append(profile.getResponseLength()).append("]\n");
        if ("SHORT".equals(profile.getResponseLength())) {
            sb.append("- Limit total response to 3–5 sentences maximum.\n");
            sb.append("- Do NOT include code blocks, ASCII diagrams, or complexity tables.\n");
            sb.append("- Answer ONLY the exact question directly.\n\n");
        } else if ("MEDIUM".equals(profile.getResponseLength())) {
            sb.append("- Provide a concise, balanced explanation (2–4 short paragraphs).\n");
            if (profile.isIncludeExample()) sb.append("- Include 1 short, practical example.\n");
            if (!profile.isIncludeCode()) sb.append("- Do NOT include code implementation unless explicitly required.\n");
            sb.append("\n");
        } else { // LONG
            sb.append("- Provide a complete, thorough lesson with detailed explanations.\n");
            if (profile.isIncludeAnalogy()) sb.append("- Include an intuitive real-world analogy.\n");
            if (profile.isIncludeCode()) sb.append("- Include complete, syntax-highlighted production code with comments. NEVER truncate code with '...'.\n");
            if (profile.isIncludeComplexity()) sb.append("- Include a Big-O Time & Space Complexity summary.\n");
            sb.append("\n");
        }

        // 3. Conversation Awareness / Continuation Directive
        if (profile.isContinuation()) {
            sb.append("[CONVERSATION CONTINUATION DIRECTIVE]\n");
            sb.append("- The student is following up on the previous discussion in [CONVERSATION MEMORY].\n");
            sb.append("- Do NOT restart the lesson or re-define basic concepts from scratch.\n");
            sb.append("- Respond directly to the requested follow-up action (e.g., providing code, giving an example, expanding details, or summarizing).\n\n");
        }

        // 4. Mastery Adaptation Instructions
        sb.append("[MASTERY ADAPTATION: ").append(profile.getMasteryLevel()).append("]\n");
        switch (profile.getMasteryLevel()) {
            case "BEGINNER" -> sb.append("- Use simple, accessible language, intuitive analogies, and step-by-step guidance. Avoid dense unexplained jargon.\n\n");
            case "INTERMEDIATE" -> sb.append("- Use balanced technical depth, standard terminology, and practical examples.\n\n");
            case "MASTER" -> sb.append("- Deliver advanced technical depth, optimization trade-offs, edge cases, and performance/system considerations.\n\n");
        }

        // 5. Learning Mode High Priority Directives
        sb.append("[LEARNING MODE DIRECTIVE: ").append(profile.getLearningMode()).append("]\n");
        switch (profile.getLearningMode()) {
            case LEARN -> sb.append("- Focus on clear concept teaching, intuition building, and structured guidance.\n\n");
            case PRACTICE -> sb.append("- DO NOT give direct solution answers immediately. Present 1 targeted practice problem and ask the student to attempt it with hints.\n\n");
            case REVISION -> sb.append("- Provide high-density cheat-sheet bullet points, formulas, and complexity summaries. Keep prose minimal.\n\n");
            case INTERVIEW -> sb.append("- Act as a Senior Technical Interviewer. Present an interview question, model candidate response, and candidate pitfalls.\n\n");
            case CODING -> sb.append("- Focus on algorithm walkthrough, complete code implementation, and Big-O complexity analysis.\n\n");
            case EXPLAIN_MISTAKES -> sb.append("- Address common conceptual pitfalls and misconceptions. Explain WHY mistakes happen and HOW to avoid them.\n\n");
        }

        // 6. Universal Directives
        sb.append("[UNIVERSAL DIRECTIVES]\n");
        sb.append("- NEVER open with generic intros: 'Hi Student', 'Hello!', 'Sure, I can help', or 'Regarding your question'.\n");
        sb.append("- Jump DIRECTLY into the answer.\n");
        sb.append("- Avoid unnecessary repetition. Keep answers proportional to the question.\n\n");

        // 7. Student Context Integration
        if (context != null) {
            sb.append("[STUDENT PROFILE]\n");
            sb.append("- Name: ").append(nvl(context.getStudentName())).append("\n");
            sb.append("- Degree & Branch: ").append(nvl(context.getDegree())).append(" in ").append(nvl(context.getBranch())).append("\n");
            sb.append("- Semester: Semester ").append(context.getSemester()).append("\n");
            sb.append("- CGPA: ").append(context.getCurrentCgpa()).append(" / Target ").append(context.getTargetCgpa()).append("\n");
            sb.append("- SGI: ").append(context.getSgi()).append(" / 10.0 | Risk: ").append(nvl(context.getRiskLevel())).append("\n\n");

            sb.append("[KNOWLEDGE MATRIX]\n");
            sb.append("- Learning Health: ").append(context.getLearningHealthScore()).append("%\n");
            sb.append("- Strong Concepts: ").append(listToString(context.getStrongConcepts())).append("\n");
            sb.append("- Weak Concepts: ").append(listToString(context.getWeakConcepts())).append("\n\n");

            if (context.getActiveRecommendations() != null && !context.getActiveRecommendations().isEmpty()) {
                sb.append("[ACTIVE RECOMMENDATIONS]\n");
                context.getActiveRecommendations().forEach(r -> sb.append("- ").append(r).append("\n"));
                sb.append("\n");
            }

            sb.append("[TODAY'S PLAN]\n");
            sb.append("- Subject: ").append(nvl(context.getActiveSubject())).append("\n");
            sb.append("- Focus Task: ").append(nvl(context.getTodayFocusTask())).append("\n\n");

            if (context.getConversationSummary() != null && !context.getConversationSummary().isBlank()) {
                sb.append("[CONVERSATION MEMORY]\n");
                sb.append(context.getConversationSummary()).append("\n");
            }
        }

        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BACKWARDS-COMPATIBLE OVERLOADS
    // ══════════════════════════════════════════════════════════════════════════

    public String buildStructuredSystemPrompt(StudentContextDTO context, LearningMode mode) {
        return buildStructuredSystemPrompt(context, mode, null);
    }

    public String buildStructuredSystemPrompt(StudentContextDTO context) {
        return buildStructuredSystemPrompt(context, LearningMode.LEARN, null);
    }

    public String buildSystemPrompt(Map<String, Object> contextMap) {
        StudentContextDTO dto = new StudentContextDTO();
        if (contextMap != null) {
            if (contextMap.containsKey("studentName")) dto.setStudentName((String) contextMap.get("studentName"));
            if (contextMap.containsKey("referencedConcept")) dto.setTodayFocusTask((String) contextMap.get("referencedConcept"));
        }
        return buildStructuredSystemPrompt(dto, LearningMode.LEARN, null);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UTILITY METHODS
    // ══════════════════════════════════════════════════════════════════════════

    private boolean containsAny(String msg, String... keywords) {
        for (String kw : keywords) {
            if (msg.contains(kw)) return true;
        }
        return false;
    }

    private boolean isGreetingOrSimple(String msg) {
        String clean = msg.replaceAll("[^a-z0-9 ]", "").trim();
        return Pattern.compile(
            "^(hi|hello|hey|hii|helo|good morning|good afternoon|good evening|" +
            "whats up|how are you|ok|okay|thanks|thank you|got it|understood|" +
            "great|nice|cool|sure|yes|no|yep|nope|start|begin|help|help me)$"
        ).matcher(clean).find();
    }

    private String nvl(String s) {
        return (s != null && !s.isBlank()) ? s : "N/A";
    }

    private String listToString(List<String> list) {
        if (list == null || list.isEmpty()) return "N/A";
        return String.join(", ", list);
    }
}
