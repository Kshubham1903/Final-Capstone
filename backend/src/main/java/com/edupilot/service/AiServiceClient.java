package com.edupilot.service;

import com.edupilot.model.StudentProfile;
import com.edupilot.model.LifestyleData;
import com.edupilot.model.LifestyleQuestionnaire;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceClient {

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> predictPerformance(StudentProfile profile, LifestyleData lifestyle, int quizCount) {
        String endpoint = aiServiceUrl + "/api/ai/predict-performance";
        
        // Construct Request Payload
        Map<String, Object> request = new HashMap<>();
        request.put("student_id", profile.getId());
        request.put("target_cgpa", profile.getTargetCgpa());
        request.put("current_cgpa", profile.getTargetCgpa() - 0.5); // Mock current gpa
        request.put("concept_mastery", profile.getConceptMastery());
        request.put("completed_quizzes_count", quizCount);
        
        Map<String, Object> lifestyleMap = new HashMap<>();
        lifestyleMap.put("sleep_hours", lifestyle != null ? lifestyle.getSleepHours() : 7.0);
        lifestyleMap.put("screen_time_hours", lifestyle != null ? lifestyle.getScreenTimeHours() : 4.0);
        lifestyleMap.put("stress_level", lifestyle != null ? lifestyle.getStressLevel() : 5);
        lifestyleMap.put("exercise_minutes", lifestyle != null ? lifestyle.getExerciseMinutes() : 20.0);
        lifestyleMap.put("study_minutes", lifestyle != null ? lifestyle.getStudyMinutes() : 180.0);
        lifestyleMap.put("attendance_rate", lifestyle != null ? lifestyle.getAttendanceRate() : 85.0);
        lifestyleMap.put("productivity_rating", lifestyle != null ? lifestyle.getProductivityRating() : 7.0);
        
        request.put("lifestyle", lifestyleMap);

        try {
            return restTemplate.postForObject(endpoint, request, Map.class);
        } catch (Exception ex) {
            // Fallback rule-based calculations if AI service is offline
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("predicted_cgpa", profile.getTargetCgpa() - 0.1);
            fallback.put("academic_risk_level", "LOW");
            fallback.put("student_growth_index", 7.2);
            fallback.put("improvement_rate", 0.05);
            fallback.put("insights", List.of(
                "[AI Fallback] Maintain structured study times to ensure conceptual clarity.",
                "[AI Fallback] Consider boosting physical exercise to mitigate stress peaks."
            ));
            return fallback;
        }
    }

    public Map<String, Object> predictStudentDevelopment(StudentProfile profile, LifestyleQuestionnaire questionnaire) {
        String endpoint = aiServiceUrl + "/api/ai/predict-student-development";
        
        Map<String, Object> request = new HashMap<>();
        request.put("student_id", profile.getId());
        request.put("hours_studied", questionnaire.getHoursStudied());
        request.put("attendance", questionnaire.getAttendance());
        request.put("parental_involvement", questionnaire.getParentalInvolvement());
        request.put("access_to_resources", questionnaire.getAccessToResources());
        request.put("extracurricular_activities", questionnaire.getExtracurricularActivities());
        request.put("sleep_hours", questionnaire.getSleepHours());
        request.put("previous_scores", questionnaire.getPreviousScores());
        request.put("motivation_level", questionnaire.getMotivationLevel());
        request.put("internet_access", questionnaire.getInternetAccess());
        request.put("tutoring_sessions", questionnaire.getTutoringSessions());
        request.put("family_income", questionnaire.getFamilyIncome());
        request.put("teacher_quality", questionnaire.getTeacherQuality());
        request.put("school_type", questionnaire.getSchoolType());
        request.put("peer_influence", questionnaire.getPeerInfluence());
        request.put("physical_activity", questionnaire.getPhysicalActivity());
        request.put("learning_disabilities", questionnaire.getLearningDisabilities());
        request.put("parental_education_level", questionnaire.getParentalEducationLevel());
        request.put("distance_from_home", questionnaire.getDistanceFromHome());
        request.put("gender", questionnaire.getGender());

        try {
            return restTemplate.postForObject(endpoint, request, Map.class);
        } catch (Exception ex) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("predicted_performance_level", questionnaire.getPreviousScores() >= 75 ? "High" : questionnaire.getPreviousScores() >= 50 ? "Medium" : "Low");
            fallback.put("predicted_cgpa", Math.min(10.0, 5.0 + (questionnaire.getPreviousScores() / 100.0) * 4.5));
            fallback.put("academic_risk_level", questionnaire.getPreviousScores() >= 75 ? "LOW" : questionnaire.getPreviousScores() >= 50 ? "MEDIUM" : "HIGH");
            
            double attendanceFactor = questionnaire.getAttendance() / 100.0;
            double studyFactor = Math.min(questionnaire.getHoursStudied() / 40.0, 1.0);
            double sleepFactor = Math.max(0, 1.0 - Math.abs(questionnaire.getSleepHours() - 8.0) * 0.15);
            double composite = (questionnaire.getPreviousScores() * 0.40) + (attendanceFactor * 25) + (studyFactor * 20) + (sleepFactor * 15);
            fallback.put("student_growth_index", Math.round((composite / 10.0) * 100.0) / 100.0);
            
            fallback.put("consistency_score", questionnaire.getPreviousScores() >= 75 ? 90 : questionnaire.getPreviousScores() >= 50 ? 75 : 50);
            fallback.put("productivity_score", questionnaire.getHoursStudied() >= 20 ? 85 : questionnaire.getHoursStudied() >= 10 ? 70 : 45);
            fallback.put("lifestyle_score", (int) (questionnaire.getSleepHours() * 8 + questionnaire.getPhysicalActivity() * 4));
            
            fallback.put("insights", List.of(
                "[AI Fallback] Increase weekly study hours to reinforce intermediate concepts.",
                "[AI Fallback] Maintain structured sleep timing to reduce cognitive fatigue."
            ));
            return fallback;
        }
    }

    public Map<String, Object> adjustQuizDifficulty(String concept, String currentDifficulty, boolean isCorrect, double responseTimeSeconds) {
        String endpoint = aiServiceUrl + "/api/ai/adaptive-quiz";
        
        Map<String, Object> request = new HashMap<>();
        request.put("concept", concept);
        request.put("current_difficulty", currentDifficulty);
        request.put("is_correct", isCorrect);
        request.put("response_time_seconds", responseTimeSeconds);

        try {
            return restTemplate.postForObject(endpoint, request, Map.class);
        } catch (Exception ex) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("next_difficulty", isCorrect ? "HARD" : "EASY");
            fallback.put("reason", "[AI Fallback] Simple logic fallback response.");
            return fallback;
        }
    }
}

