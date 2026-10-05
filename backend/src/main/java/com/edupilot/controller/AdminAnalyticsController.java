package com.edupilot.controller;

import com.edupilot.dto.AdminAnalyticsOverviewDTO;
import com.edupilot.dto.AdminCohortAnalyticsDTO;
import com.edupilot.service.AdminAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
