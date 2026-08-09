package com.edupilot.service;

import com.edupilot.dto.*;
import com.edupilot.model.LearningPlan;
import com.edupilot.model.Recommendation;
import com.edupilot.model.StudentProfile;
import com.edupilot.model.StudySession;
import com.edupilot.model.Subject;
import com.edupilot.repository.LearningPlanRepository;
import com.edupilot.repository.RecommendationRepository;
import com.edupilot.repository.StudentProfileRepository;
import com.edupilot.repository.StudySessionRepository;
import com.edupilot.repository.SubjectRepository;
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
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private StudentService studentService;

    private int getPriorityWeight(Recommendation.Priority priority) {
        if (priority == null) return 3;
        switch (priority) {
            case CRITICAL: return 1;
            case HIGH: return 2;
            case MEDIUM: return 3;
            case LOW: return 4;
            default: return 3;
        }
    }

    /**
     * Generate adaptive Learning Plan dynamically derived from Recommendation Engine outputs.
     */
    public LearningPlanResponse generateLearningPlan(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        LocalDate today = LocalDate.now();

        List<Recommendation> recs = recommendationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, Recommendation.Status.ACTIVE);
        
        // Sort recommendations by priority (CRITICAL -> HIGH -> MEDIUM -> LOW)
        recs.sort(Comparator.comparingInt(r -> getPriorityWeight(r.getPriority())));

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

        // Fallback default task derived from student's actual enrolled subjects
        if (todayTasks.isEmpty()) {
            String targetSubjCode = "CS301";
            String targetSubjName = "Data Structures & Algorithms";

            Optional<StudentProfile> profOpt = studentProfileRepository.findByUserId(userId);
            if (profOpt.isEmpty()) {
                profOpt = studentProfileRepository.findById(userId);
            }
            if (profOpt.isPresent()) {
                StudentProfile prof = profOpt.get();
                if (prof.getSubjects() != null && !prof.getSubjects().isEmpty()) {
                    targetSubjName = prof.getSubjects().get(0);
                    Optional<Subject> catOpt = subjectRepository.findBySubjectName(targetSubjName);
                    if (catOpt.isPresent()) {
                        targetSubjCode = catOpt.get().getSubjectCode();
                    }
                } else if (prof.getBranch() != null) {
                    List<Subject> catalogSubjs = subjectRepository.findByBranchAndIsActiveTrue(prof.getBranch());
                    if (!catalogSubjs.isEmpty()) {
                        targetSubjCode = catalogSubjs.get(0).getSubjectCode();
                        targetSubjName = catalogSubjs.get(0).getSubjectName();
                    }
                }
            }

            LearningPlan.LearningTask defaultTask = new LearningPlan.LearningTask();
            defaultTask.setTaskId("task_def_" + targetSubjCode.toLowerCase());
            defaultTask.setSubjectCode(targetSubjCode);
            defaultTask.setSubjectName(targetSubjName);
            defaultTask.setTopic("Initial Diagnostic");
            defaultTask.setConceptName(targetSubjName + " Diagnostic");
            defaultTask.setPriority(Recommendation.Priority.HIGH);
            defaultTask.setEstimatedStudyTimeMinutes(20);
            defaultTask.setRecommendedOrder(1);
            defaultTask.setReason("Baseline diagnostic evaluation required to map conceptual mastery for " + targetSubjName + ".");
            defaultTask.setRecommendedAction("Attempt 5-minute diagnostic assessment for " + targetSubjName + ".");
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
        if (planOpt.isPresent()) {
            LearningPlan plan = planOpt.get();
            // Automatically regenerate legacy hardcoded fallback plans ("task_def_1")
            boolean isLegacyFallback = plan.getTasks() != null && plan.getTasks().stream()
                    .anyMatch(t -> "task_def_1".equals(t.getTaskId()) || "Binary Search Trees".equalsIgnoreCase(t.getConceptName()));
            if (isLegacyFallback) {
                return generateLearningPlan(userId);
            }
            return new LearningPlanResponse(plan);
        }
        return generateLearningPlan(userId);
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
