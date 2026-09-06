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

    private String stripMarkdownFences(String input) {
        if (input == null || input.isBlank()) return "";
        String text = input.trim();

        int firstFence = text.indexOf("```");
        if (firstFence != -1) {
            int contentStart = text.indexOf('\n', firstFence);
            if (contentStart != -1) {
                int lastFence = text.lastIndexOf("```");
                if (lastFence > contentStart) {
                    text = text.substring(contentStart + 1, lastFence).trim();
                } else {
                    text = text.substring(contentStart + 1).trim();
                }
            } else {
                text = text.substring(firstFence + 3).trim();
                if (text.endsWith("```")) {
                    text = text.substring(0, text.length() - 3).trim();
                }
            }
        }

        int firstBrace = text.indexOf('{');
        int firstBracket = text.indexOf('[');

        if (firstBrace != -1 && (firstBracket == -1 || firstBrace < firstBracket)) {
            int lastBrace = text.lastIndexOf('}');
            if (lastBrace > firstBrace) {
                text = text.substring(firstBrace, lastBrace + 1).trim();
            }
        } else if (firstBracket != -1) {
            int lastBracket = text.lastIndexOf(']');
            if (lastBracket > firstBracket) {
                text = text.substring(firstBracket, lastBracket + 1).trim();
            }
        }

        return text;
    }

    private static class BatchParseResult {
        private final List<QuizQuestion> questions;
        private final String errorReason;

        public BatchParseResult(List<QuizQuestion> questions) {
            this.questions = questions;
            this.errorReason = null;
        }

        public BatchParseResult(String errorReason) {
            this.questions = Collections.emptyList();
            this.errorReason = errorReason;
        }

        public boolean isSuccess() {
            return errorReason == null && questions != null && !questions.isEmpty();
        }

        public List<QuizQuestion> getQuestions() { return questions; }
        public String getErrorReason() { return errorReason; }
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
                "4. Output MUST contain a top-level key \"questions\" with an array of EXACTLY " + blueprint.size() + " question objects matching the requested blueprint.";

        StringBuilder baseUserPrompt = new StringBuilder();
        baseUserPrompt.append("Generate EXACTLY ").append(blueprint.size()).append(" multiple-choice diagnostic questions for subject \"").append(subject)
                .append("\" strictly following the ").append(blueprint.size()).append("-question blueprint below.\n\n")
                .append(blueprint.size()).append("-QUESTION BLUEPRINT:\n");

        for (QuestionBlueprintSpec spec : blueprint) {
            baseUserPrompt.append("Question ").append(spec.getPosition())
                    .append(": Concept: \"").append(spec.getConcept())
                    .append("\", Difficulty: ").append(spec.getDifficulty().name()).append("\n");
        }

        baseUserPrompt.append("\nRequirements:\n")
                .append("- Generate EXACTLY ").append(blueprint.size()).append(" questions matching blueprint items 1 through ").append(blueprint.size()).append(" in exact sequential order.\n")
                .append("- Question 1 MUST match blueprint item 1, Question 2 MUST match blueprint item 2, ..., Question ").append(blueprint.size()).append(" MUST match blueprint item ").append(blueprint.size()).append(".\n")
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

        int maxRetries = 2;
        String lastError = "Groq API returned empty or invalid batch output";

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            System.out.println("========== GROQ BATCH REQUEST ==========");
            System.out.println("Purpose: DIAGNOSTIC_BATCH_10");
            System.out.println("Subject: " + subject);
            System.out.println("Model Configured: llama-3.3-70b-versatile");
            System.out.println("Requested Blueprint Size: " + blueprint.size());
            System.out.println("Attempt: " + attempt + " of " + maxRetries);
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

                BatchParseResult parseResult = parseBatchQuestionsResult(rawResponse, subject, blueprint);

                System.out.println("========== GROQ BATCH RESPONSE ==========");
                System.out.println("Groq response received (Attempt " + attempt + ")");
                System.out.println("Response Structure: " + (rawResponse != null ? (rawResponse.trim().startsWith("[") ? "DIRECT_JSON_ARRAY" : "JSON_OBJECT") : "NULL"));
                System.out.println("Parsed question count: " + parseResult.getQuestions().size());
                System.out.println("Validation result: " + (parseResult.isSuccess() ? "SUCCESS" : "FAILURE (" + parseResult.getErrorReason() + ")"));
                System.out.println("=========================================");

                if (parseResult.isSuccess()) {
                    List<QuizQuestion> savedBatch = questionRepository.saveAll(parseResult.getQuestions());
                    return savedBatch;
                } else {
                    lastError = parseResult.getErrorReason();
                }
            } catch (Exception ex) {
                lastError = ex.getMessage();
                System.err.println("[QuizGenerationService] Batch generation attempt " + attempt + " failed: " + lastError);
                if (lastError != null && lastError.contains("daily token quota")) throw ex;
            }
        }

        throw new IllegalStateException("Groq API 10-question batch generation failed after " + maxRetries + " attempts. Last error: " + lastError);
    }

    private BatchParseResult parseBatchQuestionsResult(String rawJson, String subject, List<QuestionBlueprintSpec> blueprint) {
        if (rawJson == null || rawJson.isBlank()) {
            return new BatchParseResult("Groq returned null or blank response");
        }

        String cleanJson = stripMarkdownFences(rawJson);
        if (cleanJson.isBlank()) {
            return new BatchParseResult("Stripped response content was empty");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(cleanJson);
        } catch (Exception e) {
            String excerpt = rawJson.length() > 150 ? rawJson.substring(0, 150) + "..." : rawJson;
            return new BatchParseResult("Invalid JSON syntax: " + e.getMessage() + " (Raw excerpt: " + excerpt + ")");
        }

        if (root.has("success") && !root.get("success").asBoolean(true)) {
            String errType = root.path("error").path("type").asText("UNKNOWN");
            String errMsg = root.path("error").path("message").asText("Groq error payload");
            return new BatchParseResult("Groq API error [" + errType + "]: " + errMsg);
        }

        JsonNode questionsNode = null;
        if (root.isArray()) {
            questionsNode = root;
        } else if (root.isObject()) {
            if (root.has("questions") && root.get("questions").isArray()) {
                questionsNode = root.get("questions");
            } else if (root.has("quiz") && root.get("quiz").isArray()) {
                questionsNode = root.get("quiz");
            } else if (root.has("data") && root.get("data").isArray()) {
                questionsNode = root.get("data");
            } else if (root.has("items") && root.get("items").isArray()) {
                questionsNode = root.get("items");
            } else if (root.has("questionList") && root.get("questionList").isArray()) {
                questionsNode = root.get("questionList");
            } else {
                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    if (entry.getValue().isArray()) {
                        questionsNode = entry.getValue();
                        break;
                    }
                }
            }
        }

        if (questionsNode == null || !questionsNode.isArray()) {
            return new BatchParseResult("Wrong response structure: Expected JSON array or object containing 'questions' array, got root shape: " + root.getNodeType());
        }

        int expectedCount = blueprint != null ? blueprint.size() : 10;
        if (questionsNode.size() != expectedCount) {
            return new BatchParseResult("Wrong question count: Groq returned " + questionsNode.size() + " questions, expected exactly " + expectedCount);
        }

        List<QuizQuestion> result = new ArrayList<>();
        Set<String> seenTexts = new HashSet<>();

        for (int i = 0; i < questionsNode.size(); i++) {
            JsonNode qNode = questionsNode.get(i);
            QuestionBlueprintSpec spec = (blueprint != null && i < blueprint.size()) ? blueprint.get(i) : null;

            String questionText = "";
            if (qNode.has("questionText")) questionText = qNode.path("questionText").asText().trim();
            else if (qNode.has("question")) questionText = qNode.path("question").asText().trim();
            else if (qNode.has("text")) questionText = qNode.path("text").asText().trim();

            if (questionText.isEmpty()) {
                return new BatchParseResult("Missing required field 'questionText' in question #" + (i + 1));
            }

            if (seenTexts.contains(questionText.toLowerCase())) {
                return new BatchParseResult("Duplicate question text in question #" + (i + 1) + ": " + questionText);
            }
            seenTexts.add(questionText.toLowerCase());

            JsonNode optionsNode = null;
            if (qNode.has("options") && qNode.get("options").isArray()) optionsNode = qNode.get("options");
            else if (qNode.has("choices") && qNode.get("choices").isArray()) optionsNode = qNode.get("choices");
            else if (qNode.has("answers") && qNode.get("answers").isArray()) optionsNode = qNode.get("answers");

            if (optionsNode == null) {
                return new BatchParseResult("Missing required array 'options' in question #" + (i + 1));
            }

            List<String> options = new ArrayList<>();
            optionsNode.forEach(opt -> options.add(opt.asText().trim()));
            if (options.size() != 4) {
                return new BatchParseResult("Invalid options count in question #" + (i + 1) + ": expected 4, got " + options.size());
            }

            int correctIdx = 0;
            if (qNode.has("correctOptionIndex")) correctIdx = qNode.path("correctOptionIndex").asInt(0);
            else if (qNode.has("correct_option_index")) correctIdx = qNode.path("correct_option_index").asInt(0);
            else if (qNode.has("correctIndex")) correctIdx = qNode.path("correctIndex").asInt(0);
            else if (qNode.has("answerIndex")) correctIdx = qNode.path("answerIndex").asInt(0);
            else if (qNode.has("correctOption")) correctIdx = qNode.path("correctOption").asInt(0);

            if (correctIdx < 0 || correctIdx > 3) correctIdx = 0;

            String conceptName = (spec != null) ? spec.getConcept() : qNode.path("concept").asText("Core Principle").trim();
            if (conceptName.isEmpty()) conceptName = "Core Principle";

            String explanation = "";
            if (qNode.has("conceptualExplanation")) explanation = qNode.path("conceptualExplanation").asText().trim();
            else if (qNode.has("explanation")) explanation = qNode.path("explanation").asText().trim();
            else if (qNode.has("reasoning")) explanation = qNode.path("reasoning").asText().trim();

            if (explanation.isEmpty()) {
                explanation = "Conceptual explanation for " + conceptName;
            }

            QuizQuestion.Difficulty diff = (spec != null) ? spec.getDifficulty() : QuizQuestion.Difficulty.MEDIUM;

            QuizQuestion question = QuizQuestion.builder()
                    .subject(subject)
                    .concept(conceptName)
                    .difficulty(diff)
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

        return new BatchParseResult(result);
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

        String cleanJson = stripMarkdownFences(rawJson);
        if (cleanJson.isBlank()) return result;

        try {
            JsonNode root = objectMapper.readTree(cleanJson);

            if (root.has("success") && !root.get("success").asBoolean(true)) {
                System.err.println("[QuizGenerationService] Groq returned an error payload: " + root.toString());
                return result;
            }

            JsonNode questionsNode = null;
            if (root.isArray()) {
                questionsNode = root;
            } else if (root.isObject()) {
                if (root.has("questions") && root.get("questions").isArray()) {
                    questionsNode = root.get("questions");
                } else if (root.has("quiz") && root.get("quiz").isArray()) {
                    questionsNode = root.get("quiz");
                } else if (root.has("data") && root.get("data").isArray()) {
                    questionsNode = root.get("data");
                } else if (root.has("items") && root.get("items").isArray()) {
                    questionsNode = root.get("items");
                } else {
                    Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        if (entry.getValue().isArray()) {
                            questionsNode = entry.getValue();
                            break;
                        }
                    }
                }
            }

            if (questionsNode == null || !questionsNode.isArray() || questionsNode.isEmpty()) {
                System.err.println("[QuizGenerationService] No valid 'questions' array in Groq response: " + cleanJson);
                return result;
            }

            for (JsonNode q : questionsNode) {
                String questionText = "";
                if (q.has("questionText")) questionText = q.path("questionText").asText().trim();
                else if (q.has("question")) questionText = q.path("question").asText().trim();
                else if (q.has("text")) questionText = q.path("text").asText().trim();

                if (questionText.isEmpty()) continue;

                JsonNode optionsNode = null;
                if (q.has("options") && q.get("options").isArray()) optionsNode = q.get("options");
                else if (q.has("choices") && q.get("choices").isArray()) optionsNode = q.get("choices");
                else if (q.has("answers") && q.get("answers").isArray()) optionsNode = q.get("answers");

                if (optionsNode == null) continue;

                List<String> options = new ArrayList<>();
                optionsNode.forEach(opt -> options.add(opt.asText().trim()));

                if (options.size() != 4) continue;

                int correctIdx = 0;
                if (q.has("correctOptionIndex")) correctIdx = q.path("correctOptionIndex").asInt(0);
                else if (q.has("correct_option_index")) correctIdx = q.path("correct_option_index").asInt(0);
                else if (q.has("correctIndex")) correctIdx = q.path("correctIndex").asInt(0);

                QuizQuestion question = QuizQuestion.builder()
                        .subject(subject)
                        .concept(q.path("concept").asText(subject + " Core").trim())
                        .difficulty(difficulty)
                        .questionText(questionText)
                        .options(options)
                        .correctOptionIndex(correctIdx)
                        .conceptualExplanation(q.path("conceptualExplanation").asText("Conceptual explanation for " + subject).trim())
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
