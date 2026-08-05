package com.edupilot.controller;

import com.edupilot.dto.*;
import com.edupilot.service.LearningPlannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planner")
@CrossOrigin(origins = "*")
public class LearningPlannerController {

    @Autowired
    private LearningPlannerService plannerService;

    @GetMapping("/today/{userId}")
    public ResponseEntity<LearningPlanResponse> getTodayPlan(@PathVariable String userId) {
        return ResponseEntity.ok(plannerService.getTodayPlan(userId));
    }

    @GetMapping("/week/{userId}")
    public ResponseEntity<List<LearningPlanResponse>> getWeekPlan(@PathVariable String userId) {
        return ResponseEntity.ok(plannerService.getWeekPlan(userId));
    }

    @PostMapping("/regenerate/{userId}")
    public ResponseEntity<LearningPlanResponse> regeneratePlan(@PathVariable String userId) {
        return ResponseEntity.ok(plannerService.generateLearningPlan(userId));
    }

    @PatchMapping("/task/{taskId}/complete")
    public ResponseEntity<?> completeTask(@PathVariable String taskId, @RequestParam String userId) {
        try {
            LearningPlanResponse response = plannerService.completeTask(userId, taskId);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/session/start")
    public ResponseEntity<StudySessionResponse> startStudySession(@RequestBody StudySessionStartRequest req) {
        return ResponseEntity.ok(plannerService.startStudySession(req));
    }

    @PostMapping("/session/end")
    public ResponseEntity<StudySessionResponse> endStudySession(@RequestBody StudySessionEndRequest req) {
        return ResponseEntity.ok(plannerService.endStudySession(req));
    }
}
