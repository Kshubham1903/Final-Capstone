package com.edupilot.service;

import com.edupilot.model.ModuleType;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        if (!context.containsKey("maxTokens")) context.put("maxTokens", 280);
        if (!context.containsKey("purpose")) context.put("purpose", "DASHBOARD_BATCH");

        int maxRetries = 2;
        String lastError = "Groq API response validation failed";
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
                    ModuleType targetSource = ModuleType.PRACTICE;
                    if (callerContext != null && callerContext.containsKey("moduleSource")) {
                        Object srcObj = callerContext.get("moduleSource");
                        if (srcObj instanceof ModuleType) {
                            targetSource = (ModuleType) srcObj;
                        } else if (srcObj instanceof String) {
                            try {
                                targetSource = ModuleType.valueOf(((String) srcObj).toUpperCase());
                            } catch (Exception ignored) {}
                        }
                    }
                    for (QuizQuestion q : questions) {
                        q.setModuleSource(targetSource);
                    }
                    questionRepository.saveAll(questions);
                    return questions;
                }
            } catch (Exception ex) {
                lastError = (ex.getMessage() != null && !ex.getMessage().isBlank()) ? ex.getMessage() : ex.toString();
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
        List<QuizQuestion> result = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        // 1. Try exact match in MongoDB matching subject and concept
        try {
            List<QuizQuestion> existing = questionRepository.findBySubjectAndConcept(subject.trim(), concept.trim());
            if (existing != null) {
                for (QuizQuestion q : existing) {
                    if (q != null && q.getQuestionText() != null && !q.getQuestionText().isBlank() && q.getId() != null) {
                        q.setModuleSource(ModuleType.REMEDIATION);
                        result.add(q);
                        seenIds.add(q.getId());
                        if (result.size() >= targetCount) {
                            return result;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[generateForConcept] Error querying existing questions for concept '" + concept + "': " + ex.getMessage());
        }

        // 2. Try case-insensitive / substring match from subject questions
        try {
            if (result.size() < targetCount) {
                List<QuizQuestion> subjectQs = questionRepository.findBySubject(subject.trim());
                if (subjectQs != null) {
                    String cleanTarget = concept.trim().toLowerCase();
                    for (QuizQuestion q : subjectQs) {
                        if (q != null && q.getQuestionText() != null && !q.getQuestionText().isBlank() && q.getId() != null && !seenIds.contains(q.getId())) {
                            String qConcept = q.getConcept() != null ? q.getConcept().trim().toLowerCase() : "";
                            if (qConcept.equals(cleanTarget) || qConcept.contains(cleanTarget) || cleanTarget.contains(qConcept)) {
                                q.setModuleSource(ModuleType.REMEDIATION);
                                result.add(q);
                                seenIds.add(q.getId());
                                if (result.size() >= targetCount) {
                                    return result;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[generateForConcept] Error querying substring concept questions: " + ex.getMessage());
        }

        // 3. Generate missing questions 1-by-1 to respect Groq maxTokens (280) and avoid JSON truncation
        int needed = targetCount - result.size();
        List<String> accumulatedExclusions = new ArrayList<>();
        for (QuizQuestion q : result) {
            if (q.getQuestionText() != null) {
                accumulatedExclusions.add(q.getQuestionText());
            }
        }

        Map<String, Object> genContext = new HashMap<>();
        genContext.put("excludeQuestions", accumulatedExclusions);

        String lastError = "Groq API response validation failed";
        for (int i = 0; i < needed; i++) {
            QuestionBlueprintSpec spec = new QuestionBlueprintSpec(result.size() + 1, concept, difficulty);
            Map<String, Object> subContext = new HashMap<>(genContext);
            subContext.put("maxTokens", 280);
            subContext.put("purpose", "CONCEPT_REMEDIATION_QUESTION_" + (result.size() + 1));
            
            try {
                QuizQuestion singleQ = generateSingleQuestionWithRetry(subject, spec, subContext, result.size() + 1, targetCount);
                if (singleQ != null) {
                    singleQ.setConcept(concept);
                    singleQ.setSubject(subject);
                    singleQ.setModuleSource(ModuleType.REMEDIATION);
                    QuizQuestion savedQ = questionRepository.save(singleQ);
                    result.add(savedQ);
                    if (savedQ.getId() != null) seenIds.add(savedQ.getId());
                    if (savedQ.getQuestionText() != null) {
                        accumulatedExclusions.add(savedQ.getQuestionText());
                        genContext.put("excludeQuestions", new ArrayList<>(accumulatedExclusions));
                    }
                }
            } catch (Exception ex) {
                lastError = (ex.getMessage() != null && !ex.getMessage().isBlank()) ? ex.getMessage() : ex.toString();
                System.err.println("[generateForConcept] Groq 1-by-1 generation attempt failed: " + lastError);
            }
        }

        if (result.size() >= targetCount) {
            return result;
        }

        // 4. Emergency fallback: fill remaining slots from existing subject question bank so Remediation Test NEVER fails
        try {
            List<QuizQuestion> fallbackSubjectQs = questionRepository.findBySubject(subject.trim());
            if (fallbackSubjectQs != null) {
                for (QuizQuestion q : fallbackSubjectQs) {
                    if (q != null && q.getQuestionText() != null && !q.getQuestionText().isBlank() && q.getId() != null && !seenIds.contains(q.getId())) {
                        QuizQuestion cloneQ = QuizQuestion.builder()
                                .subject(subject)
                                .concept(concept)
                                .difficulty(difficulty)
                                .questionText(q.getQuestionText())
                                .options(q.getOptions())
                                .correctOptionIndex(q.getCorrectOptionIndex())
                                .conceptualExplanation(q.getConceptualExplanation())
                                .moduleSource(ModuleType.REMEDIATION)
                                .build();
                        cloneQ.setQuestionSource("REMEDIATION_FALLBACK");
                        QuizQuestion savedFallback = questionRepository.save(cloneQ);
                        result.add(savedFallback);
                        seenIds.add(savedFallback.getId());
                        if (result.size() >= targetCount) {
                            return result;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[generateForConcept] Error applying fallback questions: " + ex.getMessage());
        }

        if (!result.isEmpty()) {
            return result;
        }

        throw new IllegalStateException("Groq API question generation failed for concept '" + concept + "'. Last failure: " + lastError);
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

    public QuizQuestion generateSingleDiagnosticQuestion(String subject, QuestionBlueprintSpec spec, Map<String, Object> context, int position, int totalQuestions) {
        Map<String, Object> subContext = context != null ? new HashMap<>(context) : new HashMap<>();
        subContext.put("maxTokens", 280);
        subContext.put("purpose", "DIAGNOSTIC_QUESTION_" + position + "_OF_" + totalQuestions);

        QuizQuestion singleQuestion = generateSingleQuestionWithRetry(subject, spec, subContext, position, totalQuestions);
        if (singleQuestion != null) {
            singleQuestion.setModuleSource(ModuleType.DIAGNOSTIC);
        }

        int promptTok = subContext.containsKey("lastPromptTokens") ? Integer.parseInt(subContext.get("lastPromptTokens").toString()) : 0;
        int compTok = subContext.containsKey("lastCompletionTokens") ? Integer.parseInt(subContext.get("lastCompletionTokens").toString()) : 0;
        int totalTok = subContext.containsKey("lastTotalTokens") ? Integer.parseInt(subContext.get("lastTotalTokens").toString()) : 0;

        System.out.println("[QUESTION GENERATED] position=" + position + "/" + totalQuestions +
                ", questionId=" + (singleQuestion != null ? singleQuestion.getId() : "null") +
                ", concept=" + spec.getConcept() +
                ", difficulty=" + spec.getDifficulty().name() +
                ", questionText=\"" + (singleQuestion != null ? singleQuestion.getQuestionText() : "") + "\"");

        System.out.println("[GroqDiagnostic] Question position: " + position + "/" + totalQuestions +
                " | Concept: " + spec.getConcept() +
                " | Difficulty: " + spec.getDifficulty().name() +
                " | maxTokens: 280" +
                " | Prompt tokens: " + promptTok +
                " | Completion tokens: " + compTok +
                " | Total tokens: " + totalTok);

        return singleQuestion;
    }

    public List<QuizQuestion> generateBatchDiagnosticQuestionsViaGroq(String subject, List<QuestionBlueprintSpec> blueprint, Map<String, Object> context) {
        if (blueprint == null || blueprint.isEmpty()) {
            throw new IllegalArgumentException("Blueprint cannot be null or empty");
        }

        Map<String, Object> genContext = context != null ? new HashMap<>(context) : new HashMap<>();
        int totalQuestions = blueprint.size();

        List<QuizQuestion> finalQuestions = new ArrayList<>();
        List<String> accumulatedExclusions = new ArrayList<>();

        if (genContext.containsKey("excludeQuestions")) {
            List<String> callerExclusions = (List<String>) genContext.get("excludeQuestions");
            if (callerExclusions != null) {
                accumulatedExclusions.addAll(callerExclusions);
            }
        }

        System.out.println("========== GROQ DIAGNOSTIC GENERATION (1 Q PER REQUEST) ==========");
        System.out.println("Subject: " + subject);
        System.out.println("Model Configured: " + groqProvider.getProviderName());
        System.out.println("Total Blueprint Size: " + totalQuestions);
        System.out.println("====================================================================");

        for (int i = 0; i < totalQuestions; i++) {
            QuestionBlueprintSpec spec = blueprint.get(i);

            Map<String, Object> subContext = new HashMap<>(genContext);
            subContext.put("excludeQuestions", new ArrayList<>(accumulatedExclusions));

            QuizQuestion singleQuestion = generateSingleDiagnosticQuestion(subject, spec, subContext, i + 1, totalQuestions);
            if (singleQuestion != null) {
                singleQuestion.setModuleSource(ModuleType.ADAPTIVE);
                finalQuestions.add(singleQuestion);
                if (singleQuestion.getQuestionText() != null && !singleQuestion.getQuestionText().isBlank()) {
                    accumulatedExclusions.add(singleQuestion.getQuestionText());
                }
            }
        }

        if (finalQuestions.size() != blueprint.size()) {
            throw new IllegalStateException("Generated question count (" + finalQuestions.size() + ") does not match requested blueprint size (" + blueprint.size() + ").");
        }

        List<QuizQuestion> savedQuestions = questionRepository.saveAll(finalQuestions);
        return savedQuestions;
    }

    private QuizQuestion generateSingleQuestionWithRetry(String subject, QuestionBlueprintSpec spec, Map<String, Object> subContext, int position, int totalQuestions) {
        String systemPrompt = "You are an expert academic assessment question generator for " + subject + ".\n" +
                "CRITICAL INSTRUCTIONS:\n" +
                "1. You MUST respond with ONLY a single valid JSON object. Do NOT include markdown code blocks (such as ```json), preambles, or commentary.\n" +
                "2. All keys and string values MUST use strict double quotes (\"). NEVER use single quotes (') or unescaped control characters.\n" +
                "3. Ensure all brackets, braces, and double quotes are perfectly closed and valid RFC-8259 syntax.\n" +
                "4. Output MUST contain a top-level key \"questions\" with an array of EXACTLY 1 question object matching the requested blueprint.";

        StringBuilder baseUserPrompt = new StringBuilder();
        baseUserPrompt.append("Generate EXACTLY 1 multiple-choice diagnostic question for subject \"").append(subject)
                .append("\" strictly following the question blueprint specification below.\n\n")
                .append("QUESTION BLUEPRINT:\n")
                .append("Position: ").append(spec.getPosition())
                .append(", Concept: \"").append(spec.getConcept())
                .append("\", Difficulty: ").append(spec.getDifficulty().name()).append("\n\n")
                .append("Requirements:\n")
                .append("- Generate EXACTLY 1 question matching the assigned concept and difficulty.\n")
                .append("- Test genuine conceptual understanding of ").append(spec.getConcept()).append(".\n")
                .append("- Exactly 4 distinct answer options, with only one correct option.\n")
                .append("- Include correctOptionIndex (0, 1, 2, or 3).\n")
                .append("- Include a brief conceptual explanation of why the correct answer is correct.\n")
                .append("- Keep question text concise and conceptual explanation brief (1-2 sentences max).\n")
                .append("- Do NOT duplicate existing questions.\n");

        List<String> excludeTexts = (List<String>) subContext.get("excludeQuestions");
        if (excludeTexts != null && !excludeTexts.isEmpty()) {
            List<String> clean = new ArrayList<>();
            for (String exc : excludeTexts) {
                if (exc != null && !exc.isBlank() && !exc.startsWith("fp_")) {
                    clean.add(exc);
                }
            }
            List<String> recentExclusions = clean.size() > 6 ? clean.subList(clean.size() - 6, clean.size()) : clean;
            if (!recentExclusions.isEmpty()) {
                baseUserPrompt.append("- DO NOT generate questions similar to these existing questions:\n");
                for (String exc : recentExclusions) {
                    String shortExc = exc.length() > 60 ? exc.substring(0, 60) + "..." : exc;
                    baseUserPrompt.append("  * ").append(shortExc).append("\n");
                }
            }
        }

        baseUserPrompt.append("\nRequired JSON Format (strict double quotes ONLY):\n")
                .append("{\n")
                .append("  \"questions\": [\n")
                .append("    {\n")
                .append("      \"concept\": \"").append(spec.getConcept().replace("\"", "'")).append("\",\n")
                .append("      \"questionText\": \"Clear conceptual question text here\",\n")
                .append("      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n")
                .append("      \"correctOptionIndex\": 0,\n")
                .append("      \"conceptualExplanation\": \"Explanation of why option A is correct\"\n")
                .append("    }\n")
                .append("  ]\n")
                .append("}");

        int maxRetries = 3;
        String lastError = "Groq API returned invalid output";

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            System.out.println("========== GROQ QUESTION REQUEST [" + position + "/" + totalQuestions + "] ==========");
            System.out.println("Question Position: " + position + "/" + totalQuestions);
            System.out.println("Concept: " + spec.getConcept());
            System.out.println("Difficulty: " + spec.getDifficulty().name());
            System.out.println("Attempt: " + attempt + " of " + maxRetries);
            System.out.println("maxTokens: 280");
            System.out.println("=============================================================");

            StringBuilder currentPrompt = new StringBuilder(baseUserPrompt);
            if (attempt > 1) {
                currentPrompt.append("\n\nSTRICT JSON RETRY NOTICE (Attempt ").append(attempt).append(" of ").append(maxRetries).append("):\n")
                        .append("Your previous output failed validation (Reason: ").append(lastError).append("). Output strictly valid RFC-8259 JSON containing an array of exactly 1 question matching the blueprint.");
            }

            try {
                String rawResponse = groqProvider.generateResponse(systemPrompt, currentPrompt.toString(), subContext);

                if (rawResponse != null && (rawResponse.contains("RATE_LIMIT_TPD") || rawResponse.contains("daily token quota"))) {
                    System.err.println("[QuizGenerationService] Groq Daily Quota Exceeded (TPD). Halting retries.");
                    throw new IllegalStateException("Groq daily token quota (TPD) reached. Diagnostic assessment generation halted. Please try again after quota resets.");
                }

                if (rawResponse != null && (rawResponse.contains("RATE_LIMIT_TPM") || rawResponse.contains("retryAfterMs="))) {
                    long retryDelayMs = 3000;
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
                        System.err.println("[QuizGenerationService] Groq TPD delay detected (" + retryDelayMs + "ms). Halting retries.");
                        throw new IllegalStateException("Groq daily token quota (TPD) reached. Diagnostic assessment generation halted. Please try again after quota resets.");
                    }

                    long waitMs = retryDelayMs;
                    System.err.println("[QuizGenerationService] Question " + position + "/" + totalQuestions + " hit Groq TPM rate limit (429). Server requested retryAfter: " + retryDelayMs + " ms. Waiting " + waitMs + " ms before retrying ONLY failed question (Attempt " + attempt + " of " + maxRetries + ")");
                    lastError = "Groq API TPM rate limit (429) exceeded.";

                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(waitMs);
                        } catch (InterruptedException ignored) {}
                        continue;
                    }
                }

                List<QuestionBlueprintSpec> singleSpecList = List.of(spec);
                BatchParseResult parseResult = parseBatchQuestionsResult(rawResponse, subject, singleSpecList);
                if (parseResult.isSuccess()) {
                    return parseResult.getQuestions().get(0);
                } else {
                    lastError = parseResult.getErrorReason();
                    System.err.println("[QuizGenerationService] Question " + position + "/" + totalQuestions + " validation failed (Attempt " + attempt + "): " + lastError);
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {}
                    }
                }
            } catch (Exception ex) {
                lastError = (ex.getMessage() != null && !ex.getMessage().isBlank()) ? ex.getMessage() : ex.toString();
                System.err.println("[QuizGenerationService] Question " + position + "/" + totalQuestions + " attempt " + attempt + " exception: " + lastError);
                if (lastError != null && lastError.contains("daily token quota")) throw ex;
            }
        }

        throw new IllegalStateException("Groq API question " + position + " of " + totalQuestions + " (" + spec.getConcept() + ", " + spec.getDifficulty() + ") failed after " + maxRetries + " attempts. Last error: " + lastError);
    }

    public String validateTextIntegrity(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return fieldName + " is missing or blank.";
        }
        String trimmed = text.trim();
        if (trimmed.length() < 15) {
            return fieldName + " is too short (minimum 15 characters required, got " + trimmed.length() + ").";
        }

        Pattern malformedPattern = Pattern.compile("\\b\\?\\b|\\?\\s*=|\\?\\s*_|\\?\\s*\\(|\\?\\s*\\)");
        if (malformedPattern.matcher(trimmed).find()) {
            return fieldName + " contains malformed '?' character replacing mathematical/physics symbols or variables: \"" + trimmed + "\"";
        }

        Pattern truncatedPattern = Pattern.compile("(?:\\.\\.\\.|\\b(?:and|or|the|is|are|with|in|of|for|to|a|an))\\s*$", Pattern.CASE_INSENSITIVE);
        if (truncatedPattern.matcher(trimmed).find()) {
            return fieldName + " appears to be truncated or incomplete at sentence end: \"" + trimmed + "\"";
        }

        return null;
    }

    public String validatePhysicsRules(String questionText, List<String> options, int correctIdx, String explanation) {
        String fullContent = (questionText + " " + explanation).toLowerCase();

        if (fullContent.contains("total internal reflection")) {
            boolean claimsLowerToHigher = fullContent.contains("lower to higher") || 
                                          fullContent.contains("rarer to denser") || 
                                          fullContent.contains("optically rarer to optically denser") ||
                                          fullContent.contains("lower refractive index to higher");
            if (claimsLowerToHigher) {
                return "Contradictory Physics Rule: Total Internal Reflection requires light to travel from a denser (higher refractive index) to a rarer (lower refractive index) medium. Text claims lower to higher/rarer to denser.";
            }
        }

        return null;
    }

    public String validateDataStructureRules(String questionText, List<String> options, int correctIdx, String explanation) {
        String fullContent = (questionText + " " + explanation).toLowerCase();
        String correctOption = (options != null && correctIdx >= 0 && correctIdx < options.size()) ? options.get(correctIdx).toLowerCase() : "";

        if ((fullContent.contains("singly linked list") || fullContent.contains("linked list")) && 
            (fullContent.contains("head") || fullContent.contains("beginning") || fullContent.contains("start"))) {
            if (fullContent.contains("insert") || fullContent.contains("add")) {
                if (correctOption.contains("o(n)") && !correctOption.contains("o(1)")) {
                    return "Contradictory Data Structure Rule: Inserting at the head of a singly linked list is O(1) time complexity, but correct option specifies O(n).";
                }
            }
        }

        if (fullContent.contains("array") && (fullContent.contains("random access") || fullContent.contains("access by index"))) {
            if (correctOption.contains("o(n)") && !correctOption.contains("o(1)")) {
                return "Contradictory Data Structure Rule: Random access in an array is O(1) time complexity, but correct option specifies O(n).";
            }
        }

        return null;
    }

    public String validateQuestionIntegrity(QuizQuestion q, QuestionBlueprintSpec spec) {
        if (q == null) return "Question object is null.";

        String qTextErr = validateTextIntegrity(q.getQuestionText(), "questionText");
        if (qTextErr != null) return qTextErr;

        String expErr = validateTextIntegrity(q.getConceptualExplanation(), "conceptualExplanation");
        if (expErr != null) return expErr;

        List<String> opts = q.getOptions();
        if (opts == null || opts.size() != 4) {
            return "Options array must contain exactly 4 choices (found " + (opts == null ? 0 : opts.size()) + ").";
        }

        Set<String> uniqueOpts = new HashSet<>();
        for (String opt : opts) {
            if (opt == null || opt.isBlank()) return "Option text cannot be empty or null.";
            String optErr = validateTextIntegrity(opt, "option");
            if (optErr != null && optErr.contains("malformed")) return optErr;
            uniqueOpts.add(opt.trim().toLowerCase());
        }
        if (uniqueOpts.size() != 4) {
            return "Options must contain 4 distinct choices (found duplicates). Options: " + opts;
        }

        int correctIdx = q.getCorrectOptionIndex();
        if (correctIdx < 0 || correctIdx > 3) {
            return "correctOptionIndex must explicitly be an integer between 0 and 3 (got " + correctIdx + ").";
        }

        String inconsistency = validateExplanationConsistency(opts, correctIdx, q.getConceptualExplanation());
        if (inconsistency != null) return inconsistency;

        String physErr = validatePhysicsRules(q.getQuestionText(), opts, correctIdx, q.getConceptualExplanation());
        if (physErr != null) return physErr;

        String dsErr = validateDataStructureRules(q.getQuestionText(), opts, correctIdx, q.getConceptualExplanation());
        if (dsErr != null) return dsErr;

        return null;
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
            String errType = root.has("errorType") ? root.path("errorType").asText("UNKNOWN")
                    : (root.has("error") && root.get("error").has("type") ? root.path("error").path("type").asText("UNKNOWN") : "UNKNOWN");
            String errMsg = root.has("message") ? root.path("message").asText("Groq error payload")
                    : (root.has("error") && root.get("error").has("message") ? root.path("error").path("message").asText("Groq error payload") : "Groq error payload");
            String suggestion = root.has("suggestion") ? root.path("suggestion").asText("") : "";
            return new BatchParseResult("Groq API error [" + errType + "]: " + errMsg + (suggestion.isBlank() ? "" : " Suggestion: " + suggestion));
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

            int correctIdx = -1;
            if (qNode.has("correctOptionIndex")) correctIdx = qNode.path("correctOptionIndex").asInt(-1);
            else if (qNode.has("correct_option_index")) correctIdx = qNode.path("correct_option_index").asInt(-1);
            else if (qNode.has("correctIndex")) correctIdx = qNode.path("correctIndex").asInt(-1);
            else if (qNode.has("answerIndex")) correctIdx = qNode.path("answerIndex").asInt(-1);
            else if (qNode.has("correctOption")) correctIdx = qNode.path("correctOption").asInt(-1);

            if (qNode.has("correctAnswer") || qNode.has("answer")) {
                String ansStr = qNode.has("correctAnswer") ? qNode.path("correctAnswer").asText().trim() : qNode.path("answer").asText().trim();
                if (!ansStr.isEmpty()) {
                    for (int optI = 0; optI < options.size(); optI++) {
                        if (options.get(optI).equalsIgnoreCase(ansStr)) {
                            correctIdx = optI;
                            break;
                        }
                    }
                }
            }

            if (correctIdx < 0 || correctIdx > 3) {
                return new BatchParseResult("Question #" + (i + 1) + " validation error: Missing or invalid correctOptionIndex (" + correctIdx + "). Must explicitly be 0, 1, 2, or 3.");
            }

            String conceptName = (spec != null) ? spec.getConcept() : qNode.path("concept").asText("Core Principle").trim();
            if (conceptName.isEmpty()) conceptName = "Core Principle";

            String explanation = "";
            if (qNode.has("conceptualExplanation")) explanation = qNode.path("conceptualExplanation").asText().trim();
            else if (qNode.has("explanation")) explanation = qNode.path("explanation").asText().trim();
            else if (qNode.has("reasoning")) explanation = qNode.path("reasoning").asText().trim();

            if (explanation.isEmpty() || explanation.length() < 15) {
                return new BatchParseResult("Question #" + (i + 1) + " validation error: Missing or insufficient conceptualExplanation.");
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

            String validationError = validateQuestionIntegrity(question, spec);
            if (validationError != null) {
                return new BatchParseResult("Question #" + (i + 1) + " validation error: " + validationError);
            }

            question.setQuestionSource("GROQ_DIAGNOSTIC_BATCH");
            question.setGenerationVersion(3);
            String fp = "fp_" + Math.abs(question.getQuestionText().hashCode());
            question.setQuestionFingerprint(fp);

            result.add(question);
        }

        return new BatchParseResult(result);
    }

    private String validateExplanationConsistency(List<String> options, int correctIdx, String explanation) {
        if (explanation == null || explanation.isBlank() || options == null || options.size() != 4 || correctIdx < 0 || correctIdx > 3) {
            return null;
        }

        String expLower = explanation.toLowerCase();
        char correctLetter = (char) ('A' + correctIdx);

        // 1. Check for explicit Option [A-D] letter mentions in explanation
        Pattern letterPattern = Pattern.compile("Option\\s+([A-D])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = letterPattern.matcher(explanation);
        while (matcher.find()) {
            char mentionedLetter = Character.toUpperCase(matcher.group(1).charAt(0));
            int mentionedIdx = mentionedLetter - 'A';
            if (mentionedIdx != correctIdx) {
                String excerpt = explanation.substring(Math.max(0, matcher.start() - 10), Math.min(explanation.length(), matcher.end() + 30));
                if (expLower.contains("correct") || expLower.contains("efficient") || expLower.contains("takes") || expLower.contains("is the")) {
                    return "Explanation states Option " + mentionedLetter + " is correct, but correctOptionIndex is " + correctIdx + " (Option " + correctLetter + "). Excerpt: \"" + excerpt + "\"";
                }
            }
        }

        // 2. Check if explanation text explicitly contains another option's text while omitting correct option key terms
        String correctTextLower = options.get(correctIdx).toLowerCase();
        Set<String> correctWordsSet = new HashSet<>(Arrays.asList(correctTextLower.split("\\s+")));

        for (int i = 0; i < options.size(); i++) {
            if (i == correctIdx) continue;
            String otherTextLower = options.get(i).toLowerCase();

            if (otherTextLower.length() >= 8 && expLower.contains(otherTextLower)) {
                Set<String> otherWordsSet = new HashSet<>(Arrays.asList(otherTextLower.split("\\s+")));
                int distinctCorrectMatches = 0;
                for (String w : correctWordsSet) {
                    if (w.length() > 3 && !otherWordsSet.contains(w) && expLower.contains(w)) {
                        distinctCorrectMatches++;
                    }
                }
                if (distinctCorrectMatches == 0) {
                    return "Explanation describes option " + (char)('A' + i) + " (\"" + options.get(i) + "\") instead of correct option " + correctLetter + " (\"" + options.get(correctIdx) + "\").";
                }
            }
        }

        return null;
    }

    public QuizQuestion generateOneDiagnosticQuestionViaGroq(String subject, String concept, QuizQuestion.Difficulty difficulty, Map<String, Object> context) {
        if (difficulty == null) difficulty = QuizQuestion.Difficulty.MEDIUM;
        if (concept == null || concept.isBlank()) concept = "General Principles";

        Map<String, Object> genContext = context != null ? new HashMap<>(context) : new HashMap<>();
        if (!genContext.containsKey("maxTokens")) genContext.put("maxTokens", 280);
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
                .append("- Include a brief conceptual explanation.\n")
                .append("- Keep question text concise and conceptual explanation brief (1-2 sentences max).\n");

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
                lastError = (ex.getMessage() != null && !ex.getMessage().isBlank()) ? ex.getMessage() : ex.toString();
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

                int correctIdx = -1;
                if (q.has("correctOptionIndex")) correctIdx = q.path("correctOptionIndex").asInt(-1);
                else if (q.has("correct_option_index")) correctIdx = q.path("correct_option_index").asInt(-1);
                else if (q.has("correctIndex")) correctIdx = q.path("correctIndex").asInt(-1);
                else if (q.has("answerIndex")) correctIdx = q.path("answerIndex").asInt(-1);
                else if (q.has("correctOption")) correctIdx = q.path("correctOption").asInt(-1);

                if (q.has("correctAnswer") || q.has("answer")) {
                    String ansStr = q.has("correctAnswer") ? q.path("correctAnswer").asText().trim() : q.path("answer").asText().trim();
                    if (!ansStr.isEmpty()) {
                        for (int optI = 0; optI < options.size(); optI++) {
                            if (options.get(optI).equalsIgnoreCase(ansStr)) {
                                correctIdx = optI;
                                break;
                            }
                        }
                    }
                }

                if (correctIdx == 4) correctIdx = 3;
                if (correctIdx < 0 || correctIdx > 3) correctIdx = 0;

                String conceptName = q.has("concept") && !q.path("concept").asText().isBlank() 
                        ? q.path("concept").asText().trim() 
                        : (subject + " Core");

                String explanation = "";
                if (q.has("conceptualExplanation")) explanation = q.path("conceptualExplanation").asText().trim();
                else if (q.has("explanation")) explanation = q.path("explanation").asText().trim();
                else if (q.has("reasoning")) explanation = q.path("reasoning").asText().trim();

                if (explanation.isEmpty() || explanation.length() < 5) {
                    explanation = "Option " + (char)('A' + correctIdx) + " (\"" + options.get(correctIdx) + "\") is correct for testing " + conceptName + ".";
                }

                QuizQuestion question = QuizQuestion.builder()
                        .subject(subject)
                        .concept(conceptName)
                        .difficulty(difficulty)
                        .questionText(questionText)
                        .options(options)
                        .correctOptionIndex(correctIdx)
                        .conceptualExplanation(explanation)
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
