package com.edupilot.controller;

import com.edupilot.dto.StudentGrowthResponseDTO;
import com.edupilot.dto.StudentGrowthResponseDTO.MLFeaturePointDTO;
import com.edupilot.service.StudentGrowthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class StudentGrowthController {

    @Autowired
    private StudentGrowthService studentGrowthService;

    @GetMapping("/api/student-growth/{userId}")
    public ResponseEntity<?> getStudentGrowth(@PathVariable String userId) {
        try {
            StudentGrowthResponseDTO response = studentGrowthService.calculateStudentGrowth(userId);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/api/students/{userId}/growth")
    public ResponseEntity<?> getStudentGrowthAlias(@PathVariable String userId) {
        return getStudentGrowth(userId);
    }

    @GetMapping("/api/student-growth/{userId}/ml-dataset")
    public ResponseEntity<?> getMLFeatureDataset(@PathVariable String userId) {
        try {
            List<MLFeaturePointDTO> dataset = studentGrowthService.extractMLFeatureDataset(userId);
            return ResponseEntity.ok(dataset);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
