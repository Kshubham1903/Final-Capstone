package com.edupilot.controller;

import com.edupilot.dto.StudyResourceDTO;
import com.edupilot.service.StudyResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/study-resources")
@CrossOrigin(origins = "*")
public class StudyResourceController {

    @Autowired
    private StudyResourceService studyResourceService;

    @GetMapping
    public ResponseEntity<?> getStudyResources(
            @RequestParam(value = "subject", required = false, defaultValue = "") String subject,
            @RequestParam(value = "concept", required = false) String concept
    ) {
        if (concept == null || concept.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "concept query parameter is required and cannot be blank"
            ));
        }

        try {
            StudyResourceDTO resources = studyResourceService.getStudyResources(subject.trim(), concept.trim());
            return ResponseEntity.ok(resources);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to discover study resources: " + e.getMessage()));
        }
    }
}
