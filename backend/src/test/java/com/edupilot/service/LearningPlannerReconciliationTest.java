package com.edupilot.service;

import com.edupilot.dto.LearningPlanResponse;
import com.edupilot.model.ConceptMastery;
import com.edupilot.model.LearningPlan;
import com.edupilot.model.Recommendation;
import com.edupilot.repository.ConceptMasteryRepository;
import com.edupilot.repository.LearningPlanRepository;
import com.edupilot.repository.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LearningPlannerReconciliationTest {

    @Autowired
    private LearningPlannerService learningPlannerService;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    @Autowired
    private LearningPlanRepository planRepository;

    private final String userId = "test_reconciliation_user_" + System.currentTimeMillis();

    @BeforeEach
    public void setup() {
        recommendationRepository.deleteAll(recommendationRepository.findByUserId(userId));
        conceptMasteryRepository.deleteAll(conceptMasteryRepository.findByUserId(userId));
        planRepository.deleteAll(planRepository.findByUserIdOrderByPlanDateDesc(userId));
    }

    @Test
    public void testLearningPlanDynamicStateReconciliationFlow() {
        // --- TEST 1: Active recommendation appears in Today's Learning Plan ---
        Recommendation rec1 = new Recommendation();
        rec1.setUserId(userId);
        rec1.setSubjectCode("CS301");
        rec1.setSubjectName("Data Structures & Algorithms");
        rec1.setTopic("Arrays & Linked Lists");
        rec1.setConceptName("Arrays & Linked Lists");
        rec1.setPriority(Recommendation.Priority.HIGH);
        rec1.setStatus(Recommendation.Status.ACTIVE);
        rec1.setCreatedAt(LocalDateTime.now());
        rec1 = recommendationRepository.save(rec1);

        LearningPlanResponse planRes1 = learningPlannerService.getTodayPlan(userId);
        assertNotNull(planRes1);
        assertEquals(1, planRes1.getTasks().size(), "TEST 1: Active recommendation should appear in plan");
        assertEquals("Arrays & Linked Lists", planRes1.getTasks().get(0).getConceptName());
        assertEquals(LearningPlan.LearningTask.TaskStatus.PENDING, planRes1.getTasks().get(0).getStatus());

        // --- TEST 2: Remediation completed -> VERIFICATION_PENDING -> remains visible ---
        rec1.setStatus(Recommendation.Status.VERIFICATION_PENDING);
        recommendationRepository.save(rec1);

        LearningPlanResponse planRes2 = learningPlannerService.getTodayPlan(userId);
        assertEquals(1, planRes2.getTasks().size(), "TEST 2: Remediation completed task should remain visible");
        assertEquals(LearningPlan.LearningTask.TaskStatus.VERIFICATION_PENDING, planRes2.getTasks().get(0).getStatus());

        // --- TEST 3: Verification passed -> COMPLETED -> disappears from plan ---
        rec1.setStatus(Recommendation.Status.COMPLETED);
        recommendationRepository.save(rec1);

        ConceptMastery cm1 = new ConceptMastery();
        cm1.setUserId(userId);
        cm1.setSubjectCode("CS301");
        cm1.setSubjectName("Data Structures & Algorithms");
        cm1.setConceptName("Arrays & Linked Lists");
        cm1.setMasteryLevel(ConceptMastery.MasteryLevel.MASTER);
        cm1.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm1.setAccuracy(90.0);
        conceptMasteryRepository.save(cm1);

        LearningPlanResponse planRes3 = learningPlannerService.getTodayPlan(userId);
        boolean rec1Present = planRes3.getTasks().stream()
                .anyMatch(t -> "Arrays & Linked Lists".equalsIgnoreCase(t.getConceptName()));
        assertFalse(rec1Present, "TEST 3: Verification passed concept must disappear from Today's Plan");

        // --- TEST 5 & 6 & 7: Refresh/Navigate/Regenerate -> completed recommendation does NOT return ---
        LearningPlanResponse planRes7 = learningPlannerService.generateLearningPlan(userId);
        boolean rec1PresentAfterRegen = planRes7.getTasks().stream()
                .anyMatch(t -> "Arrays & Linked Lists".equalsIgnoreCase(t.getConceptName()));
        assertFalse(rec1PresentAfterRegen, "TEST 7: Completed recommendation must NOT return upon plan regeneration");

        // --- TEST 8: Multiple recommendations -> completing one removes ONLY that item ---
        Recommendation rec2 = new Recommendation();
        rec2.setUserId(userId);
        rec2.setSubjectCode("CS301");
        rec2.setSubjectName("Data Structures & Algorithms");
        rec2.setTopic("Binary Search Trees");
        rec2.setConceptName("Binary Search Trees");
        rec2.setPriority(Recommendation.Priority.HIGH);
        rec2.setStatus(Recommendation.Status.ACTIVE);
        rec2.setCreatedAt(LocalDateTime.now());
        rec2 = recommendationRepository.save(rec2);

        Recommendation rec3 = new Recommendation();
        rec3.setUserId(userId);
        rec3.setSubjectCode("CS301");
        rec3.setSubjectName("Data Structures & Algorithms");
        rec3.setTopic("Graph Algorithms");
        rec3.setConceptName("Graph Algorithms");
        rec3.setPriority(Recommendation.Priority.HIGH);
        rec3.setStatus(Recommendation.Status.ACTIVE);
        rec3.setCreatedAt(LocalDateTime.now());
        rec3 = recommendationRepository.save(rec3);

        LearningPlanResponse multiPlan1 = learningPlannerService.generateLearningPlan(userId);
        assertTrue(multiPlan1.getTasks().size() >= 2, "TEST 8: Multiple active recommendations should appear");

        // Complete rec2
        rec2.setStatus(Recommendation.Status.COMPLETED);
        recommendationRepository.save(rec2);

        LearningPlanResponse multiPlan2 = learningPlannerService.getTodayPlan(userId);
        boolean rec2InPlan = multiPlan2.getTasks().stream().anyMatch(t -> "Binary Search Trees".equalsIgnoreCase(t.getConceptName()));
        boolean rec3InPlan = multiPlan2.getTasks().stream().anyMatch(t -> "Graph Algorithms".equalsIgnoreCase(t.getConceptName()));

        assertFalse(rec2InPlan, "TEST 8: Completed rec2 must disappear");
        assertTrue(rec3InPlan, "TEST 8: Active rec3 must remain");

        // --- TEST 9: No active recommendations -> correct empty/no-attention state ---
        rec3.setStatus(Recommendation.Status.COMPLETED);
        recommendationRepository.save(rec3);

        LearningPlanResponse emptyPlan = learningPlannerService.getTodayPlan(userId);
        boolean weakRecsRemaining = emptyPlan.getTasks().stream().anyMatch(t -> 
            "Binary Search Trees".equalsIgnoreCase(t.getConceptName()) || "Graph Algorithms".equalsIgnoreCase(t.getConceptName())
        );
        assertFalse(weakRecsRemaining, "TEST 9: No weak recommendations should remain in plan");
    }
}
