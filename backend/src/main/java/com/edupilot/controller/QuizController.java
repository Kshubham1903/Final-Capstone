package com.edupilot.controller;

import com.edupilot.model.QuizQuestion;
import com.edupilot.model.StudentProfile;
import com.edupilot.repository.QuizQuestionRepository;
import com.edupilot.repository.StudentProfileRepository;
import com.edupilot.service.AiServiceClient;
import com.edupilot.service.StudentService;
import com.edupilot.service.QuizGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/quizzes")
@CrossOrigin(origins = "*")
public class QuizController {

    @Autowired
    private QuizQuestionRepository questionRepository;

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private QuizGenerationService quizGenerationService;

    @Autowired
    private AiServiceClient aiServiceClient;

    @Autowired
    private StudentService studentService;

    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(
            @RequestParam String subject,
            @RequestParam String difficulty,
            @RequestParam(required = false) String exclude) {
        
        QuizQuestion.Difficulty diff;
        try {
            diff = QuizQuestion.Difficulty.valueOf(difficulty.toUpperCase());
        } catch (Exception ex) {
            diff = QuizQuestion.Difficulty.EASY;
        }

        Set<String> excludedSet = new HashSet<>();
        if (exclude != null && !exclude.isBlank()) {
            for (String raw : exclude.split(",")) {
                String trimmed = raw.trim();
                if (!trimmed.isEmpty()) {
                    excludedSet.add(trimmed);
                    excludedSet.add(AiServiceClient.normalizeText(trimmed));
                }
            }
        }

        List<QuizQuestion> dbQuestions = questionRepository.findBySubjectAndDifficulty(subject, diff);
        List<QuizQuestion> validUnseen = new ArrayList<>();
        List<String> allSubjectExcludeTexts = new ArrayList<>();

        List<QuizQuestion> legacyToClean = new ArrayList<>();
        for (QuizQuestion q : dbQuestions) {
            boolean isLegacy = q.getGenerationVersion() < 3 ||
                               (q.getConcept() != null && q.getConcept().contains("#")) ||
                               (q.getQuestionText() != null && q.getQuestionText().startsWith("Regarding fundamental principles"));
            if (isLegacy) {
                legacyToClean.add(q);
                continue;
            }

            if (!AiServiceClient.isGenericTemplateQuestion(q.getQuestionText())) {
                allSubjectExcludeTexts.add(q.getQuestionText());
                String qId = q.getId();
                String normText = AiServiceClient.normalizeText(q.getQuestionText());
                boolean isExcluded = (qId != null && excludedSet.contains(qId)) || excludedSet.contains(normText);
                if (!isExcluded) {
                    validUnseen.add(q);
                }
            }
        }

        if (!legacyToClean.isEmpty()) {
            try {
                questionRepository.deleteAll(legacyToClean);
            } catch (Exception ex) {
                System.err.println("Note: Could not purge legacy questions: " + ex.getMessage());
            }
        }

        if (validUnseen.size() < 10) {
            List<String> combinedExclusions = new ArrayList<>(allSubjectExcludeTexts);
            combinedExclusions.addAll(excludedSet);

            int maxAttempts = 2;
            for (int attempt = 0; attempt < maxAttempts && validUnseen.size() < 10; attempt++) {
                List<QuizQuestion> aiGenerated = aiServiceClient.generateQuestionsForSubject(subject, diff, combinedExclusions);
                if (aiGenerated != null && !aiGenerated.isEmpty()) {
                    List<QuizQuestion> toSave = new ArrayList<>();
                    for (QuizQuestion g : aiGenerated) {
                        if (!AiServiceClient.isGenericTemplateQuestion(g.getQuestionText())) {
                            String normText = AiServiceClient.normalizeText(g.getQuestionText());
                            boolean isDup = combinedExclusions.stream().anyMatch(e -> AiServiceClient.isDuplicateQuestion(g.getQuestionText(), e));
                            if (!isDup) {
                                toSave.add(g);
                                validUnseen.add(g);
                                combinedExclusions.add(g.getQuestionText());
                            }
                        }
                    }
                    if (!toSave.isEmpty()) {
                        try {
                            questionRepository.saveAll(toSave);
                        } catch (Exception ex) {
                            System.err.println("Failed to persist AI generated questions: " + ex.getMessage());
                        }
                    }
                }
            }
        }

        List<QuizQuestion> finalPool = new ArrayList<>();
        Set<String> seenInPool = new HashSet<>();
        for (QuizQuestion q : validUnseen) {
            String normText = AiServiceClient.normalizeText(q.getQuestionText());
            if (!seenInPool.contains(normText)) {
                seenInPool.add(normText);
                finalPool.add(q);
            }
        }

        Collections.shuffle(finalPool);
        int limit = Math.min(finalPool.size(), 10);
        List<QuizQuestion> selected = finalPool.subList(0, limit);
        List<QuizQuestion> randomizedResponse = new ArrayList<>();
        for (QuizQuestion q : selected) {
            randomizedResponse.add(AiServiceClient.shuffleQuestionOptions(q));
        }
        return ResponseEntity.ok(randomizedResponse);
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitAnswer(@RequestBody Map<String, Object> payload) {
        try {
            String profileId = (String) payload.get("profileId");
            String subject = (String) payload.get("subject");
            String concept = (String) payload.get("concept");
            String difficulty = (String) payload.get("difficulty");
            boolean isCorrect = (boolean) payload.get("isCorrect");
            double responseTimeSeconds = ((Number) payload.get("responseTimeSeconds")).doubleValue();

            // 1. Call AI service to adjust difficulty
            Map<String, Object> aiResult = aiServiceClient.adjustQuizDifficulty(concept, difficulty, isCorrect, responseTimeSeconds);
            String nextDifficulty = (String) aiResult.get("next_difficulty");
            String reason = (String) aiResult.get("reason");

            // 2. Fetch and update student profile
            studentService.updateStreak(profileId);
            StudentProfile profile = studentService.findOrCreateProfile(profileId);
            if (profile != null) {
                
                // Adjust concept mastery
                double masteryChange = isCorrect ? 8.0 : -3.0;
                Map<String, Double> mastery = profile.getConceptMastery();
                if (mastery == null) {
                    mastery = new HashMap<>();
                }
                double currentMastery = mastery.getOrDefault(subject, 50.0);
                double nextMastery = Math.min(Math.max(currentMastery + masteryChange, 0.0), 100.0);
                mastery.put(subject, nextMastery);
                profile.setConceptMastery(mastery);

                // Increment completed quizzes
                profile.setCompletedQuizzesCount(profile.getCompletedQuizzesCount() + 1);

                // Adjust weak/strong concepts
                Map<String, List<String>> weakConcepts = profile.getWeakConcepts();
                if (weakConcepts == null) weakConcepts = new HashMap<>();
                Map<String, List<String>> strongConcepts = profile.getStrongConcepts();
                if (strongConcepts == null) strongConcepts = new HashMap<>();

                List<String> weaks = weakConcepts.get(subject);
                if (weaks == null) {
                    weaks = new ArrayList<>();
                    weakConcepts.put(subject, weaks);
                }
                List<String> strongs = strongConcepts.get(subject);
                if (strongs == null) {
                    strongs = new ArrayList<>();
                    strongConcepts.put(subject, strongs);
                }

                if (isCorrect) {
                    if (!strongs.contains(concept)) {
                        strongs.add(concept);
                    }
                    weaks.remove(concept);
                } else {
                    if (!weaks.contains(concept)) {
                        weaks.add(concept);
                    }
                    strongs.remove(concept);
                }
                profile.setWeakConcepts(weakConcepts);
                profile.setStrongConcepts(strongConcepts);

                // Run background predictor
                studentService.runSilentBackgroundPrediction(profile);
                profileRepository.save(profile);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("nextDifficulty", nextDifficulty);
            response.put("reason", reason);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createQuestion(@RequestBody QuizQuestion question) {
        try {
            if (question.getSubject() == null || question.getConcept() == null || question.getQuestionText() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Core fields cannot be null"));
            }
            QuizQuestion saved = questionRepository.save(question);
            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/generate-ai")
    public ResponseEntity<?> generateAiQuiz(@RequestBody Map<String, Object> request) {
        String studentId = (String) request.get("studentId");
        String subject = (String) request.get("subject");
        int count = request.containsKey("count") ? ((Number) request.get("count")).intValue() : 5;

        if (studentId == null || subject == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "studentId and subject are required."));
        }
        if (count < 1 || count > 20) {
            return ResponseEntity.badRequest().body(Map.of("message", "count must be between 1 and 20."));
        }

        try {
            List<QuizQuestion> questions = quizGenerationService.generateForStudent(studentId, subject, count);
            if (questions.isEmpty()) {
                return ResponseEntity.status(502).body(Map.of(
                    "message", "AI generation returned no valid questions. Check GROQ_API_KEY and try again."
                ));
            }
            return ResponseEntity.ok(Map.of(
                "subject", subject,
                "count", questions.size(),
                "questions", questions
            ));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(404).body(Map.of("message", iae.getMessage()));
        }
    }
}
