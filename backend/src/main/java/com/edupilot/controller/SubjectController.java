package com.edupilot.controller;

import com.edupilot.dto.SubjectRequest;
import com.edupilot.dto.SubjectResponse;
import com.edupilot.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subjects")
@CrossOrigin(origins = "*")
public class SubjectController {

    @Autowired
    private SubjectService subjectService;

    @GetMapping
    public ResponseEntity<List<SubjectResponse>> getAllActiveSubjects() {
        return ResponseEntity.ok(subjectService.getAllActiveSubjects());
    }

    @GetMapping("/branches")
    public ResponseEntity<List<String>> getDistinctBranches() {
        return ResponseEntity.ok(subjectService.getDistinctBranches());
    }

    @GetMapping("/branches/{branch}")
    public ResponseEntity<List<SubjectResponse>> getSubjectsByBranch(@PathVariable String branch) {
        return ResponseEntity.ok(subjectService.getSubjectsByBranch(branch));
    }

    @GetMapping("/branches/{branch}/semesters/{semester}")
    public ResponseEntity<List<SubjectResponse>> getSubjectsByBranchAndSemester(
            @PathVariable String branch,
            @PathVariable int semester) {
        return ResponseEntity.ok(subjectService.getSubjectsByBranchAndSemester(branch, semester));
    }

    @GetMapping("/{subjectCode}")
    public ResponseEntity<?> getSubjectByCode(@PathVariable String subjectCode) {
        Optional<SubjectResponse> resp = subjectService.getSubjectByCode(subjectCode);
        if (resp.isPresent()) {
            return ResponseEntity.ok(resp.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> createOrUpdateSubject(@RequestBody SubjectRequest request) {
        try {
            SubjectResponse response = subjectService.createOrUpdateSubject(request);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
