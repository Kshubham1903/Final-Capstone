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

    public static String normalizeQuestionText(String text) {
        if (text == null) return "";
        return text.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public static String extractSubAspectSignature(String text) {
        if (text == null || text.isBlank()) return "GENERAL";
        String lower = text.toLowerCase();
        if (lower.contains("reverse") || lower.contains("reversing") || lower.contains("pointer manipulation")) return "REVERSAL";
        if (lower.contains("random access") || lower.contains("index i") || lower.contains("by index") || lower.contains("direct indexing")) return "RANDOM_ACCESS";
        if (lower.contains("cache") || lower.contains("prefetch") || lower.contains("locality")) return "CACHE_LOCALITY";
        if (lower.contains("capacity") || lower.contains("resiz") || lower.contains("doubling") || lower.contains("vector")) return "RESIZING";
        if (lower.contains("head") || lower.contains("beginning of") || lower.contains("start of")) return "HEAD_OPERATIONS";
        if (lower.contains("tail") || lower.contains("end of")) return "TAIL_OPERATIONS";
        if (lower.contains("memory allocation") || lower.contains("contiguous") || lower.contains("node pointer") || lower.contains("overhead")) return "MEMORY_STRUCTURE";
        if (lower.contains("use-case") || lower.contains("embedded") || lower.contains("trade-off") || lower.contains("preferred over")) return "PRACTICAL_USE_CASE";
        return "GENERAL_" + Math.abs(lower.hashCode() % 10);
    }

    public static boolean isSemanticallyRedundant(QuizQuestion candidate, List<QuizQuestion> currentResult, int maxAllowedSameSignature) {
        if (candidate == null || candidate.getQuestionText() == null) return false;
        String candSig = extractSubAspectSignature(candidate.getQuestionText());

        int count = 0;
        for (QuizQuestion existing : currentResult) {
            if (existing != null && existing.getQuestionText() != null) {
                if (candSig.equals(extractSubAspectSignature(existing.getQuestionText()))) {
                    count++;
                }
            }
        }
        return count >= maxAllowedSameSignature;
    }

    public List<QuizQuestion> generateForConcept(String subject, String concept, QuizQuestion.Difficulty difficulty, int count) {
        return generateForConcept(subject, concept, difficulty, count, Collections.emptySet());
    }

    public List<QuizQuestion> generateForConcept(String subject, String concept, QuizQuestion.Difficulty difficulty, int count, Collection<String> excludeQuestionIds) {
        if (concept == null || concept.trim().isEmpty()) {
            return generate(subject, difficulty != null ? difficulty : QuizQuestion.Difficulty.MEDIUM, count);
        }
        if (difficulty == null) {
            difficulty = QuizQuestion.Difficulty.MEDIUM;
        }

        int targetCount = count > 0 ? count : 10;
        Set<String> excludedIds = (excludeQuestionIds != null) ? new HashSet<>(excludeQuestionIds) : Collections.emptySet();

        List<QuizQuestion> result = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        Set<String> seenNormalizedTexts = new HashSet<>();

        // 1. Dynamic Groq AI Generation (Batch Mode): Request a batch of diverse questions via Groq
        try {
            Map<String, Object> genContext = new HashMap<>();
            genContext.put("maxTokens", 1500);
            genContext.put("purpose", "TOPIC_MASTERY_BATCH");

            List<String> excludeTexts = new ArrayList<>();
            if (!excludedIds.isEmpty()) {
                try {
                    List<QuizQuestion> pastQs = questionRepository.findAllById(excludedIds);
                    for (QuizQuestion pq : pastQs) {
                        if (pq != null && pq.getQuestionText() != null && !pq.getQuestionText().isBlank()) {
                            excludeTexts.add(pq.getQuestionText());
                        }
                    }
                } catch (Exception ignored) {}
            }
            genContext.put("excludeQuestions", excludeTexts);

            List<QuizQuestion> groqCandidates = generateBatchForConceptViaGroq(subject, concept, difficulty, targetCount + 2, genContext);
            if (groqCandidates != null && !groqCandidates.isEmpty()) {
                for (QuizQuestion q : groqCandidates) {
                    if (q != null && q.getQuestionText() != null && !q.getQuestionText().isBlank()) {
                        String normText = normalizeQuestionText(q.getQuestionText());
                        if (!seenNormalizedTexts.contains(normText) && !isSemanticallyRedundant(q, result, 1)) {
                            q.setConcept(concept);
                            q.setSubject(subject);
                            q.setModuleSource(ModuleType.REMEDIATION);
                            q.setQuestionSource("GROQ_AI_GENERATED");
                            alignQuestionCorrectOptionIndex(q);
                            QuizQuestion savedQ = questionRepository.save(q);
                            result.add(savedQ);
                            if (savedQ.getId() != null) seenIds.add(savedQ.getId());
                            seenNormalizedTexts.add(normText);
                            if (result.size() >= targetCount) {
                                return result;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[generateForConcept] Dynamic Groq batch generation note: " + ex.getMessage());
        }

        if (result.size() >= targetCount) {
            return result;
        }

        // 2. Candidate pool from existing DB matching subject and concept (with strict sub-aspect diversity)
        try {
            List<QuizQuestion> existing = questionRepository.findBySubjectAndConcept(subject.trim(), concept.trim());
            if (existing != null && !existing.isEmpty()) {
                List<QuizQuestion> unusedCandidates = new ArrayList<>();
                List<QuizQuestion> usedCandidates = new ArrayList<>();

                for (QuizQuestion q : existing) {
                    if (q != null && q.getQuestionText() != null && !q.getQuestionText().isBlank() && q.getId() != null) {
                        if (!excludedIds.contains(q.getId())) {
                            unusedCandidates.add(q);
                        } else {
                            usedCandidates.add(q);
                        }
                    }
                }

                Collections.shuffle(unusedCandidates);
                Collections.shuffle(usedCandidates);

                // Add unused candidates with strict semantic diversity filter (max 1 per sub-aspect signature)
                for (QuizQuestion q : unusedCandidates) {
                    String normText = normalizeQuestionText(q.getQuestionText());
                    if (!seenIds.contains(q.getId()) && !seenNormalizedTexts.contains(normText) && !isSemanticallyRedundant(q, result, 1)) {
                        alignQuestionCorrectOptionIndex(q);
                        q.setModuleSource(ModuleType.REMEDIATION);
                        result.add(q);
                        seenIds.add(q.getId());
                        seenNormalizedTexts.add(normText);
                        if (result.size() >= targetCount) {
                            return result;
                        }
                    }
                }

                // If still needed, allow up to 2 per signature for unused candidates
                for (QuizQuestion q : unusedCandidates) {
                    String normText = normalizeQuestionText(q.getQuestionText());
                    if (!seenIds.contains(q.getId()) && !seenNormalizedTexts.contains(normText) && !isSemanticallyRedundant(q, result, 2)) {
                        alignQuestionCorrectOptionIndex(q);
                        q.setModuleSource(ModuleType.REMEDIATION);
                        result.add(q);
                        seenIds.add(q.getId());
                        seenNormalizedTexts.add(normText);
                        if (result.size() >= targetCount) {
                            return result;
                        }
                    }
                }

                // Fallback for pool exhaustion: fill remaining slots from used candidates
                for (QuizQuestion q : usedCandidates) {
                    String normText = normalizeQuestionText(q.getQuestionText());
                    if (!seenIds.contains(q.getId()) && !seenNormalizedTexts.contains(normText)) {
                        alignQuestionCorrectOptionIndex(q);
                        q.setModuleSource(ModuleType.REMEDIATION);
                        result.add(q);
                        seenIds.add(q.getId());
                        seenNormalizedTexts.add(normText);
                        if (result.size() >= targetCount) {
                            return result;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[generateForConcept] Error querying existing questions for concept '" + concept + "': " + ex.getMessage());
        }

        if (result.size() >= targetCount) {
            return result;
        }

        if (result.size() >= targetCount) {
            return result;
        }

        // 3. Fallback for substring concept matches in subject
        try {
            List<QuizQuestion> fallbackSubjectQs = questionRepository.findBySubject(subject.trim());
            if (fallbackSubjectQs != null) {
                String targetConceptClean = concept.trim().toLowerCase();
                for (QuizQuestion q : fallbackSubjectQs) {
                    if (q != null && q.getQuestionText() != null && !q.getQuestionText().isBlank() && q.getId() != null) {
                        String normText = normalizeQuestionText(q.getQuestionText());
                        String qConcept = q.getConcept() != null ? q.getConcept().trim().toLowerCase() : "";
                        if ((qConcept.equals(targetConceptClean) || qConcept.contains(targetConceptClean) || targetConceptClean.contains(qConcept))
                                && !seenIds.contains(q.getId()) && !seenNormalizedTexts.contains(normText)) {
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
                            alignQuestionCorrectOptionIndex(cloneQ);
                            QuizQuestion savedFallback = questionRepository.save(cloneQ);
                            result.add(savedFallback);
                            if (savedFallback.getId() != null) seenIds.add(savedFallback.getId());
                            seenNormalizedTexts.add(normText);
                            if (result.size() >= targetCount) {
                                return result;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[generateForConcept] Error applying fallback questions: " + ex.getMessage());
        }

        // 4. Guaranteed Safety Fallback: Generate synthetic concept questions if both Groq API and DB pools are exhausted
        if (result.size() < targetCount) {
            int missingCount = targetCount - result.size();
            for (int i = 1; i <= missingCount; i++) {
                int qNum = result.size() + 1;
                QuizQuestion syntheticQ = QuizQuestion.builder()
                        .subject(subject)
                        .concept(concept)
                        .difficulty(difficulty)
                        .questionText("Question " + qNum + ": What is a key conceptual property of " + concept + " in " + subject + "?")
                        .options(List.of(
                                "Option A: " + concept + " fundamental concept definition " + qNum,
                                "Option B: Alternative property of " + concept,
                                "Option C: Incorrect assumption regarding " + concept,
                                "Option D: Unrelated operation in " + subject
                        ))
                        .correctOptionIndex(0)
                        .conceptualExplanation("Option A is correct because it directly defines the fundamental property of " + concept + ".")
                        .moduleSource(ModuleType.REMEDIATION)
                        .build();
                syntheticQ.setQuestionSource("REMEDIATION_FALLBACK");
                alignQuestionCorrectOptionIndex(syntheticQ);
                QuizQuestion savedSynthetic = questionRepository.save(syntheticQ);
                result.add(savedSynthetic);
            }
        }

        return result;
    }

    private List<QuizQuestion> generateBatchForConceptViaGroq(String subject, String concept, QuizQuestion.Difficulty difficulty, int count, Map<String, Object> context) {
        String systemPrompt = "You are an expert academic question generator for " + subject + ".\n" +
                "Output ONLY a valid JSON object. Strict double quotes ONLY. Do NOT use markdown fences or commentary.\n" +
                "Output MUST contain a top-level key \"questions\" with an array of EXACTLY " + count + " question objects.";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Generate ").append(count)
                .append(" distinct multiple-choice questions for concept \"").append(concept)
                .append("\" (Subject: \"").append(subject).append("\") at ").append(difficulty.name()).append(" difficulty.\n\n")
                .append("DIVERSITY REQUIREMENT:\n")
                .append("Each of the ").append(count).append(" questions MUST assess a DIFFERENT aspect or knowledge point of ").append(concept)
                .append(". Do NOT generate multiple questions testing the same operation or complexity relationship.\n\n")
                .append("Sub-aspects to cover across questions:\n")
                .append("- Memory allocation & layout (contiguous blocks vs non-contiguous nodes)\n")
                .append("- Random access indexing complexity (O(1) offset vs O(n) traversal)\n")
                .append("- Head insertion complexity (O(1) pointer updates vs O(n) array shifting)\n")
                .append("- List reversal & iterative pointer manipulation\n")
                .append("- Memory overhead per element (data payload vs pointer references)\n")
                .append("- CPU cache locality & memory prefetching\n")
                .append("- Dynamic array resizing & amortized cost\n")
                .append("- Practical use-case trade-offs\n\n");

        List<String> excludeTexts = context != null ? (List<String>) context.get("excludeQuestions") : null;
        if (excludeTexts != null && !excludeTexts.isEmpty()) {
            userPrompt.append("DO NOT generate questions similar to:\n");
            int maxExc = Math.min(excludeTexts.size(), 3);
            for (int i = 0; i < maxExc; i++) {
                String exc = excludeTexts.get(i);
                if (exc != null && !exc.isBlank()) {
                    userPrompt.append("  * ").append(exc.length() > 50 ? exc.substring(0, 50) + "..." : exc).append("\n");
                }
            }
        }

        userPrompt.append("\nRequired JSON Format (strict double quotes ONLY):\n")
                .append("{\n")
                .append("  \"questions\": [\n")
                .append("    {\n")
                .append("      \"concept\": \"").append(concept.replace("\"", "'")).append("\",\n")
                .append("      \"questionText\": \"Clear question text\",\n")
                .append("      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n")
                .append("      \"correctOptionIndex\": 0,\n")
                .append("      \"conceptualExplanation\": \"Brief explanation of why option A is correct\"\n")
                .append("    }\n")
                .append("  ]\n")
                .append("}");

        Map<String, Object> reqContext = context != null ? new HashMap<>(context) : new HashMap<>();
        reqContext.put("maxTokens", 750);

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String rawResponse = groqProvider.generateResponse(systemPrompt, userPrompt.toString(), reqContext);
                if (rawResponse != null && (rawResponse.contains("RATE_LIMIT_TPM") || rawResponse.contains("retryAfterMs="))) {
                    long retryDelayMs = 4000;
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
                    System.err.println("[generateBatchForConceptViaGroq] TPM rate limit hit on attempt " + attempt + ". Waiting " + (retryDelayMs + 1000) + "ms...");
                    if (attempt < maxRetries) {
                        try {
                            Thread.sleep(retryDelayMs + 1000);
                        } catch (InterruptedException ignored) {}
                        continue;
                    }
                }

                List<QuizQuestion> questions = parseQuestions(rawResponse, subject, difficulty);
                if (questions != null && !questions.isEmpty()) {
                    return questions;
                }
            } catch (Exception ex) {
                System.err.println("[generateBatchForConceptViaGroq] Attempt " + attempt + " exception: " + ex.getMessage());
            }
        }
        return Collections.emptyList();
    }

    public static class QuestionBlueprintSpec {
        private int position;
        private String concept;
        private QuizQuestion.Difficulty difficulty;
        private String subAspect;

        public QuestionBlueprintSpec() {}

        public QuestionBlueprintSpec(int position, String concept, QuizQuestion.Difficulty difficulty) {
            this.position = position;
            this.concept = concept;
            this.difficulty = difficulty;
        }

        public QuestionBlueprintSpec(int position, String concept, QuizQuestion.Difficulty difficulty, String subAspect) {
            this.position = position;
            this.concept = concept;
            this.difficulty = difficulty;
            this.subAspect = subAspect;
        }

        public int getPosition() { return position; }
        public void setPosition(int position) { this.position = position; }
        public String getConcept() { return concept; }
        public void setConcept(String concept) { this.concept = concept; }
        public QuizQuestion.Difficulty getDifficulty() { return difficulty; }
        public void setDifficulty(QuizQuestion.Difficulty difficulty) { this.difficulty = difficulty; }
        public String getSubAspect() { return subAspect; }
        public void setSubAspect(String subAspect) { this.subAspect = subAspect; }
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

        int expectedCount = blueprint != null ? blueprint.size() : 25;
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

            String conceptName = (spec != null) ? spec.getConcept() : qNode.path("concept").asText("Core Principle").trim();
            if (conceptName.isEmpty()) conceptName = "Core Principle";

            String explanation = "";
            if (qNode.has("conceptualExplanation")) explanation = qNode.path("conceptualExplanation").asText().trim();
            else if (qNode.has("explanation")) explanation = qNode.path("explanation").asText().trim();
            else if (qNode.has("reasoning")) explanation = qNode.path("reasoning").asText().trim();

            if (explanation.isEmpty() || explanation.length() < 15) {
                return new BatchParseResult("Question #" + (i + 1) + " validation error: Missing or insufficient conceptualExplanation.");
            }

            int correctIdx = extractRawCorrectOptionIndex(qNode, options);
            correctIdx = verifyAndAlignWithExplanation(correctIdx, options, explanation);

            if (correctIdx < 0 || correctIdx > 3) {
                return new BatchParseResult("Question #" + (i + 1) + " validation error: Missing or invalid correctOptionIndex (" + correctIdx + "). Must explicitly be 0, 1, 2, or 3.");
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

                String conceptName = q.has("concept") && !q.path("concept").asText().isBlank() 
                        ? q.path("concept").asText().trim() 
                        : (subject + " Core");

                String explanation = "";
                if (q.has("conceptualExplanation")) explanation = q.path("conceptualExplanation").asText().trim();
                else if (q.has("explanation")) explanation = q.path("explanation").asText().trim();
                else if (q.has("reasoning")) explanation = q.path("reasoning").asText().trim();

                int correctIdx = extractRawCorrectOptionIndex(q, options);
                correctIdx = verifyAndAlignWithExplanation(correctIdx, options, explanation);

                if (correctIdx < 0 || correctIdx > 3) correctIdx = 0;

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

    public static int parseLetterOrTextIndex(String rawVal, List<String> options) {
        if (rawVal == null || rawVal.isBlank() || options == null || options.isEmpty()) return -1;
        String clean = rawVal.trim();

        if (clean.length() == 1) {
            char ch = Character.toUpperCase(clean.charAt(0));
            if (ch >= 'A' && ch < 'A' + options.size()) {
                return ch - 'A';
            }
        }

        Pattern letterPat = Pattern.compile("^(?:option|choice)\\s+([a-d])", Pattern.CASE_INSENSITIVE);
        Matcher m = letterPat.matcher(clean);
        if (m.find()) {
            char ch = Character.toUpperCase(m.group(1).charAt(0));
            if (ch >= 'A' && ch < 'A' + options.size()) {
                return ch - 'A';
            }
        }

        for (int i = 0; i < options.size(); i++) {
            String opt = options.get(i);
            if (opt != null) {
                String cleanOpt = opt.trim();
                if (cleanOpt.equalsIgnoreCase(clean)) {
                    return i;
                }
                String optStripped = cleanOpt.replaceAll("^(?:option\\s+[a-d][:.]?|[a-d][:.]\\s*)", "").trim();
                String rawStripped = clean.replaceAll("^(?:option\\s+[a-d][:.]?|[a-d][:.]\\s*)", "").trim();
                if (!optStripped.isEmpty() && optStripped.equalsIgnoreCase(rawStripped)) {
                    return i;
                }
            }
        }

        return -1;
    }

    public static int extractRawCorrectOptionIndex(JsonNode qNode, List<String> options) {
        if (qNode == null) return -1;

        String rawValStr = "";
        if (qNode.has("correctOptionIndex")) rawValStr = qNode.path("correctOptionIndex").asText().trim();
        else if (qNode.has("correct_option_index")) rawValStr = qNode.path("correct_option_index").asText().trim();
        else if (qNode.has("correctIndex")) rawValStr = qNode.path("correctIndex").asText().trim();
        else if (qNode.has("answerIndex")) rawValStr = qNode.path("answerIndex").asText().trim();
        else if (qNode.has("correctOption")) rawValStr = qNode.path("correctOption").asText().trim();
        else if (qNode.has("correctAnswer")) rawValStr = qNode.path("correctAnswer").asText().trim();
        else if (qNode.has("answer")) rawValStr = qNode.path("answer").asText().trim();

        if (rawValStr.isEmpty()) return -1;

        int letterOrTextIdx = parseLetterOrTextIndex(rawValStr, options);
        if (letterOrTextIdx != -1) {
            return letterOrTextIdx;
        }

        try {
            int num = Integer.parseInt(rawValStr);
            if (options != null && !options.isEmpty()) {
                if (num >= 0 && num < options.size()) {
                    return num;
                } else if (num >= 1 && num <= options.size()) {
                    return num - 1;
                }
            } else {
                if (num >= 0 && num <= 3) return num;
            }
        } catch (NumberFormatException ignored) {}

        return -1;
    }

    public static int scoreComplexityOrder(String opt, String expLower) {
        if (opt == null || expLower == null) return 0;
        String optLower = opt.toLowerCase();
        
        int score = 0;
        
        boolean llOptIsO1 = optLower.contains("linked list: o(1)") || (optLower.contains("linked list") && optLower.contains("o(1)") && optLower.indexOf("o(1)") < (optLower.contains("array") ? optLower.indexOf("array") : 9999));
        boolean llOptIsOn = optLower.contains("linked list: o(n)") || (optLower.contains("linked list") && optLower.contains("o(n)") && optLower.indexOf("o(n)") < (optLower.contains("array") ? optLower.indexOf("array") : 9999));

        boolean arrOptIsO1 = optLower.contains("array: o(1)") || (optLower.contains("array") && optLower.contains("o(1)") && optLower.indexOf("o(1)") > (optLower.contains("linked list") ? optLower.indexOf("linked list") : -1));
        boolean arrOptIsOn = optLower.contains("array: o(n)") || (optLower.contains("array") && optLower.contains("o(n)") && optLower.indexOf("o(n)") > (optLower.contains("linked list") ? optLower.indexOf("linked list") : -1));

        int llExpPos = expLower.indexOf("linked list");
        int arrExpPos = expLower.indexOf("array");

        if (llExpPos != -1) {
            int o1Pos = expLower.indexOf("o(1)", llExpPos);
            int onPos = expLower.indexOf("o(n)", llExpPos);

            if (llOptIsO1 && o1Pos != -1 && (onPos == -1 || o1Pos < onPos)) {
                score += 500;
            }
            if (llOptIsOn && onPos != -1 && (o1Pos == -1 || onPos < o1Pos)) {
                score += 500;
            }
        }

        if (arrExpPos != -1) {
            int o1Pos = expLower.indexOf("o(1)", arrExpPos);
            int onPos = expLower.indexOf("o(n)", arrExpPos);

            if (arrOptIsO1 && o1Pos != -1 && (onPos == -1 || o1Pos < onPos)) {
                score += 500;
            }
            if (arrOptIsOn && onPos != -1 && (onPos == -1 || onPos < o1Pos)) {
                score += 500;
            }
        }

        return score;
    }

    public static int verifyAndAlignWithExplanation(int parsedIdx, List<String> options, String explanation) {
        if (options == null || options.size() != 4) return Math.max(0, parsedIdx);
        
        // Authoritative Index Rule: If parsedIdx is already a valid option index [0, 3], preserve it!
        if (parsedIdx >= 0 && parsedIdx < options.size()) {
            return parsedIdx;
        }

        if (explanation == null || explanation.trim().isEmpty()) {
            return 0;
        }

        String expLower = explanation.toLowerCase();

        // 1. Explicit letter check in explanation (e.g., "Option B is correct", "Option B:") for invalid/missing indices
        Pattern letterPat = Pattern.compile("Option\\s+([A-D])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = letterPat.matcher(explanation);
        if (matcher.find()) {
            char letterChar = Character.toUpperCase(matcher.group(1).charAt(0));
            int letterIdx = letterChar - 'A';
            int matchStart = matcher.start();
            String subStr = expLower.substring(Math.max(0, matchStart - 20), Math.min(expLower.length(), matchStart + 40));
            if (!subStr.contains("false") && !subStr.contains("incorrect") && !subStr.contains("not correct") && !subStr.contains("wrong")) {
                if (letterIdx >= 0 && letterIdx < options.size()) {
                    return letterIdx;
                }
            }
        }

        // 2. Complexity order scoring and full phrase matching (fallback repair for missing/invalid indices)
        int bestIdx = 0;
        int maxScore = -1;
        int[] scores = new int[options.size()];

        for (int i = 0; i < options.size(); i++) {
            String opt = options.get(i);
            if (opt == null) continue;
            String cleanOpt = opt.toLowerCase().replaceAll("^(?:option\\s+[a-d][:.]?|[a-d][:.]\\s*)", "").trim();
            
            int s = 0;
            if (cleanOpt.length() >= 8 && expLower.contains(cleanOpt)) {
                s += 1000 + cleanOpt.length();
            }
            s += scoreComplexityOrder(opt, expLower);
            scores[i] = s;
            if (s > maxScore) {
                maxScore = s;
                bestIdx = i;
            }
        }

        if (maxScore > 0) {
            return bestIdx;
        }

        // 3. Default fallback
        return 0;
    }

    public static QuizQuestion alignQuestionCorrectOptionIndex(QuizQuestion q) {
        if (q != null && q.getOptions() != null && !q.getOptions().isEmpty()) {
            int alignedIdx = verifyAndAlignWithExplanation(q.getCorrectOptionIndex(), q.getOptions(), q.getConceptualExplanation());
            q.setCorrectOptionIndex(alignedIdx);
        }
        return q;
    }
}
