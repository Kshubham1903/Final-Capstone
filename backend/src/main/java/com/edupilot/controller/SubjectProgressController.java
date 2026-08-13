package com.edupilot.controller;

import com.edupilot.dto.AttemptMasteryPointDTO;
import com.edupilot.dto.DailyMasteryPointDTO;
import com.edupilot.service.SubjectProgressHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subject-progress")
@CrossOrigin(origins = "*")
public class SubjectProgressController {

    @Autowired
    private SubjectProgressHistoryService progressHistoryService;

    @GetMapping("/{studentId}/{subject}")
    public ResponseEntity<?> getSubjectProgressHistory(
            @PathVariable String studentId,
            @PathVariable String subject,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "perAttempt") String granularity) {

        if (studentId == null || studentId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "studentId parameter is required and cannot be blank"));
        }
        if (subject == null || subject.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "subject parameter is required and cannot be blank"));
        }

        try {
            if ("daily".equalsIgnoreCase(granularity)) {
                List<DailyMasteryPointDTO> history = progressHistoryService.getDailyMasteryHistory(
                        studentId.trim(), 
                        subject.trim(), 
                        days
                );
                return ResponseEntity.ok(history);
            } else {
                List<AttemptMasteryPointDTO> history = progressHistoryService.getPerAttemptMasteryHistory(
                        studentId.trim(), 
                        subject.trim(), 
                        days
                );
                return ResponseEntity.ok(history);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to retrieve subject progress history: " + e.getMessage()));
        }
    }
}
