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
import java.util.stream.Collectors;

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
            @RequestParam String difficulty,
            @RequestParam(required = false) String exclude,
            @RequestParam(required = false) String targetConcept,
            @RequestParam(required = false) String concept) {
        
        String effectiveTargetConcept = targetConcept != null && !targetConcept.isBlank() ? targetConcept : concept;
        
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

        List<QuizQuestion> dbQuestions = questionRepository.findBySubject(subject);
        List<QuizQuestion> validUnseen = new ArrayList<>();
        List<String> allSubjectExcludeTexts = new ArrayList<>();

        List<QuizQuestion> legacyToClean = new ArrayList<>();
        for (QuizQuestion q : dbQuestions) {
            boolean isLegacy = (q.getGenerationVersion() > 0 && q.getGenerationVersion() < 3);
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

        if (validUnseen.size() < 30) {
            List<String> userExclusions = new ArrayList<>(excludedSet);

            for (QuizQuestion.Difficulty d : List.of(diff, QuizQuestion.Difficulty.MEDIUM, QuizQuestion.Difficulty.HARD, QuizQuestion.Difficulty.EASY)) {
                if (validUnseen.size() >= 40) break;
                List<QuizQuestion> aiGenerated = aiServiceClient.generateQuestionsForSubject(subject, d, userExclusions);
                if (aiGenerated != null && !aiGenerated.isEmpty()) {
                    List<QuizQuestion> toSave = new ArrayList<>();
                    for (QuizQuestion g : aiGenerated) {
                        if (!AiServiceClient.isGenericTemplateQuestion(g.getQuestionText())) {
                            boolean isDupInUnseen = validUnseen.stream().anyMatch(e -> AiServiceClient.isDuplicateQuestion(g.getQuestionText(), e.getQuestionText()));
                            if (!isDupInUnseen) {
                                toSave.add(g);
                                validUnseen.add(g);
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

        List<QuizQuestion> selected = new ArrayList<>();

        if (effectiveTargetConcept != null && !effectiveTargetConcept.isBlank()) {
            // FOCUSED VERIFICATION QUIZ: Filter exclusively to targetConcept
            String normTarget = com.edupilot.service.RecommendationService.normalizeConceptName(effectiveTargetConcept, subject);
            List<QuizQuestion> targetPool = new ArrayList<>();
            for (QuizQuestion q : validUnseen) {
                String normQConcept = com.edupilot.service.RecommendationService.normalizeConceptName(q.getConcept(), subject);
                if (normTarget.equalsIgnoreCase(normQConcept)) {
                    targetPool.add(q);
                }
            }
            if (targetPool.size() < 10) {
                List<QuizQuestion> allSubjectQs = questionRepository.findBySubject(subject);
                for (QuizQuestion q : allSubjectQs) {
                    String normQConcept = com.edupilot.service.RecommendationService.normalizeConceptName(q.getConcept(), subject);
                    if (normTarget.equalsIgnoreCase(normQConcept) && !targetPool.contains(q)) {
                        targetPool.add(q);
                    }
                }
            }
            Collections.shuffle(targetPool);
            int limit = Math.min(targetPool.size(), 10);
            selected = targetPool.subList(0, limit);
        } else {
            // STANDARD ADAPTIVE QUIZ: Select questions from ONLY 2 to 3 normalized concepts (~3 to 5 questions per concept)
            List<QuizQuestion> allSubjectQuestions = questionRepository.findBySubject(subject);
            if (allSubjectQuestions.size() < 10) {
                allSubjectQuestions = validUnseen;
            }

            Map<String, List<QuizQuestion>> fullConceptMap = new LinkedHashMap<>();
            Set<String> seenTexts = new HashSet<>();
            
            for (QuizQuestion q : allSubjectQuestions) {
                if (q.getQuestionText() == null || AiServiceClient.isGenericTemplateQuestion(q.getQuestionText())) continue;
                String normText = AiServiceClient.normalizeText(q.getQuestionText());
                if (seenTexts.contains(normText)) continue;
                seenTexts.add(normText);

                String normConcept = com.edupilot.service.RecommendationService.normalizeConceptName(q.getConcept(), subject);
                fullConceptMap.computeIfAbsent(normConcept, k -> new ArrayList<>()).add(q);
            }

            List<String> multiQuestionConcepts = fullConceptMap.entrySet().stream()
                    .filter(e -> e.getValue().size() >= 2)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<String> availableConcepts = !multiQuestionConcepts.isEmpty() ? multiQuestionConcepts : new ArrayList<>(fullConceptMap.keySet());
            Collections.shuffle(availableConcepts);

            int targetConceptCount = Math.min(availableConcepts.size(), availableConcepts.size() >= 3 ? 3 : (availableConcepts.size() >= 2 ? 2 : 1));
            List<String> chosenConcepts = availableConcepts.subList(0, targetConceptCount);

            List<QuizQuestion> conceptFilteredPool = new ArrayList<>();
            for (String cName : chosenConcepts) {
                List<QuizQuestion> cQuestions = fullConceptMap.get(cName);
                if (cQuestions != null) {
                    int take = Math.min(cQuestions.size(), 5);
                    conceptFilteredPool.addAll(cQuestions.subList(0, take));
                }
            }

            Collections.shuffle(conceptFilteredPool);
            int limit = Math.min(conceptFilteredPool.size(), 10);
            selected = conceptFilteredPool.subList(0, limit);
        }

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

            boolean isVerification = false;
            if (payload.containsKey("isVerification")) {
                isVerification = (boolean) payload.get("isVerification");
            } else if (payload.containsKey("isVerificationQuiz")) {
                isVerification = (boolean) payload.get("isVerificationQuiz");
            }

            String targetConcept = null;
            if (payload.containsKey("targetConcept")) {
                targetConcept = (String) payload.get("targetConcept");
            }

            String questionId = payload.containsKey("questionId") ? (String) payload.get("questionId") : null;
            String questionText = payload.containsKey("questionText") ? (String) payload.get("questionText") : null;

            // 1. Call AI service to adjust difficulty
            Map<String, Object> aiResult = aiServiceClient.adjustQuizDifficulty(concept, difficulty, isCorrect, responseTimeSeconds);
            String nextDifficulty = (String) aiResult.get("next_difficulty");
            String reason = (String) aiResult.get("reason");

            // 2. Fetch and update student profile
            studentService.updateStreak(profileId);
            StudentProfile profile = studentService.findOrCreateProfile(profileId);
            String actualUserId = profile != null && profile.getUserId() != null ? profile.getUserId() : profileId;

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

            // 3. Record/Update ConceptMastery entity, QuizSession, and trigger recommendation engine
            try {
                updateConceptMasteryAndRecommendations(actualUserId, profile != null ? profile.getId() : actualUserId, subject, concept, difficulty, isCorrect, responseTimeSeconds, isVerification, targetConcept, questionId, questionText);
            } catch (Exception ex) {
                System.err.println("Error updating concept mastery from quiz submission: " + ex.getMessage());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("nextDifficulty", nextDifficulty);
            response.put("reason", reason);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @Autowired
    private com.edupilot.repository.ConceptMasteryRepository conceptMasteryRepository;

    @Autowired
    private com.edupilot.repository.QuizSessionRepository quizSessionRepository;

    @Autowired
    private com.edupilot.repository.SubjectRepository subjectRepository;

    @Autowired
    private com.edupilot.service.RecommendationService recommendationService;

    @Autowired
    private com.edupilot.service.LearningPlannerService learningPlannerService;

    private void updateConceptMasteryAndRecommendations(String userId, String studentProfileId, String subjectName, String conceptName, String difficulty, boolean isCorrect, double responseTimeSeconds, boolean isVerification, String targetConcept, String questionId, String questionText) {
        if (userId == null || conceptName == null || conceptName.isBlank()) return;

        String normalizedConcept = com.edupilot.service.RecommendationService.normalizeConceptName(conceptName);

        // Resolve subject code from subject name or fallback
        String subjectCode = "CS301";
        if (subjectName != null && !subjectName.isBlank()) {
            Optional<com.edupilot.model.Subject> subjOpt = subjectRepository.findBySubjectName(subjectName);
            if (subjOpt.isPresent()) {
                subjectCode = subjOpt.get().getSubjectCode();
            } else if (subjectName.contains("Artificial Intelligence")) {
                subjectCode = "AI601";
            } else if (subjectName.contains("Discrete")) {
                subjectCode = "CS303";
            }
        }

        final String resolvedSubjectCode = subjectCode;
        final String resolvedSubjectName = subjectName != null ? subjectName : "Data Structures & Algorithms";

        // 1. Record Quiz Session for session tracking
        try {
            Optional<com.edupilot.model.QuizSession> sessionOpt = quizSessionRepository
                    .findFirstByUserIdAndSubjectNameAndStatusOrderByLastAnswerTimeDesc(userId, resolvedSubjectName, com.edupilot.model.QuizSession.Status.IN_PROGRESS);

            com.edupilot.model.QuizSession session;
            if (sessionOpt.isPresent() && sessionOpt.get().getLastAnswerTime().isAfter(java.time.LocalDateTime.now().minusMinutes(30)) && sessionOpt.get().getAnswers().size() < 10) {
                session = sessionOpt.get();
            } else {
                session = new com.edupilot.model.QuizSession();
                session.setUserId(userId);
                session.setStudentProfileId(studentProfileId);
                session.setSubjectCode(resolvedSubjectCode);
                session.setSubjectName(resolvedSubjectName);
                session.setStatus(com.edupilot.model.QuizSession.Status.IN_PROGRESS);
            }

            if (isVerification) {
                session.setVerificationQuiz(true);
                session.setTargetConcept(targetConcept != null ? targetConcept : normalizedConcept);
            }

            com.edupilot.model.QuizSession.QuizAnswerRecord rec = new com.edupilot.model.QuizSession.QuizAnswerRecord(normalizedConcept, difficulty, isCorrect, responseTimeSeconds);
            rec.setQuestionId(questionId);
            rec.setQuestionText(questionText);
            session.getAnswers().add(rec);
            session.setTotalQuestions(session.getAnswers().size());
            if (isCorrect) {
                session.setCorrectCount(session.getCorrectCount() + 1);
            } else {
                session.setIncorrectCount(session.getIncorrectCount() + 1);
            }
            session.setLastAnswerTime(java.time.LocalDateTime.now());

            if (session.getTotalQuestions() >= 10) {
                session.setStatus(com.edupilot.model.QuizSession.Status.COMPLETED);
            }
            quizSessionRepository.save(session);
        } catch (Exception ex) {
            System.err.println("Failed to record quiz session: " + ex.getMessage());
        }

        String topic = normalizedConcept;
        Optional<com.edupilot.model.ConceptMastery> opt = conceptMasteryRepository.findByUserIdAndSubjectCodeAndTopicAndConceptName(
                userId, subjectCode, topic, normalizedConcept
        );
        if (opt.isEmpty()) {
            // Also try by userId and conceptName across subjects
            List<com.edupilot.model.ConceptMastery> userConcepts = conceptMasteryRepository.findByUserId(userId);
            for (com.edupilot.model.ConceptMastery cm : userConcepts) {
                if (normalizedConcept.equalsIgnoreCase(cm.getConceptName())) {
                    opt = Optional.of(cm);
                    break;
                }
            }
        }

        com.edupilot.model.ConceptMastery cm = opt.orElseGet(() -> {
            com.edupilot.model.ConceptMastery c = new com.edupilot.model.ConceptMastery();
            c.setUserId(userId);
            c.setStudentProfileId(studentProfileId);
            c.setSubjectCode(resolvedSubjectCode);
            c.setSubjectName(resolvedSubjectName);
            c.setTopic(topic);
            c.setConceptName(normalizedConcept);
            return c;
        });

        int attempts = cm.getAttemptCount() + 1;
        int corrects = cm.getCorrectCount() + (isCorrect ? 1 : 0);
        double accuracy = Math.round((corrects * 100.0 / attempts) * 10.0) / 10.0;
        double confidence = Math.min(Math.round((attempts * 20.0 + accuracy * 0.8) * 10.0) / 10.0, 100.0);

        com.edupilot.model.ConceptMastery.MasteryLevel level;
        if (accuracy >= 85.0 && attempts >= 2) {
            level = com.edupilot.model.ConceptMastery.MasteryLevel.MASTER;
        } else if (accuracy >= 70.0) {
            level = com.edupilot.model.ConceptMastery.MasteryLevel.PROFICIENT;
        } else if (accuracy >= 50.0) {
            level = com.edupilot.model.ConceptMastery.MasteryLevel.INTERMEDIATE;
        } else {
            level = com.edupilot.model.ConceptMastery.MasteryLevel.BEGINNER;
        }

        String action;
        if (level == com.edupilot.model.ConceptMastery.MasteryLevel.BEGINNER) {
            action = "Review core concepts and collision/traversal handling for " + conceptName + ".";
        } else if (level == com.edupilot.model.ConceptMastery.MasteryLevel.INTERMEDIATE) {
            action = "Solve medium-difficulty practice questions on " + conceptName + ".";
        } else if (level == com.edupilot.model.ConceptMastery.MasteryLevel.PROFICIENT) {
            action = "Attempt advanced challenge questions for " + conceptName + ".";
        } else {
            action = "Mastery achieved! Maintain proficiency with periodic retention.";
        }

        cm.setAttemptCount(attempts);
        cm.setCorrectCount(corrects);
        cm.setAccuracy(accuracy);
        cm.setConfidenceScore(confidence);
        cm.setMasteryLevel(level);
        cm.setRecommendedAction(action);
        cm.setLastAssessedAt(java.time.LocalDateTime.now());

        conceptMasteryRepository.save(cm);

        if (isVerification || (targetConcept != null && !targetConcept.isBlank())) {
            String activeTarget = targetConcept != null && !targetConcept.isBlank() ? targetConcept : normalizedConcept;
            recommendationService.processVerificationResult(userId, resolvedSubjectName, activeTarget, isCorrect, isCorrect ? 100.0 : 0.0);
            try {
                learningPlannerService.generateLearningPlan(userId);
            } catch (Exception ex) {
                System.err.println("Error refreshing plan after verification: " + ex.getMessage());
            }
        } else {
            try {
                recommendationService.generateRecommendations(userId);
                learningPlannerService.generateLearningPlan(userId);
            } catch (Exception ex) {
                System.err.println("Error regenerating recommendations: " + ex.getMessage());
            }
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
