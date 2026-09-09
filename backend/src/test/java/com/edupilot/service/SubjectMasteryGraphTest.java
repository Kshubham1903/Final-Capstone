package com.edupilot.service;

import com.edupilot.model.ConceptMastery;
import com.edupilot.model.StudentProfile;
import com.edupilot.repository.ConceptMasteryRepository;
import com.edupilot.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SubjectMasteryGraphTest {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    private final String userId = "test_graph_user_" + System.currentTimeMillis();

    @BeforeEach
    public void setup() {
        conceptMasteryRepository.deleteAll(conceptMasteryRepository.findByUserId(userId));
        studentProfileRepository.deleteById(userId);
    }

    @Test
    public void testSubjectMasteryGraphDataFlowAndNormalization() {
        // Setup initial profile with canonical subjects
        StudentProfile prof = studentService.findOrCreateProfile(userId);
        prof.setSubjects(List.of("Data Structures & Algorithms", "Operating Systems"));
        prof = studentProfileRepository.save(prof);

        // --- TEST 1 & 8: Initial/empty state ---
        assertNotNull(prof.getConceptMastery());

        // --- TEST 2 & 3: Adaptive Quiz attempt 1 updates concept mastery -> subject mastery updates ---
        knowledgeService.updateSingleConceptMastery(
                userId,
                prof.getId(),
                "CS301",
                "Data Structures & Algorithms",
                "Arrays & Linked Lists",
                true
        );

        StudentProfile updatedProf1 = studentProfileRepository.findById(prof.getId()).orElseThrow();
        assertNotNull(updatedProf1.getConceptMastery());
        assertTrue(updatedProf1.getConceptMastery().containsKey("Data Structures & Algorithms"), "TEST 3: Subject key should exist");
        double initialScore = updatedProf1.getConceptMastery().get("Data Structures & Algorithms");
        assertTrue(initialScore > 0, "TEST 2: Subject mastery should be > 0 after correct answer");

        // --- TEST 4: Variant subject name ("Data Structures and Algorithms") normalizes to canonical key ---
        knowledgeService.updateSingleConceptMastery(
                userId,
                prof.getId(),
                "CS301",
                "Data Structures and Algorithms", // "and" variant
                "Binary Search Trees",
                true
        );

        StudentProfile updatedProf2 = studentProfileRepository.findById(prof.getId()).orElseThrow();
        assertEquals(1, updatedProf2.getConceptMastery().entrySet().stream()
                .filter(e -> e.getKey().contains("Data Structures"))
                .count(), "TEST 4: 'and' variant must NOT create duplicate subject key in StudentProfile");

        // --- TEST 5: Only affected subject changes (Operating Systems remains untouched) ---
        knowledgeService.updateSingleConceptMastery(
                userId,
                prof.getId(),
                "CS302",
                "Operating Systems",
                "Processes & Threads",
                true
        );

        StudentProfile updatedProf3 = studentProfileRepository.findById(prof.getId()).orElseThrow();
        assertTrue(updatedProf3.getConceptMastery().containsKey("Operating Systems"), "TEST 5: Operating Systems key should exist");
        assertTrue(updatedProf3.getConceptMastery().get("Operating Systems") > 0, "TEST 5: Operating Systems score updated independently");
    }
}
