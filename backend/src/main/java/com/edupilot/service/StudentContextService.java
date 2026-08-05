package com.edupilot.service;

import com.edupilot.model.StudentProfile;
import com.edupilot.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class StudentContextService {

    @Autowired
    private StudentProfileRepository profileRepository;

    /**
     * Builds contextual student map for LLM prompt context injection.
     */
    public Map<String, Object> buildStudentContext(String studentId, String referencedConcept) {
        Map<String, Object> context = new HashMap<>();
        context.put("studentId", studentId);
        if (referencedConcept != null) {
            context.put("referencedConcept", referencedConcept);
        }

        Optional<StudentProfile> profileOpt = profileRepository.findByUserId(studentId);
        if (profileOpt.isPresent()) {
            StudentProfile profile = profileOpt.get();
            context.put("studentName", profile.getFullName() != null ? profile.getFullName() : "Student");
            context.put("sgi", profile.getStudentGrowthIndex());
            context.put("riskLevel", profile.getAcademicRiskLevel());
        }

        return context;
    }
}
