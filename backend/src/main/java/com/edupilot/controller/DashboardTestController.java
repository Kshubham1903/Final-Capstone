package com.edupilot.controller;

import com.edupilot.dto.DashboardTestResultDTO;
import com.edupilot.dto.DashboardTestSubmissionDTO;
import com.edupilot.service.DashboardKnowledgeTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard-test")
@CrossOrigin(origins = "*")
public class DashboardTestController {

    @Autowired
    private DashboardKnowledgeTestService dashboardTestService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateTest(@RequestBody Map<String, String> payload) {
        if (payload == null || !payload.containsKey("studentId") || payload.get("studentId") == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "studentId is required and cannot be blank"));
        }

        String studentId = payload.get("studentId").trim();
        if (studentId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "studentId is required and cannot be blank"));
        }

        try {
            Map<String, Object> testData = dashboardTestService.generateTestForStudent(studentId);
            return ResponseEntity.ok(testData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to generate dashboard knowledge test: " + e.getMessage()));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitTest(@RequestBody DashboardTestSubmissionDTO submission) {
        if (submission == null || submission.getStudentId() == null || submission.getStudentId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "studentId is required and cannot be blank"));
        }
        if (submission.getSessionId() == null || submission.getSessionId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "sessionId is required and cannot be blank"));
        }

        try {
            DashboardTestResultDTO result = dashboardTestService.gradeSubmission(submission);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to grade dashboard test submission: " + e.getMessage()));
        }
    }

    @GetMapping("/latest-result/{studentId}")
    public ResponseEntity<?> getLatestResult(@PathVariable String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "studentId is required and cannot be blank"));
        }

        try {
            DashboardTestResultDTO result = dashboardTestService.getLatestResult(studentId.trim());
            if (result == null) {
                return ResponseEntity.ok(Map.of("message", "No previous knowledge test results found for student", "hasResults", false));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch latest test result: " + e.getMessage()));
        }
    }
}
