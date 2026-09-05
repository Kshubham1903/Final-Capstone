package com.edupilot.controller;

import com.edupilot.dto.DashboardTestSubmissionDTO;
import com.edupilot.service.ConceptRemediationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/concept-remediation")
@CrossOrigin(origins = "*")
public class ConceptRemediationController {

    @Autowired
    private ConceptRemediationService remediationService;

    public static class StartRemediationRequest {
        private String studentId;
        private String subject;
        private String concept;

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getConcept() { return concept; }
        public void setConcept(String concept) { this.concept = concept; }
    }

    public static class SubmitRemediationRequest {
        private String studentId;
        private String sessionId;
        private List<DashboardTestSubmissionDTO.AnswerEntry> answers;

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public List<DashboardTestSubmissionDTO.AnswerEntry> getAnswers() { return answers; }
        public void setAnswers(List<DashboardTestSubmissionDTO.AnswerEntry> answers) { this.answers = answers; }
    }

    @PostMapping("/start")
    public ResponseEntity<?> startRemediation(@RequestBody StartRemediationRequest req) {
        if (req == null || req.getStudentId() == null || req.getStudentId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "studentId parameter is required and cannot be blank"));
        }
        if (req.getSubject() == null || req.getSubject().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "subject parameter is required and cannot be blank"));
        }
        if (req.getConcept() == null || req.getConcept().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "concept parameter is required and cannot be blank"));
        }

        try {
            Map<String, Object> result = remediationService.startRemediationTest(
                    req.getStudentId().trim(), 
                    req.getSubject().trim(), 
                    req.getConcept().trim()
            );
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to start concept remediation test: " + e.getMessage()));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitRemediation(@RequestBody SubmitRemediationRequest req) {
        if (req == null || req.getSessionId() == null || req.getSessionId().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "sessionId parameter is required and cannot be blank"));
        }

        try {
            Map<String, Object> result = remediationService.submitRemediationTest(
                    req.getStudentId(), 
                    req.getSessionId().trim(), 
                    req.getAnswers()
            );
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to submit concept remediation test: " + e.getMessage()));
        }
    }
}
