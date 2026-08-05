package com.edupilot.controller;

import com.edupilot.dto.RecommendationResponse;
import com.edupilot.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<RecommendationResponse>> getActiveRecommendations(@PathVariable String userId) {
        return ResponseEntity.ok(recommendationService.getActiveRecommendations(userId));
    }

    @GetMapping("/high-priority/{userId}")
    public ResponseEntity<List<RecommendationResponse>> getHighPriorityRecommendations(@PathVariable String userId) {
        return ResponseEntity.ok(recommendationService.getHighPriorityRecommendations(userId));
    }

    @PostMapping("/regenerate/{userId}")
    public ResponseEntity<List<RecommendationResponse>> regenerateRecommendations(@PathVariable String userId) {
        return ResponseEntity.ok(recommendationService.generateRecommendations(userId));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<?> completeRecommendation(@PathVariable String id) {
        try {
            RecommendationResponse response = recommendationService.completeRecommendation(id);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
