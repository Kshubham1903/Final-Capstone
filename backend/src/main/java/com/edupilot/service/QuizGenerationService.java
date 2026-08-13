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
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(subject, difficulty, count);

        String rawResponse = groqProvider.generateResponse(systemPrompt, userPrompt, Map.of());
        List<QuizQuestion> questions = parseQuestions(rawResponse, subject, difficulty);

        if (questions.isEmpty()) {
            System.out.println("[QuizGenerationService] Groq response was empty or rate-limited for subject '" + subject + "'. Using fallback questions.");
            questions = createFallbackQuestions(subject, difficulty, count);
        }

        if (!questions.isEmpty()) {
            questionRepository.saveAll(questions);
        }
        return questions;
    }

    public List<QuizQuestion> generateForConcept(String subject, String concept, QuizQuestion.Difficulty difficulty, int count) {
        if (concept == null || concept.trim().isEmpty()) {
            return generate(subject, difficulty != null ? difficulty : QuizQuestion.Difficulty.MEDIUM, count);
        }
        if (difficulty == null) {
            difficulty = QuizQuestion.Difficulty.MEDIUM;
        }

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPromptForConcept(subject, concept, difficulty, count);

        String rawResponse = groqProvider.generateResponse(systemPrompt, userPrompt, Map.of());
        List<QuizQuestion> questions = parseQuestions(rawResponse, subject, difficulty);

        for (QuizQuestion q : questions) {
            q.setConcept(concept);
        }

        if (questions.isEmpty()) {
            System.out.println("[QuizGenerationService] Groq response empty for concept '" + concept + "'. Using concept fallback.");
            questions = createFallbackConceptQuestions(subject, concept, difficulty, count);
        }

        if (!questions.isEmpty()) {
            questionRepository.saveAll(questions);
        }
        return questions;
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

    private List<QuizQuestion> createFallbackConceptQuestions(String subject, String concept, QuizQuestion.Difficulty difficulty, int count) {
        List<QuizQuestion> fallbackList = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            QuizQuestion q = QuizQuestion.builder()
                    .subject(subject)
                    .concept(concept)
                    .difficulty(difficulty)
                    .questionText("Regarding key principles of " + concept + " in " + subject + ": Which statement best describes core mechanics (" + difficulty.name() + " #" + i + ")?")
                    .options(List.of(
                            concept + " provides structural integrity and correctness for " + subject + ".",
                            concept + " defaults to unencrypted plain-text network broadcasts.",
                            concept + " requires manual physical hardware resets on execution.",
                            concept + " bypasses all internal validation checks."
                    ))
                    .correctOptionIndex(0)
                    .conceptualExplanation("Correct: " + concept + " ensures proper execution in " + subject + ".")
                    .build();
            q.setQuestionSource("STATIC_CONCEPT_FALLBACK");
            fallbackList.add(q);
        }
        return fallbackList;
    }

    private List<QuizQuestion> createFallbackQuestions(String subject, QuizQuestion.Difficulty difficulty, int count) {
        List<QuizQuestion> fallbackList = new ArrayList<>();
        String[] defaultConcepts = {"Core Principles", "System Design", "Operational Best Practices", "Data Management", "Architecture"};
        for (int i = 1; i <= count; i++) {
            String concept = defaultConcepts[(i - 1) % defaultConcepts.length];
            QuizQuestion q = QuizQuestion.builder()
                    .subject(subject)
                    .concept(concept)
                    .difficulty(difficulty)
                    .questionText("Regarding fundamental principles of " + subject + ": Which statement best describes key concepts of " + concept + " (" + difficulty.name() + " #" + i + ")?")
                    .options(List.of(
                            concept + " enforces structural integrity and efficiency in " + subject + ".",
                            concept + " handles unencrypted network transport defaults.",
                            concept + " forces mandatory system restart on every execution.",
                            concept + " bypasses query optimization layers."
                    ))
                    .correctOptionIndex(0)
                    .conceptualExplanation(concept + " provides essential architecture for " + subject + ".")
                    .build();
            q.setQuestionSource("STATIC_FALLBACK");
            fallbackList.add(q);
        }
        return fallbackList;
    }

    private String buildSystemPrompt() {
        return "You are a quiz question generator for an academic learning platform. " +
               "You always respond with a single valid JSON object and nothing else - " +
               "no markdown, no code fences, no explanation outside the JSON.";
    }

    private String buildUserPrompt(String subject, QuizQuestion.Difficulty difficulty, int count) {
        return "Generate " + count + " multiple-choice quiz questions for the subject \"" + subject +
               "\" at " + difficulty.name() + " difficulty level.\n\n" +
               "Requirements:\n" +
               "- Each question must test genuine conceptual understanding, not trivia.\n" +
               "- Exactly 4 answer options per question, only one correct.\n" +
               "- Include a short explanation of why the correct answer is correct.\n" +
               "- Vary the concepts covered - do not repeat the same concept twice.\n\n" +
               "Respond with JSON in exactly this shape:\n" +
               "{\"questions\": [{" +
               "\"concept\": \"short concept name\", " +
               "\"questionText\": \"the question\", " +
               "\"options\": [\"option A\", \"option B\", \"option C\", \"option D\"], " +
               "\"correctOptionIndex\": 0, " +
               "\"conceptualExplanation\": \"why this answer is correct\"" +
               "}]}";
    }

    private List<QuizQuestion> parseQuestions(String rawJson, String subject, QuizQuestion.Difficulty difficulty) {
        List<QuizQuestion> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            if (root.has("success") && !root.get("success").asBoolean(true)) {
                System.err.println("[QuizGenerationService] Groq returned an error: " + root.toString());
                return result;
            }

            JsonNode questionsNode = root.get("questions");
            if (questionsNode == null || !questionsNode.isArray()) {
                System.err.println("[QuizGenerationService] No 'questions' array in Groq response: " + rawJson);
                return result;
            }

            for (JsonNode q : questionsNode) {
                List<String> options = new ArrayList<>();
                q.get("options").forEach(opt -> options.add(opt.asText()));

                if (options.size() != 4) continue;

                QuizQuestion question = QuizQuestion.builder()
                        .subject(subject)
                        .concept(q.path("concept").asText("General"))
                        .difficulty(difficulty)
                        .questionText(q.path("questionText").asText())
                        .options(options)
                        .correctOptionIndex(q.path("correctOptionIndex").asInt(0))
                        .conceptualExplanation(q.path("conceptualExplanation").asText())
                        .build();

                question.setQuestionSource("GROQ_AI_GENERATED");
                question.setGenerationVersion(3);
                result.add(question);
            }
        } catch (Exception e) {
            System.err.println("[QuizGenerationService] Failed to parse Groq response: " + e.getMessage());
            System.err.println("[QuizGenerationService] Raw response was: " + rawJson);
        }
        return result;
    }
}
