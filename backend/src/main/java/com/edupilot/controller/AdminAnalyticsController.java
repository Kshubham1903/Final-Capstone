package com.edupilot.controller;

import com.edupilot.dto.AdminAnalyticsOverviewDTO;
import com.edupilot.dto.AdminCohortAnalyticsDTO;
import com.edupilot.dto.AdminResearchTrendsDTO;
import com.edupilot.dto.AdminStudentDirectoryDTO;
import com.edupilot.service.AdminAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AdminAnalyticsOverviewDTO> getOverview() {
        AdminAnalyticsOverviewDTO overview = adminAnalyticsService.getAnalyticsOverview();
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/cohort")
    public ResponseEntity<AdminCohortAnalyticsDTO> getCohortAnalytics() {
        AdminCohortAnalyticsDTO cohort = adminAnalyticsService.getCohortAnalytics();
        return ResponseEntity.ok(cohort);
    }

    @GetMapping("/trends")
    public ResponseEntity<AdminResearchTrendsDTO> getResearchTrends() {
        AdminResearchTrendsDTO trends = adminAnalyticsService.getResearchTrends();
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/students")
    public ResponseEntity<List<AdminStudentDirectoryDTO>> getStudentDirectory() {
        List<AdminStudentDirectoryDTO> directory = adminAnalyticsService.getStudentDirectory();
        return ResponseEntity.ok(directory);
    }

    @GetMapping("/students/{userId}")
    public ResponseEntity<?> getIndividualStudentAnalytics(@PathVariable String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "userId parameter is required and cannot be blank"));
        }
        try {
            com.edupilot.dto.AdminStudentAnalyticsDTO analytics = adminAnalyticsService.getIndividualStudentAnalytics(userId.trim());
            return ResponseEntity.ok(analytics);
        } catch (IllegalArgumentException ex) {
            org.springframework.http.HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")
                    ? org.springframework.http.HttpStatus.NOT_FOUND
                    : org.springframework.http.HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(java.util.Map.of("message", ex.getMessage()));
        }
    }
}
