package com.edupilot.controller;

import com.edupilot.dto.*;
import com.edupilot.service.AssessmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/assessment")
@CrossOrigin(origins = "*")
public class AssessmentController {

    @Autowired
    private AssessmentService assessmentService;

    @GetMapping("/subjects")
    public ResponseEntity<List<Map<String, Object>>> getAvailableAssessmentSubjects(
            @RequestParam(required = false) String branch,
            @RequestParam(required = false, defaultValue = "0") int semester) {
        return ResponseEntity.ok(assessmentService.getAvailableAssessmentSubjects(branch, semester));
    }

    @PostMapping("/start")
    public ResponseEntity<?> startAssessmentSession(@RequestBody AssessmentStartRequest request) {
        try {
            AssessmentSessionResponse response = assessmentService.startAssessmentSession(request);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitAssessment(@RequestBody AssessmentSubmissionRequest request) {
        try {
            AssessmentResultResponse response = assessmentService.submitAssessment(request);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/result/{id}")
    public ResponseEntity<?> getAssessmentResultById(@PathVariable String id) {
        Optional<AssessmentResultResponse> resp = assessmentService.getAssessmentResultById(id);
        if (resp.isPresent()) {
            return ResponseEntity.ok(resp.get());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<AssessmentResultResponse>> getAssessmentHistoryByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(assessmentService.getAssessmentHistoryByUserId(userId));
    }

    @GetMapping("/latest/{userId}")
    public ResponseEntity<?> getLatestAssessmentResultByUserId(@PathVariable String userId) {
        Optional<AssessmentResultResponse> resp = assessmentService.getLatestAssessmentResultByUserId(userId);
        if (resp.isPresent()) {
            return ResponseEntity.ok(resp.get());
        }
        return ResponseEntity.notFound().build();
    }
}
