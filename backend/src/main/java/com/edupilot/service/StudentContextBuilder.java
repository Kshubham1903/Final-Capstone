package com.edupilot.service;

import com.edupilot.dto.StudentContextDTO;
import com.edupilot.model.*;
import com.edupilot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentContextBuilder {

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private KnowledgeProfileRepository knowledgeProfileRepository;

    @Autowired
    private ConceptMasteryRepository conceptRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private LearningPlanRepository planRepository;

    public StudentContextDTO buildCompleteContext(String studentId, String referencedConcept) {
        StudentContextDTO dto = new StudentContextDTO();
        dto.setStudentId(studentId);

        // 1. Identity & Academic Profile Context
        Optional<StudentProfile> profileOpt = profileRepository.findByUserId(studentId);
        if (profileOpt.isPresent()) {
            StudentProfile profile = profileOpt.get();
            dto.setStudentName(profile.getFullName() != null ? profile.getFullName() : "Student");
            dto.setDegree(profile.getDegree() != null ? profile.getDegree() : "B.Tech");
            dto.setBranch(profile.getBranch() != null ? profile.getBranch() : "Computer Science & Engineering");
            dto.setSemester(profile.getSemester() > 0 ? profile.getSemester() : 1);
            dto.setCurrentCgpa(profile.getCurrentCgpa());
            dto.setTargetCgpa(profile.getTargetCgpa());
            dto.setSgi(profile.getStudentGrowthIndex());
            dto.setRiskLevel(profile.getAcademicRiskLevel() != null ? profile.getAcademicRiskLevel() : "LOW");
        }

        // 2. Knowledge Profile & Concept Mastery Context
        Optional<KnowledgeProfile> kpOpt = knowledgeProfileRepository.findByUserId(studentId);
        if (kpOpt.isPresent()) {
            KnowledgeProfile kp = kpOpt.get();
            dto.setLearningHealthScore(kp.getLearningHealthScore());
            dto.setStrongConcepts(kp.getStrongConcepts() != null ? kp.getStrongConcepts() : new ArrayList<>());
            dto.setWeakConcepts(kp.getWeakConcepts() != null ? kp.getWeakConcepts() : new ArrayList<>());
        } else {
            // Aggregate from concept_mastery if profile summary absent
            List<ConceptMastery> cmList = conceptRepository.findByUserId(studentId);
            List<String> strongs = cmList.stream().filter(c -> c.getAccuracy() >= 75.0).map(ConceptMastery::getConceptName).collect(Collectors.toList());
            List<String> weaks = cmList.stream().filter(c -> c.getAccuracy() < 60.0).map(ConceptMastery::getConceptName).collect(Collectors.toList());
            dto.setStrongConcepts(strongs);
            dto.setWeakConcepts(weaks);
            if (weaks.isEmpty()) {
                dto.setWeakConcepts(List.of("AVL Tree Rotations", "Recursion Depth"));
            }
        }

        // 3. Priority Recommendation Context
        List<Recommendation> recs = recommendationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(studentId, Recommendation.Status.ACTIVE);
        List<String> recSummaryList = recs.stream()
                .filter(r -> r.getPriority() == Recommendation.Priority.CRITICAL || r.getPriority() == Recommendation.Priority.HIGH)
                .map(r -> r.getPriority() + ": " + r.getConceptName() + " (" + r.getRecommendedAction() + ")")
                .limit(3)
                .collect(Collectors.toList());
        dto.setActiveRecommendations(recSummaryList);

        // 4. Today's Learning Plan Context
        Optional<LearningPlan> planOpt = planRepository.findByUserIdAndPlanDate(studentId, LocalDate.now());
        if (planOpt.isPresent() && planOpt.get().getTasks() != null && !planOpt.get().getTasks().isEmpty()) {
            LearningPlan.LearningTask topTask = planOpt.get().getTasks().get(0);
            dto.setTodayFocusTask(topTask.getConceptName() != null ? topTask.getConceptName() : topTask.getTopic());
            dto.setActiveSubject(topTask.getSubjectName() != null ? topTask.getSubjectName() : "Data Structures & Algorithms");
        } else if (referencedConcept != null && !referencedConcept.isBlank()) {
            dto.setTodayFocusTask(referencedConcept);
        }

        return dto;
    }
}
