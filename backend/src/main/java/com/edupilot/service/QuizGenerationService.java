package com.edupilot.service;

import com.edupilot.model.QuizQuestion;
import com.edupilot.model.StudentProfile;
import com.edupilot.repository.QuizQuestionRepository;
import com.edupilot.repository.StudentProfileRepository;
import com.edupilot.service.llm.GroqProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuizGenerationService {

    @Autowired
    private GroqProvider groqProvider;

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private QuizQuestionRepository questionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public QuizQuestion.Difficulty resolveDifficulty(StudentProfile profile, String subject) {
        if (profile.getConceptMastery() != null) {
            Double mastery = profile.getConceptMastery().get(subject);
            if (mastery != null) {
                if (mastery < 40) return QuizQuestion.Difficulty.EASY;
                if (mastery < 75) return QuizQuestion.Difficulty.MEDIUM;
                return QuizQuestion.Difficulty.HARD;
            }
        }

        String risk = profile.getAcademicRiskLevel();
        if (risk != null) {
            if (risk.equalsIgnoreCase("HIGH")) return QuizQuestion.Difficulty.EASY;
            if (risk.equalsIgnoreCase("MEDIUM")) return QuizQuestion.Difficulty.MEDIUM;
        }

        if (profile.getCurrentCgpa() >= 8.0) return QuizQuestion.Difficulty.HARD;
        if (profile.getCurrentCgpa() >= 6.0) return QuizQuestion.Difficulty.MEDIUM;
        return QuizQuestion.Difficulty.EASY;
    }

    public List<QuizQuestion> generateForStudent(String studentId, String subject, int count) {
        StudentProfile profile = profileRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found: " + studentId));

        QuizQuestion.Difficulty difficulty = resolveDifficulty(profile, subject);
        return generate(subject, difficulty, count);
    }

    public List<QuizQuestion> generate(String subject, QuizQuestion.Difficulty difficulty, int count) {
        return generate(subject, difficulty, count, Map.of());
    }

    public List<QuizQuestion> generate(String subject, QuizQuestion.Difficulty difficulty, int count, Map<String, Object> callerContext) {
        int targetCount = Math.min(count > 0 ? count : 5, 5);
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(subject, difficulty, targetCount, callerContext);
        Map<String, Object> context = new HashMap<>();
        if (callerContext != null) context.putAll(callerContext);
        if (!context.containsKey("maxTokens")) context.put("maxTokens", 1200);
        if (!context.containsKey("purpose")) context.put("purpose", "DASHBOARD_BATCH");

        int maxRetries = 2;
        String lastError = "Unknown error";
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String currentPrompt = userPrompt;
            if (attempt > 1) {
                currentPrompt += "\n\nSTRICT JSON RETRY NOTICE (Attempt " + attempt + " of " + maxRetries + "):\n" +
                        "Previous output failed JSON parsing. Output strictly valid RFC-8259 JSON using double quotes for all keys and strings.";
            }
            try {
                String rawResponse = groqProvider.generateResponse(systemPrompt, currentPrompt, context);

                if (rawResponse != null && (rawResponse.contains("RATE_LIMIT_TPD") || rawResponse.contains("retryAfterMs=9") || rawResponse.contains("retryAfterMs=8") || rawResponse.contains("retryAfterMs=7") || rawResponse.contains("retryAfterMs=6"))) {
                    System.err.println("[QuizGenerationService] Groq Daily Quota Exceeded (TPD). Halting automatic retries.");
                    throw new IllegalStateException("Groq daily token quota (TPD) reached. Assessment question not consumed. Please retry after quota resets.");
                }

                List<QuizQuestion> questions = parseQuestions(rawResponse, subject, difficulty);
                if (!questions.isEmpty()) {
                    questionRepository.saveAll(questions);
                    return questions;
                }
            } catch (Exception ex) {
                lastError = ex.getMessage();
                if (ex.getMessage() != null && ex.getMessage().contains("daily token quota")) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Groq API question generation failed for subject '" + subject + "' after " + maxRetries + " attempts. Last failure: " + lastError);
    }

    public List<QuizQuestion> generateForConcept(String subject, String concept, QuizQuestion.Difficulty difficulty, int count) {
        if (concept == null || concept.trim().isEmpty()) {
            return generate(subject, difficulty != null ? difficulty : QuizQuestion.Difficulty.MEDIUM, count);
        }
        if (difficulty == null) {
            difficulty = QuizQuestion.Difficulty.MEDIUM;
        }

        int targetCount = Math.min(count > 0 ? count : 5, 5);
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPromptForConcept(subject, concept, difficulty, targetCount);
        Map<String, Object> context = Map.of("maxTokens", 1200, "purpose", "CONCEPT_REMEDIATION_BATCH");

        int maxRetries = 2;
        String lastError = "Unknown error";
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String currentPrompt = userPrompt;
            if (attempt > 1) {
                currentPrompt += "\n\nSTRICT JSON RETRY NOTICE (Attempt " + attempt + " of " + maxRetries + "):\n" +
                        "Previous output failed JSON parsing. Output strictly valid RFC-8259 JSON using double quotes for all keys and strings.";
            }
            try {
                String rawResponse = groqProvider.generateResponse(systemPrompt, currentPrompt, context);

                if (rawResponse != null && (rawResponse.contains("RATE_LIMIT_TPD") || rawResponse.contains("retryAfterMs=9") || rawResponse.contains("retryAfterMs=8") || rawResponse.contains("retryAfterMs=7") || rawResponse.contains("retryAfterMs=6"))) {
                    System.err.println("[QuizGenerationService] Groq Daily Quota Exceeded (TPD). Halting automatic retries.");
                    throw new IllegalStateException("Groq daily token quota (TPD) reached. Assessment question not consumed. Please retry after quota resets.");
                }

                List<QuizQuestion> questions = parseQuestions(rawResponse, subject, difficulty);
                if (!questions.isEmpty()) {
                    for (QuizQuestion q : questions) {
                        q.setConcept(concept);
                    }
                    questionRepository.saveAll(questions);
                    return questions;
                }
            } catch (Exception ex) {
                lastError = ex.getMessage();
                if (ex.getMessage() != null && ex.getMessage().contains("daily token quota")) {
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Groq API question generation failed for concept '" + concept + "' after " + maxRetries + " attempts. Last failure: " + lastError);
    }

    public static class QuestionBlueprintSpec {
        private int position;
        private String concept;
        private QuizQuestion.Difficulty difficulty;

        public QuestionBlueprintSpec() {}

        public QuestionBlueprintSpec(int position, String concept, QuizQuestion.Difficulty difficulty) {
            this.position = position;
            this.concept = concept;
            this.difficulty = difficulty;
        }

        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
        public String getConcept() { return concept; }
        public void setConcept(String concept) { this.concept = concept; }
        public QuizQuestion.Difficulty getDifficulty() { return difficulty; }
        public void setDifficulty(QuizQuestion.Difficulty difficulty) { this.difficulty = difficulty; }
    }

    public List<QuizQuestion> generateBatchDiagnosticQuestionsViaGroq(String subject, List<QuestionBlueprintSpec> blueprint, Map<String, Object> context) {
        if (blueprint == null || blueprint.isEmpty()) {
            throw new IllegalArgumentException("Blueprint cannot be null or empty");
        }

        Map<String, Object> genContext = context != null ? new HashMap<>(context) : new HashMap<>();
        if (!genContext.containsKey("maxTokens")) genContext.put("maxTokens", 3500);
        if (!genContext.containsKey("purpose")) genContext.put("purpose", "DIAGNOSTIC_BATCH_10");

        List<String> excludeTexts = genContext.containsKey("excludeQuestions") 
                ? (List<String>) genContext.get("excludeQuestions") : List.of();

        List<String> cleanExclusions = new ArrayList<>();
        if (excludeTexts != null) {
            for (String exc : excludeTexts) {
                if (exc != null && !exc.isBlank() && !exc.startsWith("fp_")) {
                    cleanExclusions.add(exc);
                }
            }
        }
        List<String> recentExclusions = cleanExclusions.size() > 6
                ? cleanExclusions.subList(cleanExclusions.size() - 6, cleanExclusions.size())
                : cleanExclusions;

        String systemPrompt = "You are an expert academic assessment question generator for " + subject + ".\n" +
                "CRITICAL INSTRUCTIONS:\n" +
                "1. You MUST respond with ONLY a single valid JSON object. Do NOT include markdown code blocks (such as ```json), preambles, or commentary.\n" +
                "2. All keys and string values MUST use strict double quotes (\"). NEVER use single quotes (') or unescaped control characters.\n" +
                "3. Ensure all brackets, braces, and double quotes are perfectly closed and valid RFC-8259 syntax.\n" +
                "4. Follow the exact JSON structure specified below.";

        StringBuilder baseUserPrompt = new StringBuilder();
        baseUserPrompt.append("Generate EXACTLY ").append(blueprint.size()).append(" multiple-choice diagnostic questions for subject \"").append(subject)
                .append("\" strictly following the 10-question blueprint below.\n\n")
                .append("10-QUESTION BLUEPRINT:\n");

        for (QuestionBlueprintSpec spec : blueprint) {
            baseUserPrompt.append("Question ").append(spec.getPosition())
                    .append(": Concept: \"").append(spec.getConcept())
                    .append("\", Difficulty: ").append(spec.getDifficulty().name()).append("\n");
        }

        baseUserPrompt.append("\nRequirements:\n")
                .append("- Generate EXACTLY ").append(blueprint.size()).append(" questions matching blueprint items 1 through ").append(blueprint.size()).append(" in exact sequential order.\n")
                .append("- Question 1 MUST match blueprint item 1, Question 2 MUST match blueprint item 2, ..., Question 10 MUST match blueprint item 10.\n")
                .append("- Do NOT change the assigned concept or difficulty for any question.\n")
                .append("- Each question must test genuine conceptual understanding.\n")
                .append("- Exactly 4 distinct answer options per question, with only one correct option.\n")
                .append("- Include correctOptionIndex (0, 1, 2, or 3).\n")
                .append("- Include a brief conceptual explanation of why the correct answer is correct.\n")
                .append("- Do NOT duplicate questions within this batch.\n");

        if (!recentExclusions.isEmpty()) {
            baseUserPrompt.append("- DO NOT generate questions similar to these existing question texts:\n");
            for (String exc : recentExclusions) {
                String shortExc = exc.length() > 60 ? exc.substring(0, 60) + "..." : exc;
                baseUserPrompt.append("  * ").append(shortExc).append("\n");
            }
        }

        baseUserPrompt.append("\nRequired JSON Format (strict double quotes ONLY):\n")
                .append("{\n")
                .append("  \"questions\": [\n")
                .append("    {\n")
                .append("      \"concept\": \"<concept_from_blueprint>\",\n")
                .append("      \"questionText\": \"Clear conceptual question text here\",\n")
                .append("      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n")
                .append("      \"correctOptionIndex\": 0,\n")
                .append("      \"conceptualExplanation\": \"Explanation of why option A is correct\"\n")
                .append("    }\n")
                .append("  ]\n")
                .append("}");

        int promptChars = systemPrompt.length() + baseUserPrompt.length();
        int estTokens = promptChars / 4;

        // PART 15 Logging requirement: Safe structured log before sending batch request
        System.out.println("========== GROQ BATCH REQUEST ==========");
        System.out.println("Purpose: DIAGNOSTIC_BATCH_10");
        System.out.println("Subject: " + subject);
        System.out.println("Batch Size: " + blueprint.size());
        if (genContext.containsKey("adaptiveSummary")) {
            System.out.println("Adaptive Profile: " + genContext.get("adaptiveSummary"));
        }
        System.out.println("Blueprint:");
        for (QuestionBlueprintSpec spec : blueprint) {
            System.out.println("Q" + spec.getPosition() + ": " + spec.getConcept() + " / " + spec.getDifficulty().name());
        }
        System.out.println("Prompt Characters: " + promptChars);
        System.out.println("Estimated Prompt Tokens: " + estTokens);
        System.out.println("========================================");

        int maxRetries = 2;
        String lastError = "Groq API returned empty or invalid batch output";

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            StringBuilder currentPrompt = new StringBuilder(baseUserPrompt);
            if (attempt > 1) {
                currentPrompt.append("\n\nSTRICT JSON RETRY NOTICE (Attempt ").append(attempt).append(" of ").append(maxRetries).append("):\n")
                        .append("Your previous output failed validation. Output strictly valid RFC-8259 JSON containing an array of exactly 10 questions matching the blueprint.");
            }

            try {
                String rawResponse = groqProvider.generateResponse(systemPrompt, currentPrompt.toString(), genContext);

                if (rawResponse != null && (rawResponse.contains("RATE_LIMIT_TPD") || rawResponse.contains("retryAfterMs=9") || rawResponse.contains("retryAfterMs=8"))) {
                    System.err.println("[QuizGenerationService] Groq Daily Quota Exceeded (TPD). Halting automatic retries.");
                    throw new IllegalStateException("Groq daily token quota (TPD) reached. Please retry after quota resets.");
                }

                List<QuizQuestion> parsedBatch = parseBatchQuestions(rawResponse, subject, blueprint);
                if (!parsedBatch.isEmpty() && parsedBatch.size() == blueprint.size()) {
                    List<QuizQuestion> savedBatch = questionRepository.saveAll(parsedBatch);
                    System.out.println("========== GROQ BATCH RESPONSE ==========");
                    System.out.println("Groq response received");
                    System.out.println("Question count: " + savedBatch.size());
                    System.out.println("Validation result: SUCCESS");
                    System.out.println("=========================================");
                    return savedBatch;
                } else {
                    lastError = "Parsed batch size (" + parsedBatch.size() + ") did not match requested blueprint size (" + blueprint.size() + ")";
                }
            } catch (Exception ex) {
                lastError = ex.getMessage();
                System.err.println("[QuizGenerationService] Batch generation attempt " + attempt + " failed: " + lastError);
                if (lastError != null && lastError.contains("daily token quota")) throw ex;
            }
        }

        throw new IllegalStateException("Groq API 10-question batch generation failed after " + maxRetries + " attempts. Last error: " + lastError);
    }

    private List<QuizQuestion> parseBatchQuestions(String rawJson, String subject, List<QuestionBlueprintSpec> blueprint) {
        List<QuizQuestion> result = new ArrayList<>();
        if (rawJson == null || rawJson.isBlank()) return result;

        String cleanJson = rawJson.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();

        try {
            JsonNode root = objectMapper.readTree(cleanJson);
            if (root.has("success") && !root.get("success").asBoolean(true)) {
                System.err.println("[QuizGenerationService] Groq error payload in batch parse: " + root.toString());
                return result;
            }

            JsonNode questionsNode = root.get("questions");
            if (questionsNode == null || !questionsNode.isArray() || questionsNode.size() != blueprint.size()) {
                System.err.println("[QuizGenerationService] Invalid questions array in batch response. Expected " + blueprint.size() + ", got: " + (questionsNode != null && questionsNode.isArray() ? questionsNode.size() : "none"));
                return result;
            }

            Set<String> seenTexts = new HashSet<>();

            for (int i = 0; i < questionsNode.size(); i++) {
                JsonNode qNode = questionsNode.get(i);
                QuestionBlueprintSpec spec = blueprint.get(i);

                if (!qNode.has("questionText") || !qNode.has("options") || !qNode.get("options").isArray()) {
                    System.err.println("[QuizGenerationService] Question " + (i+1) + " missing questionText or options array");
                    return new ArrayList<>();
                }

                String questionText = qNode.path("questionText").asText().trim();
                if (questionText.isEmpty() || seenTexts.contains(questionText.toLowerCase())) {
                    System.err.println("[QuizGenerationService] Duplicate or empty questionText at index " + i + ": " + questionText);
                    return new ArrayList<>();
                }
                seenTexts.add(questionText.toLowerCase());

                List<String> options = new ArrayList<>();
                qNode.get("options").forEach(opt -> options.add(opt.asText().trim()));
                if (options.size() != 4) {
                    System.err.println("[QuizGenerationService] Question " + (i+1) + " options size is not 4: " + options.size());
                    return new ArrayList<>();
                }

                int correctIdx = qNode.path("correctOptionIndex").asInt(0);
                if (correctIdx < 0 || correctIdx > 3) {
                    correctIdx = 0;
                }

                String explanation = qNode.path("conceptualExplanation").asText("Conceptual explanation for " + spec.getConcept()).trim();
                if (explanation.isEmpty()) {
                    explanation = "Conceptual explanation for " + spec.getConcept();
                }

                QuizQuestion question = QuizQuestion.builder()
                        .subject(subject)
                        .concept(spec.getConcept())
                        .difficulty(spec.getDifficulty())
                        .questionText(questionText)
                        .options(options)
                        .correctOptionIndex(correctIdx)
                        .conceptualExplanation(explanation)
                        .build();

                question.setQuestionSource("GROQ_DIAGNOSTIC_BATCH");
                question.setGenerationVersion(3);
                String fp = "fp_" + Math.abs(question.getQuestionText().hashCode());
                question.setQuestionFingerprint(fp);

                result.add(question);
            }
        } catch (Exception e) {
            System.err.println("[QuizGenerationService] Failed to parse batch Groq response: " + e.getMessage());
        }
        return result;
    }

    public QuizQuestion generateOneDiagnosticQuestionViaGroq(String subject, String concept, QuizQuestion.Difficulty difficulty, Map<String, Object> context) {
        if (difficulty == null) difficulty = QuizQuestion.Difficulty.MEDIUM;
        if (concept == null || concept.isBlank()) concept = "General Principles";

        Map<String, Object> genContext = context != null ? new HashMap<>(context) : new HashMap<>();
        if (!genContext.containsKey("maxTokens")) genContext.put("maxTokens", 800);
        if (!genContext.containsKey("purpose")) genContext.put("purpose", "DIAGNOSTIC_ONE_BY_ONE");

        List<String> excludeTexts = genContext.containsKey("excludeQuestions") 
                ? (List<String>) genContext.get("excludeQuestions") : List.of();

        System.out.println("[QuizGenerationService] Adaptive question generation: concept = " + concept +
                ", difficulty = " + difficulty.name() +
                ", excludedFingerprintCount = " + excludeTexts.size());

        List<String> recentExclusions = excludeTexts.size() > 6
                ? excludeTexts.subList(excludeTexts.size() - 6, excludeTexts.size())
                : excludeTexts;

        String systemPrompt = "You are an expert academic assessment question generator for " + subject + ".\n" +
                "CRITICAL INSTRUCTIONS:\n" +
                "1. You MUST respond with ONLY a single valid JSON object. Do NOT include markdown code blocks (such as ```json), preambles, or commentary.\n" +
                "2. All keys and string values MUST use strict double quotes (\"). NEVER use single quotes (') or unescaped control characters.\n" +
                "3. Ensure all brackets, braces, and double quotes are perfectly closed and valid RFC-8259 syntax.\n" +
                "4. Follow the exact JSON structure specified below.";

        StringBuilder baseUserPrompt = new StringBuilder();
        baseUserPrompt.append("Generate EXACTLY 1 multiple-choice diagnostic question for subject \"").append(subject)
                .append("\", concept \"").append(concept)
                .append("\" at ").append(difficulty.name()).append(" difficulty.\n\n")
                .append("Requirements:\n")
                .append("- Test genuine conceptual understanding of ").append(concept).append(".\n")
                .append("- Exactly 4 distinct answer options, with only one correct option.\n")
                .append("- Include a brief conceptual explanation.\n");

        if (genContext.containsKey("mastery")) {
            baseUserPrompt.append("- Student current mastery: ").append(genContext.get("mastery")).append("%.\n");
        }
        if (!recentExclusions.isEmpty()) {
            baseUserPrompt.append("- DO NOT generate questions similar to these existing questions/fingerprints:\n");
            for (String exc : recentExclusions) {
                String shortExc = exc.length() > 60 ? exc.substring(0, 60) + "..." : exc;
                baseUserPrompt.append("  * ").append(shortExc).append("\n");
            }
        }

        baseUserPrompt.append("\nRequired JSON Format (strict double quotes ONLY):\n")
                .append("{\n")
                .append("  \"questions\": [\n")
                .append("    {\n")
                .append("      \"concept\": \"").append(concept.replace("\"", "'")).append("\",\n")
                .append("      \"questionText\": \"Clear conceptual question text here\",\n")
                .append("      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n")
                .append("      \"correctOptionIndex\": 0,\n")
                .append("      \"conceptualExplanation\": \"Explanation of why option A is correct\"\n")
                .append("    }\n")
                .append("  ]\n")
                .append("}");

        int maxRetries = 2;
        String lastError = "Groq API returned empty or invalid output";

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            StringBuilder currentPrompt = new StringBuilder(baseUserPrompt);
            if (attempt > 1) {
                currentPrompt.append("\n\nSTRICT JSON RETRY NOTICE (Attempt ").append(attempt).append(" of ").append(maxRetries).append("):\n")
                        .append("Your previous output failed JSON validation. Output strictly valid RFC-8259 JSON using double quotes for all keys and string values.");
            }

            try {
                String rawResponse = groqProvider.generateResponse(systemPrompt, currentPrompt.toString(), genContext);

                if (rawResponse != null && (rawResponse.contains("RATE_LIMIT_TPD") || rawResponse.contains("retryAfterMs=9") || rawResponse.contains("retryAfterMs=8") || rawResponse.contains("retryAfterMs=7") || rawResponse.contains("retryAfterMs=6"))) {
                    System.err.println("[QuizGenerationService] Groq Daily Quota Exceeded (TPD). Halting automatic retries.");
                    throw new IllegalStateException("Groq daily token quota (TPD) reached. Assessment question not consumed. Please retry after quota resets.");
                }

                if (rawResponse != null && (rawResponse.contains("RATE_LIMIT_TPM") || rawResponse.contains("RATE_LIMITED"))) {
                    long retryDelayMs = 2000;
                    if (rawResponse.contains("retryAfterMs=")) {
                        try {
                            int startIdx = rawResponse.indexOf("retryAfterMs=") + 13;
                            int endIdx = rawResponse.indexOf("\"", startIdx);
                            if (endIdx < 0) endIdx = rawResponse.indexOf("}", startIdx);
                            if (endIdx > startIdx) {
                                retryDelayMs = Long.parseLong(rawResponse.substring(startIdx, endIdx));
                            }
                        } catch (Exception ignored) {}
                    }

                    if (retryDelayMs >= 60000) {
                        System.err.println("[QuizGenerationService] TPD delay detected (" + retryDelayMs + "ms). Halting automatic retries.");
                        throw new IllegalStateException("Groq daily token quota (TPD) reached. Assessment question not consumed. Please retry after quota resets.");
                    }

                    System.err.println("[QuizGenerationService] Groq rate limited (TPM): retryAfter = " + retryDelayMs + "ms, attempt = " + attempt);
                    lastError = "Groq API TPM rate limit (429) exceeded.";

                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(Math.min(retryDelayMs, 4000));
                        } catch (InterruptedException ignored) {}
                        continue;
                    }
                }

                List<QuizQuestion> parsed = parseQuestions(rawResponse, subject, difficulty);
                if (!parsed.isEmpty()) {
                    QuizQuestion q = parsed.get(0);
                    q.setConcept(concept);
                    q.setDifficulty(difficulty);
                    q.setQuestionSource("GROQ_DIAGNOSTIC_DYNAMIC");
                    String fp = "fp_" + Math.abs(q.getQuestionText().hashCode());
                    q.setQuestionFingerprint(fp);
                    return questionRepository.save(q);
                } else {
                    lastError = "Parsed questions list was empty (raw response: " + (rawResponse != null ? rawResponse.replaceAll("\\s+", " ") : "null") + ")";
                }
            } catch (Exception ex) {
                lastError = ex.getMessage();
                System.err.println("[QuizGenerationService] Groq generation attempt " + attempt + " failed: " + lastError);
            }
        }

        // Absolute No-Fallback Principle: NEVER return static fallback questions for diagnostic tests!
        throw new IllegalStateException("Groq API question generation failed after " + maxRetries + " attempts. Last error: " + lastError);
    }

    private String buildSystemPrompt() {
        return "You are an expert academic question generator.\n" +
               "CRITICAL INSTRUCTIONS:\n" +
               "1. You MUST respond with ONLY a single valid JSON object. Do NOT include markdown code blocks (such as ```json), preambles, or commentary.\n" +
               "2. All keys and string values MUST use strict double quotes (\"). NEVER use single quotes (').\n" +
               "3. Ensure the JSON structure matches the specified schema with valid syntax.";
    }

    private String buildUserPrompt(String subject, QuizQuestion.Difficulty difficulty, int count) {
        return buildUserPrompt(subject, difficulty, count, Map.of());
    }

    private String buildUserPrompt(String subject, QuizQuestion.Difficulty difficulty, int count, Map<String, Object> context) {
        StringBuilder prompt = new StringBuilder("Generate ").append(count)
                .append(" multiple-choice quiz questions for subject \"").append(subject)
                .append("\" at ").append(difficulty.name()).append(" difficulty level.\n\n")
                .append("Requirements:\n")
                .append("- Each question must test genuine conceptual understanding.\n")
                .append("- Exactly 4 answer options per question, only one correct.\n")
                .append("- Include a short conceptual explanation.\n")
                .append("- Use strict double quotes (\") ONLY.\n");

        if (context != null && context.containsKey("excludeQuestions")) {
            List<String> excludeTexts = (List<String>) context.get("excludeQuestions");
            if (excludeTexts != null && !excludeTexts.isEmpty()) {
                List<String> recent = excludeTexts.size() > 6 ? excludeTexts.subList(excludeTexts.size() - 6, excludeTexts.size()) : excludeTexts;
                prompt.append("- DO NOT generate questions similar to these previous question fingerprints/texts:\n");
                for (String exc : recent) {
                    String shortExc = exc.length() > 60 ? exc.substring(0, 60) + "..." : exc;
                    prompt.append("  * ").append(shortExc).append("\n");
                }
            }
        }

        prompt.append("\nRequired JSON Format:\n")
                .append("{\n")
                .append("  \"questions\": [\n")
                .append("    {\n")
                .append("      \"concept\": \"Core Concept\",\n")
                .append("      \"questionText\": \"Question text here\",\n")
                .append("      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n")
                .append("      \"correctOptionIndex\": 0,\n")
                .append("      \"conceptualExplanation\": \"Explanation text here\"\n")
                .append("    }\n")
                .append("  ]\n")
                .append("}");
        return prompt.toString();
    }

    private String buildUserPromptForConcept(String subject, String concept, QuizQuestion.Difficulty difficulty, int count) {
        return "Generate " + count + " multiple-choice quiz questions specifically testing the concept \"" + concept +
               "\" within the subject \"" + subject + "\" at " + difficulty.name() + " difficulty level.\n\n" +
               "Requirements:\n" +
               "- Each question must test genuine conceptual understanding of " + concept + ".\n" +
               "- Exactly 4 answer options per question, only one correct.\n" +
               "- Include a short explanation of why the correct answer is correct.\n" +
               "- CRITICAL: Use strict double quotes (\") for all JSON keys and string values. Never use single quotes (').\n\n" +
               "Respond with JSON in exactly this shape:\n" +
               "{\"questions\": [{" +
               "\"concept\": \"" + concept + "\", " +
               "\"questionText\": \"the question\", " +
               "\"options\": [\"option A\", \"option B\", \"option C\", \"option D\"], " +
               "\"correctOptionIndex\": 0, " +
               "\"conceptualExplanation\": \"why this answer is correct\"" +
               "}]}";
    }

    private List<QuizQuestion> parseQuestions(String rawJson, String subject, QuizQuestion.Difficulty difficulty) {
        List<QuizQuestion> result = new ArrayList<>();
        if (rawJson == null || rawJson.isBlank()) return result;

        String cleanJson = rawJson.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();

        try {
            JsonNode root = objectMapper.readTree(cleanJson);

            if (root.has("success") && !root.get("success").asBoolean(true)) {
                System.err.println("[QuizGenerationService] Groq returned an error payload: " + root.toString());
                return result;
            }

            JsonNode questionsNode = root.get("questions");
            if (questionsNode == null || !questionsNode.isArray() || questionsNode.isEmpty()) {
                System.err.println("[QuizGenerationService] No valid 'questions' array in Groq response: " + cleanJson);
                return result;
            }

            for (JsonNode q : questionsNode) {
                if (!q.has("questionText") || !q.has("options") || !q.get("options").isArray()) continue;

                List<String> options = new ArrayList<>();
                q.get("options").forEach(opt -> options.add(opt.asText()));

                if (options.size() != 4) continue;

                QuizQuestion question = QuizQuestion.builder()
                        .subject(subject)
                        .concept(q.path("concept").asText(subject + " Core"))
                        .difficulty(difficulty)
                        .questionText(q.path("questionText").asText())
                        .options(options)
                        .correctOptionIndex(q.path("correctOptionIndex").asInt(0))
                        .conceptualExplanation(q.path("conceptualExplanation").asText("Conceptual explanation for " + subject))
                        .build();

                question.setQuestionSource("GROQ_AI_GENERATED");
                question.setGenerationVersion(3);
                String fp = "fp_" + Math.abs(question.getQuestionText().hashCode());
                question.setQuestionFingerprint(fp);
                result.add(question);
            }
        } catch (Exception e) {
            System.err.println("[QuizGenerationService] Failed to parse Groq response: " + e.getMessage());
            System.err.println("[QuizGenerationService] Raw response was: " + rawJson);
        }
        return result;
    }
}
