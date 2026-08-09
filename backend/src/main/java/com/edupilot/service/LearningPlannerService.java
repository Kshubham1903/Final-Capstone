package com.edupilot.service;

import com.edupilot.dto.*;
import com.edupilot.model.LearningPlan;
import com.edupilot.model.Recommendation;
import com.edupilot.model.StudySession;
import com.edupilot.repository.LearningPlanRepository;
import com.edupilot.repository.RecommendationRepository;
import com.edupilot.repository.StudySessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LearningPlannerService {

    @Autowired
    private LearningPlanRepository planRepository;

    @Autowired
    private StudySessionRepository sessionRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private StudentService studentService;

    /**
     * Generate adaptive Learning Plan dynamically derived from Recommendation Engine outputs.
     */
    public LearningPlanResponse generateLearningPlan(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        LocalDate today = LocalDate.now();

        List<Recommendation> recs = recommendationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Recommendation.Status.ACTIVE);

        List<LearningPlan.LearningTask> todayTasks = new ArrayList<>();
        int order = 1;
        int totalMinutes = 0;

        for (Recommendation rec : recs) {
            String taskId = "task_" + (rec.getId() != null ? rec.getId() : UUID.randomUUID().toString());
            
            LearningPlan.LearningTask task = new LearningPlan.LearningTask();
            task.setTaskId(taskId);
            task.setSubjectCode(rec.getSubjectCode());
            task.setSubjectName(rec.getSubjectName());
            task.setTopic(rec.getTopic());
            task.setConceptName(rec.getConceptName());
            task.setPriority(rec.getPriority());
            task.setEstimatedStudyTimeMinutes(rec.getEstimatedStudyTimeMinutes() > 0 ? rec.getEstimatedStudyTimeMinutes() : 20);
            task.setRecommendedOrder(order++);
            task.setReason(rec.getReason());
            task.setRecommendedAction(rec.getRecommendedAction());
            task.setStatus(LearningPlan.LearningTask.TaskStatus.PENDING);
            task.setGeneratedFromRecommendationId(rec.getId());

            todayTasks.add(task);
            totalMinutes += task.getEstimatedStudyTimeMinutes();
        }

        // Fallback default task if no recommendations available
        if (todayTasks.isEmpty()) {
            LearningPlan.LearningTask defaultTask = new LearningPlan.LearningTask();
            defaultTask.setTaskId("task_def_1");
            defaultTask.setSubjectCode("CS301");
            defaultTask.setSubjectName("Data Structures & Algorithms");
            defaultTask.setTopic("Binary Search Trees");
            defaultTask.setConceptName("Binary Search Trees");
            defaultTask.setPriority(Recommendation.Priority.HIGH);
            defaultTask.setEstimatedStudyTimeMinutes(20);
            defaultTask.setRecommendedOrder(1);
            defaultTask.setReason("Baseline diagnostic evaluation required to map conceptual mastery.");
            defaultTask.setRecommendedAction("Attempt 5-minute diagnostic assessment for Data Structures.");
            defaultTask.setStatus(LearningPlan.LearningTask.TaskStatus.PENDING);
            todayTasks.add(defaultTask);
            totalMinutes = 20;
        }

        Optional<LearningPlan> existingOpt = planRepository.findByUserIdAndPlanDate(userId, today);
        LearningPlan plan = existingOpt.orElseGet(() -> {
            LearningPlan p = new LearningPlan();
            p.setUserId(userId);
            p.setPlanDate(today);
            p.setDayLabel("TODAY");
            return p;
        });

        plan.setTasks(todayTasks);
        plan.setTotalTasks(todayTasks.size());
        plan.setCompletedTasks((int) todayTasks.stream().filter(t -> t.getStatus() == LearningPlan.LearningTask.TaskStatus.COMPLETED).count());
        plan.setTotalEstimatedMinutes(totalMinutes);
        plan.setCompletionPercentage(plan.getTotalTasks() > 0 ? (plan.getCompletedTasks() * 100.0 / plan.getTotalTasks()) : 0.0);
        plan.setUpdatedAt(LocalDateTime.now());

        LearningPlan savedPlan = planRepository.save(plan);
        return new LearningPlanResponse(savedPlan);
    }

    public LearningPlanResponse getTodayPlan(String userId) {
        LocalDate today = LocalDate.now();
        Optional<LearningPlan> planOpt = planRepository.findByUserIdAndPlanDate(userId, today);
        if (planOpt.isEmpty()) {
            return generateLearningPlan(userId);
        }
        return new LearningPlanResponse(planOpt.get());
    }

    public List<LearningPlanResponse> getWeekPlan(String userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekEnd = today.plusDays(7);
        List<LearningPlan> weekPlans = planRepository.findByUserIdAndPlanDateBetweenOrderByPlanDateAsc(userId, today, weekEnd);
        if (weekPlans.isEmpty()) {
            LearningPlanResponse todayRes = generateLearningPlan(userId);
            return List.of(todayRes);
        }
        return weekPlans.stream().map(LearningPlanResponse::new).collect(Collectors.toList());
    }

    public LearningPlanResponse completeTask(String userId, String taskId) {
        studentService.updateStreak(userId);
        LocalDate today = LocalDate.now();
        LearningPlan plan = planRepository.findByUserIdAndPlanDate(userId, today)
                .orElseGet(() -> planRepository.findByUserIdOrderByPlanDateDesc(userId).stream().findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No learning plan found for user: " + userId)));

        if (plan.getTasks() != null) {
            for (LearningPlan.LearningTask task : plan.getTasks()) {
                if (taskId.equals(task.getTaskId())) {
                    task.setStatus(LearningPlan.LearningTask.TaskStatus.COMPLETED);
                    break;
                }
            }
            int completed = (int) plan.getTasks().stream().filter(t -> t.getStatus() == LearningPlan.LearningTask.TaskStatus.COMPLETED).count();
            plan.setCompletedTasks(completed);
            plan.setCompletionPercentage(plan.getTotalTasks() > 0 ? Math.round((completed * 100.0 / plan.getTotalTasks()) * 10.0) / 10.0 : 0.0);
            plan.setUpdatedAt(LocalDateTime.now());
            planRepository.save(plan);
        }

        return new LearningPlanResponse(plan);
    }

    public StudySessionResponse startStudySession(StudySessionStartRequest req) {
        studentService.updateStreak(req.getUserId());
        StudySession s = new StudySession();
        s.setUserId(req.getUserId());
        s.setTaskId(req.getTaskId());
        s.setSubjectCode(req.getSubjectCode());
        s.setConceptName(req.getConceptName());
        s.setStartTime(LocalDateTime.now());
        s.setStatus(StudySession.SessionStatus.ACTIVE);

        StudySession saved = sessionRepository.save(s);
        return new StudySessionResponse(saved);
    }

    public StudySessionResponse endStudySession(StudySessionEndRequest req) {
        StudySession s = sessionRepository.findById(req.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Study session ID not found: " + req.getSessionId()));

        studentService.updateStreak(s.getUserId());

        s.setEndTime(LocalDateTime.now());
        s.setActualDurationMinutes(req.getActualDurationMinutes() > 0 ? req.getActualDurationMinutes() : 15);
        s.setPausedDurationMinutes(req.getPausedDurationMinutes());
        s.setCompletionNotes(req.getCompletionNotes());
        s.setStatus(StudySession.SessionStatus.COMPLETED);

        StudySession saved = sessionRepository.save(s);

        // Mark associated task completed
        if (s.getUserId() != null && s.getTaskId() != null) {
            try {
                completeTask(s.getUserId(), s.getTaskId());
            } catch (Exception ex) {
                System.err.println("Could not auto-complete task: " + ex.getMessage());
            }
        }

        return new StudySessionResponse(saved);
    }
}
