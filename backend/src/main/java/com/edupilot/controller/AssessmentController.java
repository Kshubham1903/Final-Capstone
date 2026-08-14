package com.edupilot.controller;

import com.edupilot.dto.*;
import com.edupilot.service.AssessmentService;
import com.edupilot.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    @Autowired
    private StudentService studentService;

    private String getAuthenticatedUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                if (auth.getPrincipal() instanceof UserDetails) {
                    String username = ((UserDetails) auth.getPrincipal()).getUsername();
                    return studentService.resolveUserId(username);
                } else if (auth.getPrincipal() instanceof String) {
                    return studentService.resolveUserId((String) auth.getPrincipal());
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<Map<String, Object>>> getAvailableAssessmentSubjects(
            @RequestParam(required = false) String branch,
            @RequestParam(required = false, defaultValue = "0") int semester) {
        return ResponseEntity.ok(assessmentService.getAvailableAssessmentSubjects(branch, semester));
    }

    @PostMapping("/start")
    public ResponseEntity<?> startAssessmentSession(@RequestBody AssessmentStartRequest request) {
        try {
            String authUserId = getAuthenticatedUserId();
            if (authUserId != null && !authUserId.isBlank() && !"anonymous_student".equals(authUserId)) {
                request.setUserId(authUserId);
            } else if (request.getUserId() != null) {
                request.setUserId(studentService.resolveUserId(request.getUserId()));
            }
            AssessmentSessionResponse response = assessmentService.startAssessmentSession(request);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitAssessment(@RequestBody AssessmentSubmissionRequest request) {
        try {
            String authUserId = getAuthenticatedUserId();
            if (authUserId != null && !authUserId.isBlank() && !"anonymous_student".equals(authUserId)) {
                request.setUserId(authUserId);
            } else if (request.getUserId() != null) {
                request.setUserId(studentService.resolveUserId(request.getUserId()));
            }
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
        String resolvedId = studentService.resolveUserId(userId);
        return ResponseEntity.ok(assessmentService.getAssessmentHistoryByUserId(resolvedId));
    }

    @GetMapping("/latest/{userId}")
    public ResponseEntity<?> getLatestAssessmentResultByUserId(@PathVariable String userId) {
        String resolvedId = studentService.resolveUserId(userId);
        Optional<AssessmentResultResponse> resp = assessmentService.getLatestAssessmentResultByUserId(resolvedId);
        if (resp.isPresent()) {
            return ResponseEntity.ok(resp.get());
        }
        return ResponseEntity.notFound().build();
    }

    // =========================================================================
    // PHASE 5: TRUE ONE-BY-ONE ADAPTIVE DIAGNOSTIC ENDPOINTS
    // =========================================================================

    @PostMapping("/adaptive/start")
    public ResponseEntity<?> startAdaptiveSession(@RequestBody AdaptiveAssessmentDTOs.AdaptiveStartRequest request) {
        try {
            String authUserId = getAuthenticatedUserId();
            AdaptiveAssessmentDTOs.AdaptiveStartResponse response = assessmentService.startAdaptiveSession(request, authUserId);
            return ResponseEntity.ok(response);
        } catch (SecurityException secEx) {
            return ResponseEntity.status(403).body(Map.of("message", secEx.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/adaptive/next")
    public ResponseEntity<?> getAdaptiveNextQuestion(@RequestBody AdaptiveAssessmentDTOs.AdaptiveNextRequest request) {
        try {
            String authUserId = getAuthenticatedUserId();
            AdaptiveAssessmentDTOs.AdaptiveNextResponse response = assessmentService.getAdaptiveNextQuestion(request, authUserId);
            return ResponseEntity.ok(response);
        } catch (SecurityException secEx) {
            return ResponseEntity.status(403).body(Map.of("message", secEx.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/adaptive/submit")
    public ResponseEntity<?> submitAdaptiveAnswer(@RequestBody AdaptiveAssessmentDTOs.AdaptiveSubmitRequest request) {
        try {
            String authUserId = getAuthenticatedUserId();
            AdaptiveAssessmentDTOs.AdaptiveSubmitResponse response = assessmentService.submitAdaptiveAnswer(request, authUserId);
            return ResponseEntity.ok(response);
        } catch (SecurityException secEx) {
            return ResponseEntity.status(403).body(Map.of("message", secEx.getMessage()));
        } catch (IllegalStateException stateEx) {
            return ResponseEntity.status(409).body(Map.of("message", stateEx.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    // =========================================================================
    // PHASE 6: TRUE ONE-BY-ONE GROQ INITIAL DIAGNOSTIC ENDPOINTS
    // =========================================================================

    @PostMapping("/initial/next")
    public ResponseEntity<?> getInitialNextQuestion(@RequestBody AdaptiveAssessmentDTOs.AdaptiveNextRequest request) {
        try {
            String authUserId = getAuthenticatedUserId();
            AdaptiveAssessmentDTOs.AdaptiveNextResponse response = assessmentService.getInitialNextQuestion(request, authUserId);
            return ResponseEntity.ok(response);
        } catch (SecurityException secEx) {
            return ResponseEntity.status(403).body(Map.of("message", secEx.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/initial/submit")
    public ResponseEntity<?> submitInitialAnswer(@RequestBody AdaptiveAssessmentDTOs.AdaptiveSubmitRequest request) {
        try {
            String authUserId = getAuthenticatedUserId();
            AdaptiveAssessmentDTOs.AdaptiveSubmitResponse response = assessmentService.submitInitialAnswer(request, authUserId);
            return ResponseEntity.ok(response);
        } catch (SecurityException secEx) {
            return ResponseEntity.status(403).body(Map.of("message", secEx.getMessage()));
        } catch (IllegalStateException stateEx) {
            return ResponseEntity.status(409).body(Map.of("message", stateEx.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}

