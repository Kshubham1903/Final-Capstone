package com.edupilot.controller;

import com.edupilot.model.QuizQuestion;
import com.edupilot.model.StudentProfile;
import com.edupilot.repository.QuizQuestionRepository;
import com.edupilot.repository.StudentProfileRepository;
import com.edupilot.service.AiServiceClient;
import com.edupilot.service.StudentService;
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
    private AiServiceClient aiServiceClient;

    @Autowired
    private StudentService studentService;

    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(
            @RequestParam String subject,
            @RequestParam String difficulty) {
        
        QuizQuestion.Difficulty diff;
        try {
            diff = QuizQuestion.Difficulty.valueOf(difficulty.toUpperCase());
        } catch (Exception ex) {
            diff = QuizQuestion.Difficulty.EASY;
        }

        List<QuizQuestion> questions = questionRepository.findBySubjectAndDifficulty(subject, diff);
        if (questions.isEmpty()) {
            // Fallback to fetch by subject only
            questions = questionRepository.findBySubject(subject);
        }
        if (questions.isEmpty()) {
            // Fallback to all questions
            questions = questionRepository.findAll();
        }
        
        List<QuizQuestion> mutableQuestions = new ArrayList<>(questions);
        Collections.shuffle(mutableQuestions);
        
        int limit = Math.min(mutableQuestions.size(), 4);
        return ResponseEntity.ok(mutableQuestions.subList(0, limit));
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
}
