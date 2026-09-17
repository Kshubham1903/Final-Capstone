package com.edupilot.controller;

import com.edupilot.model.StudentProfile;
import com.edupilot.model.LifestyleData;
import com.edupilot.model.LifestyleQuestionnaire;
import com.edupilot.model.OnboardingStatus;
import com.edupilot.service.StudentService;
import com.edupilot.service.AiServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private com.edupilot.service.StudentStateService studentStateService;

    @Autowired
    private com.edupilot.service.LearningGainService learningGainService;

    @Autowired
    private com.edupilot.service.StudentStateSnapshotService studentStateSnapshotService;

    @Autowired
    private com.edupilot.service.EvaluationService evaluationService;

    @Autowired
    private com.edupilot.repository.StudentSatisfactionRepository satisfactionRepository;

    @Autowired
    private AiServiceClient aiServiceClient;

    @GetMapping("/onboarding-status/{userId}")
    public ResponseEntity<?> getOnboardingStatus(@PathVariable String userId) {
        try {
            Map<String, Object> status = studentService.checkOnboardingStatus(userId);
            return ResponseEntity.ok(status);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/onboard/step")
    public ResponseEntity<?> saveOnboardingStep(@RequestBody Map<String, Object> payload) {
        try {
            String userId = (String) payload.get("userId");
            int step = ((Number) payload.get("step")).intValue();
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            OnboardingStatus updatedStatus = studentService.saveOnboardingStep(userId, step, data);
            return ResponseEntity.ok(updatedStatus);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/onboard")
    public ResponseEntity<?> onboard(@RequestBody Map<String, Object> payload) {
        try {
            String userId = (String) payload.get("userId");
            String course = payload.containsKey("course") ? (String) payload.get("course") : "Computer Science & Engineering";
            int semester = payload.containsKey("semester") ? ((Number) payload.get("semester")).intValue() : 1;
            List<String> subjects = payload.containsKey("subjects") ? (List<String>) payload.get("subjects") : List.of("Data Structures & Algorithms", "Database Management Systems", "Artificial Intelligence");
            List<String> goals = payload.containsKey("careerGoals") ? (List<String>) payload.get("careerGoals") : List.of("Software Engineer");
            double preferredHours = payload.containsKey("preferredStudyHoursPerDay") ? ((Number) payload.get("preferredStudyHoursPerDay")).doubleValue() : 4.0;
            double targetCgpa = payload.containsKey("targetCgpa") ? ((Number) payload.get("targetCgpa")).doubleValue() : 8.5;

            double sleepHours = payload.containsKey("sleepHours") ? ((Number) payload.get("sleepHours")).doubleValue() : 7.5;
            double stressLevel = payload.containsKey("stressLevel") ? ((Number) payload.get("stressLevel")).doubleValue() : 5.0;
            int exerciseMinutes = payload.containsKey("exerciseMinutes") ? ((Number) payload.get("exerciseMinutes")).intValue() : 30;
            String learningStyle = payload.containsKey("learningStyle") ? (String) payload.get("learningStyle") : "Visual";

            StudentProfile profile = studentService.onboardStudent(
                    userId, course, semester, subjects, goals, preferredHours, targetCgpa,
                    sleepHours, stressLevel, exerciseMinutes, learningStyle
            );

            return ResponseEntity.ok(profile);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable String userId) {
        try {
            studentService.updateStreak(userId);
            StudentProfile profile = studentService.getProfileByUserId(userId);
            return ResponseEntity.ok(profile);
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/profile-full/{userId}")
    public ResponseEntity<?> getFullProfile(@PathVariable String userId) {
        try {
            studentService.updateStreak(userId);
            Map<String, Object> fullProfile = studentService.getFullProfileByUserId(userId);
            return ResponseEntity.ok(fullProfile);
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<?> updateProfileAndRecalculate(@PathVariable String userId, @RequestBody Map<String, Object> payload) {
        try {
            StudentProfile updated = studentService.updateProfileAndRecalculate(userId, payload);
            return ResponseEntity.ok(updated);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/lifestyle/{profileId}")
    public ResponseEntity<?> updateLifestyle(@PathVariable String profileId, @RequestBody LifestyleData data) {
        try {
            StudentProfile updated = studentService.updateLifestyleAndTriggerAnalytics(profileId, data);
            return ResponseEntity.ok(updated);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/questionnaire/{profileId}")
    public ResponseEntity<?> submitQuestionnaire(@PathVariable String profileId, @RequestBody LifestyleQuestionnaire questionnaire) {
        try {
            StudentProfile updated = studentService.submitQuestionnaireAndRunAnalytics(profileId, questionnaire);
            return ResponseEntity.ok(updated);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/questionnaire/{profileId}")
    public ResponseEntity<?> getQuestionnaire(@PathVariable String profileId) {
        return studentService.getQuestionnaireByProfileId(profileId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/recommendations/{profileId}")
    public ResponseEntity<?> getRecommendations(@PathVariable String profileId) {
        try {
            Optional<StudentProfile> profileOpt = studentService.getProfileById(profileId);
            if (profileOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            StudentProfile profile = profileOpt.get();
            Optional<LifestyleQuestionnaire> questOpt = studentService.getQuestionnaireByProfileId(profileId);
            LifestyleQuestionnaire quest = questOpt.orElse(null);
            
            Map<String, Object> aiResult;
            if (quest != null) {
                aiResult = aiServiceClient.predictStudentDevelopment(profile, quest);
            } else {
                aiResult = aiServiceClient.predictPerformance(profile, null, 0);
            }
            return ResponseEntity.ok(aiResult);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/{userId}/state")
    public ResponseEntity<?> getStudentState(@PathVariable String userId) {
        try {
            com.edupilot.dto.StudentStateResponse state = studentStateService.getStudentState(userId);
            return ResponseEntity.ok(state);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/state/{userId}")
    public ResponseEntity<?> getStudentStateAlias(@PathVariable String userId) {
        return getStudentState(userId);
    }

    @GetMapping("/{userId}/learning-gain")
    public ResponseEntity<?> getLearningGain(@PathVariable String userId) {
        try {
            com.edupilot.dto.LearningGainResponse resp = learningGainService.calculateStudentLearningGain(userId);
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/{userId}/state/history")
    public ResponseEntity<?> getStudentStateHistory(@PathVariable String userId) {
        try {
            List<com.edupilot.dto.StudentStateHistoryDTO> history = studentStateSnapshotService.getStudentStateHistoryDTO(userId);
            return ResponseEntity.ok(history);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/{userId}/evaluation")
    public ResponseEntity<?> getEvaluationMetrics(@PathVariable String userId) {
        try {
            com.edupilot.dto.EvaluationMetricsResponse resp = evaluationService.calculateEvaluationMetrics(userId);
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/{userId}/satisfaction")
    public ResponseEntity<?> submitSatisfaction(
            @PathVariable String userId,
            @RequestBody com.edupilot.dto.SatisfactionRequest request) {
        if (request == null || request.getRating() < 1 || request.getRating() > 5) {
            return ResponseEntity.badRequest().body(Map.of("message", "Rating must be between 1 and 5"));
        }

        String resolvedUserId = studentService.resolveUserId(userId);
        com.edupilot.model.StudentSatisfaction satisfaction = new com.edupilot.model.StudentSatisfaction(
                resolvedUserId,
                request.getRating(),
                request.getFeedbackType(),
                request.getComment()
        );

        com.edupilot.model.StudentSatisfaction saved = satisfactionRepository.save(satisfaction);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
