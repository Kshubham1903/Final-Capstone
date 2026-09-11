package com.edupilot.controller;

import com.edupilot.dto.RoadmapGenerateRequest;
import com.edupilot.dto.StudyResourceDTO;
import com.edupilot.model.SubjectRoadmap;
import com.edupilot.service.RoadmapResourceRankingService;
import com.edupilot.service.RoadmapService;
import com.edupilot.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/roadmaps")
@CrossOrigin(origins = "*")
public class RoadmapController {

    @Autowired
    private RoadmapService roadmapService;

    @Autowired
    private RoadmapResourceRankingService resourceRankingService;

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
        } catch (Exception ignored) {
        }
        return null;
    }

    @GetMapping("/subject/{subjectCode}")
    public ResponseEntity<?> getRoadmapBySubjectCode(
            @PathVariable String subjectCode,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String subjectName) {

        if (subjectCode == null || subjectCode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "subjectCode parameter is required"));
        }

        try {
            String authUserId = getAuthenticatedUserId();
            if (authUserId == null || authUserId.isBlank() || "anonymous_student".equals(authUserId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentication required to access personalized roadmap."));
            }

            SubjectRoadmap roadmap = roadmapService.getOrCreateRoadmap(authUserId, subjectCode.trim().toUpperCase(), subjectName);
            return ResponseEntity.ok(roadmap);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateRoadmap(@RequestBody RoadmapGenerateRequest request) {
        if (request == null || ((request.getSubjectCode() == null || request.getSubjectCode().isBlank())
                && (request.getSubjectName() == null || request.getSubjectName().isBlank()))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Subject identification (subjectCode or subjectName) is required."));
        }

        try {
            String authUserId = getAuthenticatedUserId();
            if (authUserId == null || authUserId.isBlank() || "anonymous_student".equals(authUserId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentication required to generate personalized roadmap."));
            }

            String sCode = (request.getSubjectCode() != null && !request.getSubjectCode().isBlank())
                    ? request.getSubjectCode().trim().toUpperCase()
                    : "CS302";

            SubjectRoadmap roadmap = roadmapService.generateAndSaveRoadmap(authUserId, sCode, request.getSubjectName());
            return ResponseEntity.ok(roadmap);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/subject/{subjectCode}/topic/{conceptId}/resources")
    public ResponseEntity<?> getTopicResources(
            @PathVariable String subjectCode,
            @PathVariable String conceptId,
            @RequestParam(required = false) String conceptName,
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) String userId) {

        try {
            String authUserId = getAuthenticatedUserId();
            if (authUserId == null || authUserId.isBlank() || "anonymous_student".equals(authUserId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Authentication required to access topic resources."));
            }

            String resolvedConceptName = (conceptName != null && !conceptName.isBlank()) 
                    ? conceptName 
                    : conceptId.replace("_", " ");

            String resolvedSubjectName = (subjectName != null && !subjectName.isBlank()) 
                    ? subjectName 
                    : subjectCode;

            StudyResourceDTO rankedResources = resourceRankingService.getRankedResourcesForTopic(
                    authUserId, resolvedSubjectName, resolvedConceptName
            );

            return ResponseEntity.ok(rankedResources);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
