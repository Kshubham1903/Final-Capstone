package com.edupilot.controller;

import com.edupilot.dto.ConceptMasteryResponse;
import com.edupilot.dto.KnowledgeProfileResponse;
import com.edupilot.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin(origins = "*")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @GetMapping("/profile/{userId}")
    public ResponseEntity<KnowledgeProfileResponse> getKnowledgeProfile(@PathVariable String userId) {
        return ResponseEntity.ok(knowledgeService.getKnowledgeProfile(userId));
    }

    @GetMapping("/weak-concepts/{userId}")
    public ResponseEntity<List<ConceptMasteryResponse>> getWeakConcepts(@PathVariable String userId) {
        return ResponseEntity.ok(knowledgeService.getWeakConcepts(userId));
    }

    @GetMapping("/strong-concepts/{userId}")
    public ResponseEntity<List<ConceptMasteryResponse>> getStrongConcepts(@PathVariable String userId) {
        return ResponseEntity.ok(knowledgeService.getStrongConcepts(userId));
    }

    @GetMapping("/mastery/{userId}")
    public ResponseEntity<List<ConceptMasteryResponse>> getConceptMastery(@PathVariable String userId) {
        return ResponseEntity.ok(knowledgeService.getConceptMastery(userId));
    }
}
