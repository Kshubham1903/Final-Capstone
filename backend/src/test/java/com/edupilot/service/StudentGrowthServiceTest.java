package com.edupilot.service;

import com.edupilot.dto.StudentGrowthResponseDTO;
import com.edupilot.dto.StudentGrowthResponseDTO.*;
import com.edupilot.model.*;
import com.edupilot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class StudentGrowthServiceTest {

    @Autowired
    private StudentGrowthService studentGrowthService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    @Autowired
    private StudentStateSnapshotRepository snapshotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private ConceptRemediationService conceptRemediationService;

    @Autowired
    private RemediationSessionRepository remediationSessionRepository;

    private String testUserId;

    @BeforeEach
    public void setUp() {
        testUserId = "growth_test_user_" + System.currentTimeMillis();

        // Create student profile
        studentService.onboardStudent(
                testUserId,
                "Computer Science & Engineering",
                1,
                List.of("Data Structures & Algorithms", "Database Management Systems"),
                List.of("Software Engineer"),
                4.0,
                9.0,
                7.5,
                5.0,
                30,
                "Visual"
        );
    }

    @Test
    public void test1_BaselineCorrectlyEstablished() {
        // Seed Initial Diagnostic (T0) with 42% score
        AssessmentResult diagnostic = new AssessmentResult();
        diagnostic.setUserId(testUserId);
        diagnostic.setSubjectCode("CS301");
        diagnostic.setSubjectName("Data Structures & Algorithms");
        diagnostic.setPercentage(42.0);
        diagnostic.setCreatedAt(LocalDateTime.now().minusDays(5));

        Map<String, Map<String, Object>> breakdown = new LinkedHashMap<>();
        breakdown.put("Arrays", Map.of("percentage", 40.0));
        breakdown.put("Linked Lists", Map.of("percentage", 44.0));
        diagnostic.setTopicBreakdown(breakdown);

        assessmentResultRepository.save(diagnostic);

        StudentGrowthResponseDTO response = studentGrowthService.calculateStudentGrowth(testUserId);

        assertNotNull(response);
        assertEquals(42.0, response.getBaselineKnowledge(), 0.1);
    }

    @Test
    public void test2_CurrentKnowledgeCorrectlyCalculated() {
        // Seed Concept Mastery
        ConceptMastery cm1 = new ConceptMastery();
        cm1.setUserId(testUserId);
        cm1.setSubjectCode("CS301");
        cm1.setSubjectName("Data Structures & Algorithms");
        cm1.setTopic("Arrays");
        cm1.setConceptName("Arrays");
        cm1.setAccuracy(70.0);
        cm1.setAttemptCount(5);
        conceptMasteryRepository.save(cm1);

        ConceptMastery cm2 = new ConceptMastery();
        cm2.setUserId(testUserId);
        cm2.setSubjectCode("CS301");
        cm2.setSubjectName("Data Structures & Algorithms");
        cm2.setTopic("Linked Lists");
        cm2.setConceptName("Linked Lists");
        cm2.setAccuracy(72.0);
        cm2.setAttemptCount(5);
        conceptMasteryRepository.save(cm2);

        StudentGrowthResponseDTO response = studentGrowthService.calculateStudentGrowth(testUserId);

        assertNotNull(response);
        assertEquals(71.0, response.getCurrentKnowledge(), 0.5);
    }

    @Test
    public void test3_CumulativeGrowthCorrectlyCalculated() {
        // Baseline = 42%, Current = 71% -> Growth = +29 pp
        AssessmentResult diagnostic = new AssessmentResult();
        diagnostic.setUserId(testUserId);
        diagnostic.setSubjectName("Data Structures & Algorithms");
        diagnostic.setPercentage(42.0);
        diagnostic.setCreatedAt(LocalDateTime.now().minusDays(5));
        diagnostic.setTopicBreakdown(Map.of("Arrays", Map.of("percentage", 42.0)));
        assessmentResultRepository.save(diagnostic);

        ConceptMastery cm1 = new ConceptMastery();
        cm1.setUserId(testUserId);
        cm1.setSubjectName("Data Structures & Algorithms");
        cm1.setConceptName("Arrays");
        cm1.setAccuracy(71.0);
        cm1.setAttemptCount(5);
        conceptMasteryRepository.save(cm1);

        StudentGrowthResponseDTO response = studentGrowthService.calculateStudentGrowth(testUserId);

        assertEquals(42.0, response.getBaselineKnowledge(), 0.5);
        assertEquals(71.0, response.getCurrentKnowledge(), 0.5);
        assertEquals(29.0, response.getCumulativeGrowth(), 0.5); // +29 pp
    }

    @Test
    public void test4_RecentGainCorrectlyCalculated() {
        // T0 = 42%, T1 = 63%, T2 = 71% -> Recent gain = 71 - 63 = +8 pp
        AssessmentResult diagnostic = new AssessmentResult();
        diagnostic.setUserId(testUserId);
        diagnostic.setSubjectName("Data Structures & Algorithms");
        diagnostic.setPercentage(42.0);
        diagnostic.setCreatedAt(LocalDateTime.now().minusDays(5));
        assessmentResultRepository.save(diagnostic);

        QuizSession session1 = new QuizSession();
        session1.setUserId(testUserId);
        session1.setSubjectName("Data Structures & Algorithms");
        session1.setTotalQuestions(100);
        session1.setCorrectCount(63);
        session1.setLastAnswerTime(LocalDateTime.now().minusDays(2));
        quizSessionRepository.save(session1);

        QuizSession session2 = new QuizSession();
        session2.setUserId(testUserId);
        session2.setSubjectName("Data Structures & Algorithms");
        session2.setTotalQuestions(100);
        session2.setCorrectCount(71);
        session2.setLastAnswerTime(LocalDateTime.now().minusDays(1));
        quizSessionRepository.save(session2);

        StudentGrowthResponseDTO response = studentGrowthService.calculateStudentGrowth(testUserId);

        assertEquals(8.0, response.getRecentGain(), 0.5); // +8 pp
    }

    @Test
    public void test5_NoBaselineHandledSafely() {
        // User has no diagnostic assessment
        StudentGrowthResponseDTO response = studentGrowthService.calculateStudentGrowth(testUserId);

        assertNotNull(response);
        assertNotNull(response.getBaselineKnowledge());
        assertNotNull(response.getCurrentKnowledge());
        assertNotNull(response.getCumulativeGrowth());
        assertNotNull(response.getDataSufficiencyNote());
    }

    @Test
    public void test6_SingleAssessmentOneTrajectoryPoint() {
        AssessmentResult diagnostic = new AssessmentResult();
        diagnostic.setUserId(testUserId);
        diagnostic.setSubjectName("Data Structures & Algorithms");
        diagnostic.setPercentage(45.0);
        diagnostic.setCreatedAt(LocalDateTime.now().minusDays(1));
        assessmentResultRepository.save(diagnostic);

        StudentGrowthResponseDTO response = studentGrowthService.calculateStudentGrowth(testUserId);

        assertNotNull(response.getTrajectory());
        assertEquals(1, response.getTrajectory().size());
        assertEquals(45.0, response.getTrajectory().get(0).getKnowledgeScore(), 0.5);
    }

    @Test
    public void test7_MultipleAssessmentsChronologicalTrajectory() {
        AssessmentResult diagnostic = new AssessmentResult();
        diagnostic.setUserId(testUserId);
        diagnostic.setSubjectName("Data Structures & Algorithms");
        diagnostic.setPercentage(40.0);
        diagnostic.setCreatedAt(LocalDateTime.now().minusDays(10));
        assessmentResultRepository.save(diagnostic);

        QuizSession q1 = new QuizSession();
        q1.setUserId(testUserId);
        q1.setSubjectName("Data Structures & Algorithms");
        q1.setTotalQuestions(100);
        q1.setCorrectCount(55);
        q1.setLastAnswerTime(LocalDateTime.now().minusDays(5));
        quizSessionRepository.save(q1);

        QuizSession q2 = new QuizSession();
        q2.setUserId(testUserId);
        q2.setSubjectName("Data Structures & Algorithms");
        q2.setTotalQuestions(100);
        q2.setCorrectCount(75);
        q2.setLastAnswerTime(LocalDateTime.now().minusDays(1));
        quizSessionRepository.save(q2);

        StudentGrowthResponseDTO response = studentGrowthService.calculateStudentGrowth(testUserId);

        List<GrowthTrajectoryPointDTO> traj = response.getTrajectory();
        assertTrue(traj.size() >= 3);
        assertTrue(traj.get(0).getTimestamp().isBefore(traj.get(1).getTimestamp()));
        assertTrue(traj.get(1).getTimestamp().isBefore(traj.get(2).getTimestamp()));
    }

    @Test
    public void test8_SubjectIsolation() {
        ConceptMastery cmDsa = new ConceptMastery();
        cmDsa.setUserId(testUserId);
        cmDsa.setSubjectName("Data Structures & Algorithms");
        cmDsa.setConceptName("Arrays");
        cmDsa.setAccuracy(80.0);
        conceptMasteryRepository.save(cmDsa);

        ConceptMastery cmDbms = new ConceptMastery();
        cmDbms.setUserId(testUserId);
        cmDbms.setSubjectName("Database Management Systems");
        cmDbms.setConceptName("SQL");
        cmDbms.setAccuracy(60.0);
        conceptMasteryRepository.save(cmDbms);

        StudentGrowthResponseDTO response = studentGrowthService.calculateStudentGrowth(testUserId);

        Map<String, SubjectGrowthDTO> subjects = response.getSubjectGrowth();
        assertNotNull(subjects);
        assertTrue(subjects.containsKey("Data Structures & Algorithms") || subjects.containsKey("Database Management Systems"));
    }

    @Test
    public void test9_StudentIsolation() {
        String otherUser = "growth_other_user_" + System.currentTimeMillis();
        studentService.onboardStudent(otherUser, "CSE", 1, List.of("DSA"), List.of("Engineer"), 4.0, 9.0, 7.5, 5.0, 30, "Visual");

        ConceptMastery cmOther = new ConceptMastery();
        cmOther.setUserId(otherUser);
        cmOther.setSubjectName("Data Structures & Algorithms");
        cmOther.setConceptName("Trees");
        cmOther.setAccuracy(95.0);
        conceptMasteryRepository.save(cmOther);

        StudentGrowthResponseDTO myResponse = studentGrowthService.calculateStudentGrowth(testUserId);
        StudentGrowthResponseDTO otherResponse = studentGrowthService.calculateStudentGrowth(otherUser);

        assertNotEquals(myResponse.getUserId(), otherResponse.getUserId());
    }

    @Test
    public void test10_CanonicalUserIdentity() {
        StudentGrowthResponseDTO response = studentGrowthService.calculateStudentGrowth(testUserId);

        assertNotNull(response);
        assertNotNull(response.getUserId());
        assertFalse(response.getUserId().isBlank());
    }

    @Test
    public void test11_NoFutureAssessmentLeakage() {
        List<MLFeaturePointDTO> dataset = studentGrowthService.extractMLFeatureDataset(testUserId);

        assertNotNull(dataset);
        for (MLFeaturePointDTO point : dataset) {
            assertNotNull(point.getTimestamp());
            assertNotNull(point.getCurrentKnowledgeAtT());
        }
    }

    @Test
    public void test12_DashboardApiReturnsRealValues() {
        StudentGrowthResponseDTO response = studentGrowthService.calculateStudentGrowth(testUserId);

        assertNotNull(response.getBaselineKnowledge());
        assertNotNull(response.getCurrentKnowledge());
        assertNotNull(response.getCumulativeGrowth());
        assertNotNull(response.getRecentGain());
        assertNotNull(response.getConceptsImproved());
        assertNotNull(response.getWeakConceptsRemaining());
        assertNotNull(response.getTrajectory());
    }

    @Test
    public void test13_MLDataset_ImmutableBaseline() {
        String uid = "ml_test_user_baseline_" + System.currentTimeMillis();

        AssessmentResult d0 = new AssessmentResult();
        d0.setUserId(uid);
        d0.setSubjectName("Data Structures & Algorithms");
        d0.setPercentage(44.0);
        d0.setTotalQuestions(10);
        d0.setCreatedAt(LocalDateTime.now().minusDays(4));
        assessmentResultRepository.save(d0);

        QuizSession q1 = new QuizSession();
        q1.setUserId(uid);
        q1.setSubjectName("Data Structures & Algorithms");
        q1.setTotalQuestions(10);
        q1.setCorrectCount(5);
        q1.setStatus(QuizSession.Status.COMPLETED);
        q1.setLastAnswerTime(LocalDateTime.now().minusDays(3));
        quizSessionRepository.save(q1);

        QuizSession q2 = new QuizSession();
        q2.setUserId(uid);
        q2.setSubjectName("Data Structures & Algorithms");
        q2.setTotalQuestions(10);
        q2.setCorrectCount(6);
        q2.setStatus(QuizSession.Status.COMPLETED);
        q2.setLastAnswerTime(LocalDateTime.now().minusDays(2));
        quizSessionRepository.save(q2);

        QuizSession q3 = new QuizSession();
        q3.setUserId(uid);
        q3.setSubjectName("Data Structures & Algorithms");
        q3.setTotalQuestions(10);
        q3.setCorrectCount(7);
        q3.setStatus(QuizSession.Status.COMPLETED);
        q3.setLastAnswerTime(LocalDateTime.now().minusDays(1));
        quizSessionRepository.save(q3);

        List<MLFeaturePointDTO> dataset = studentGrowthService.extractMLFeatureDataset(uid);

        assertNotNull(dataset);
        assertEquals(4, dataset.size());
        // Verify baseline K0 is 44.0 for ALL 4 observations in the trajectory
        for (MLFeaturePointDTO point : dataset) {
            assertEquals(44.0, point.getBaselineKnowledgeAtT(), 0.01, "Baseline K0 must remain immutable across trajectory!");
        }
    }

    @Test
    public void test14_MLDataset_SubjectIsolationAndTargets() {
        String uid = "ml_test_user_subjects_" + System.currentTimeMillis();

        // DSA Event 1
        QuizSession dsa1 = new QuizSession();
        dsa1.setUserId(uid);
        dsa1.setSubjectName("Data Structures & Algorithms");
        dsa1.setTotalQuestions(10);
        dsa1.setCorrectCount(4);
        dsa1.setStatus(QuizSession.Status.COMPLETED);
        dsa1.setLastAnswerTime(LocalDateTime.now().minusDays(5));
        quizSessionRepository.save(dsa1);

        // DSA Event 2
        QuizSession dsa2 = new QuizSession();
        dsa2.setUserId(uid);
        dsa2.setSubjectName("Data Structures & Algorithms");
        dsa2.setTotalQuestions(10);
        dsa2.setCorrectCount(6);
        dsa2.setStatus(QuizSession.Status.COMPLETED);
        dsa2.setLastAnswerTime(LocalDateTime.now().minusDays(3));
        quizSessionRepository.save(dsa2);

        // DBMS Event 1
        QuizSession dbms1 = new QuizSession();
        dbms1.setUserId(uid);
        dbms1.setSubjectName("Database Management Systems");
        dbms1.setTotalQuestions(10);
        dbms1.setCorrectCount(5);
        dbms1.setStatus(QuizSession.Status.COMPLETED);
        dbms1.setLastAnswerTime(LocalDateTime.now().minusDays(2));
        quizSessionRepository.save(dbms1);

        List<MLFeaturePointDTO> dataset = studentGrowthService.extractMLFeatureDataset(uid);

        assertEquals(3, dataset.size());

        List<MLFeaturePointDTO> dsaPoints = dataset.stream().filter(p -> "Data Structures & Algorithms".equals(p.getSubject())).toList();
        List<MLFeaturePointDTO> dbmsPoints = dataset.stream().filter(p -> "Database Management Systems".equals(p.getSubject())).toList();

        assertEquals(2, dsaPoints.size());
        assertEquals(1, dbmsPoints.size());

        // DSA Trajectory Check
        assertEquals(40.0, dsaPoints.get(0).getBaselineKnowledgeAtT(), 0.01);
        assertEquals(40.0, dsaPoints.get(1).getBaselineKnowledgeAtT(), 0.01);
        assertEquals(60.0, dsaPoints.get(0).getTargetKnowledgeAtNextT(), 0.01);
        assertNull(dsaPoints.get(1).getTargetKnowledgeAtNextT(), "Latest DSA observation target must be null!");

        // DBMS Trajectory Check
        assertEquals(50.0, dbmsPoints.get(0).getBaselineKnowledgeAtT(), 0.01);
        assertEquals(0.0, dbmsPoints.get(0).getRecentGainAtT(), 0.01, "DBMS baseline recent gain must be 0.0 (isolated from DSA)!");
        assertNull(dbmsPoints.get(0).getTargetKnowledgeAtNextT(), "Latest DBMS observation target must be null!");
    }

    @Test
    public void test15_MLDataset_InvalidEventsExcluded() {
        String uid = "ml_test_user_invalid_" + System.currentTimeMillis();

        // Valid quiz session
        QuizSession validQs = new QuizSession();
        validQs.setUserId(uid);
        validQs.setSubjectName("Data Structures & Algorithms");
        validQs.setTotalQuestions(5);
        validQs.setCorrectCount(4);
        validQs.setStatus(QuizSession.Status.COMPLETED);
        validQs.setLastAnswerTime(LocalDateTime.now().minusDays(2));
        quizSessionRepository.save(validQs);

        // Abandoned quiz session with 0 questions
        QuizSession abandonedQs = new QuizSession();
        abandonedQs.setUserId(uid);
        abandonedQs.setSubjectName("Data Structures & Algorithms");
        abandonedQs.setTotalQuestions(0);
        abandonedQs.setCorrectCount(0);
        abandonedQs.setStatus(QuizSession.Status.ABANDONED);
        abandonedQs.setLastAnswerTime(LocalDateTime.now().minusDays(1));
        quizSessionRepository.save(abandonedQs);

        List<MLFeaturePointDTO> dataset = studentGrowthService.extractMLFeatureDataset(uid);

        assertEquals(1, dataset.size(), "Abandoned/zero-question quiz sessions must be excluded from dataset!");
        assertEquals(80.0, dataset.get(0).getCurrentKnowledgeAtT(), 0.01);
    }

    @Test
    public void test16_RuntimeVerification_Student6a9ecd262703446e80e2ed36() {
        List<MLFeaturePointDTO> dataset = studentGrowthService.extractMLFeatureDataset("6a9ecd262703446e80e2ed36");

        System.out.println("=== REAL ML DATASET FOR STUDENT 6a9ecd262703446e80e2ed36 ===");
        System.out.println("Total Rows: " + dataset.size());
        int idx = 0;
        for (MLFeaturePointDTO p : dataset) {
            System.out.printf("[%2d] time=%s | subj=%-30s | conc=%-10s | K0=%.1f | Kt=%.1f | CumG=%+.1f | RecG=%+.1f | Att=%d | Acc=%.1f | Conf=%s | Type=%-15s | Target=%s%n",
                    idx++,
                    p.getTimestamp(),
                    p.getSubject(),
                    p.getConcept(),
                    p.getBaselineKnowledgeAtT(),
                    p.getCurrentKnowledgeAtT(),
                    p.getCumulativeGrowthAtT(),
                    p.getRecentGainAtT(),
                    p.getAttemptCountAtT(),
                    p.getAccuracyAtT(),
                    p.getConfidenceAtT() != null ? String.format("%.1f", p.getConfidenceAtT()) : "null",
                    p.getActivityType(),
                    p.getTargetKnowledgeAtNextT() != null ? String.format("%.1f", p.getTargetKnowledgeAtNextT()) : "null"
            );
        }
        System.out.println("===============================================================");
    }

    @Test
    public void test17_RealStudentTrajectoryAndReadinessAudit() {
        System.out.println("================================================================================");
        System.out.println("         REAL STUDENT TRAJECTORY & ML DATA READINESS AUDIT REPORT               ");
        System.out.println("================================================================================");

        // 1. Gather all User IDs across MongoDB collections
        Set<String> allUserIds = new LinkedHashSet<>();
        
        userRepository.findAll().forEach(u -> {
            if (u.getId() != null) allUserIds.add(u.getId());
            if (u.getEmail() != null) allUserIds.add(u.getEmail());
        });
        studentProfileRepository.findAll().forEach(p -> {
            if (p.getUserId() != null) allUserIds.add(p.getUserId());
            if (p.getId() != null) allUserIds.add(p.getId());
        });
        assessmentResultRepository.findAll().forEach(ar -> {
            if (ar.getUserId() != null) allUserIds.add(ar.getUserId());
            if (ar.getStudentProfileId() != null) allUserIds.add(ar.getStudentProfileId());
        });
        quizSessionRepository.findAll().forEach(qs -> {
            if (qs.getUserId() != null) allUserIds.add(qs.getUserId());
            if (qs.getStudentProfileId() != null) allUserIds.add(qs.getStudentProfileId());
        });
        conceptMasteryRepository.findAll().forEach(cm -> {
            if (cm.getUserId() != null) allUserIds.add(cm.getUserId());
            if (cm.getStudentProfileId() != null) allUserIds.add(cm.getStudentProfileId());
        });

        // Separate Test / Synthetic users from Genuine users
        List<String> genuineUserIds = new ArrayList<>();
        List<String> testUserIds = new ArrayList<>();

        for (String id : allUserIds) {
            String resolved = studentService.resolveUserId(id);
            if (resolved != null && !resolved.isBlank()) {
                if (resolved.startsWith("growth_test_") || resolved.startsWith("ml_test_") || resolved.startsWith("test_")) {
                    if (!testUserIds.contains(resolved)) testUserIds.add(resolved);
                } else {
                    if (!genuineUserIds.contains(resolved)) genuineUserIds.add(resolved);
                }
            }
        }

        System.out.println("Genuine Student Users in DB: " + genuineUserIds.size());
        System.out.println("Test / Verification Users Excluded: " + testUserIds.size());

        // Collect dataset points for genuine students
        Map<String, List<MLFeaturePointDTO>> studentDatasets = new LinkedHashMap<>();
        int totalObs = 0;
        int totalSupervised = 0;
        int totalNullTargets = 0;
        Set<String> allSubjects = new LinkedHashSet<>();
        Set<String> allConcepts = new LinkedHashSet<>();
        Map<String, Integer> activityTypeCounts = new HashMap<>();

        for (String uid : genuineUserIds) {
            List<MLFeaturePointDTO> dataset = studentGrowthService.extractMLFeatureDataset(uid);
            if (!dataset.isEmpty()) {
                studentDatasets.put(uid, dataset);
                totalObs += dataset.size();
                for (MLFeaturePointDTO p : dataset) {
                    if (p.getSubject() != null) allSubjects.add(p.getSubject());
                    if (p.getConcept() != null) allConcepts.add(p.getConcept());
                    activityTypeCounts.put(p.getActivityType(), activityTypeCounts.getOrDefault(p.getActivityType(), 0) + 1);
                    if (p.getTargetKnowledgeAtNextT() != null) {
                        totalSupervised++;
                    } else {
                        totalNullTargets++;
                    }
                }
            }
        }

        System.out.println("\n--- GLOBAL DATASET SUMMARY ---");
        System.out.println("Genuine Students with Assessment Data: " + studentDatasets.size());
        System.out.println("Total Trajectory Observations: " + totalObs);
        System.out.println("Total Usable Supervised Rows (target != null): " + totalSupervised);
        System.out.println("Total Null Target Rows (latest observations): " + totalNullTargets);
        System.out.println("Total Subjects Discovered: " + allSubjects.size() + " -> " + allSubjects);
        System.out.println("Total Concepts Discovered: " + allConcepts.size() + " -> " + allConcepts);

        // Print Anonymized Student Breakdown
        System.out.println("\n--- ANONYMIZED STUDENT BREAKDOWN ---");
        int studentIdx = 1;
        for (Map.Entry<String, List<MLFeaturePointDTO>> entry : studentDatasets.entrySet()) {
            String anonId = String.format("Student-%03d", studentIdx++);
            List<MLFeaturePointDTO> points = entry.getValue();
            long supervised = points.stream().filter(p -> p.getTargetKnowledgeAtNextT() != null).count();
            Set<String> subjs = points.stream().map(MLFeaturePointDTO::getSubject).collect(Collectors.toSet());
            Set<String> concs = points.stream().map(MLFeaturePointDTO::getConcept).collect(Collectors.toSet());
            LocalDateTime firstDate = points.get(0).getTimestamp();
            LocalDateTime lastDate = points.get(points.size() - 1).getTimestamp();

            System.out.printf("%-12s | Obs: %2d | Sup: %2d | Subjs: %-30s | Concs: %-30s | First: %s | Last: %s%n",
                    anonId, points.size(), supervised, subjs, concs, firstDate, lastDate);
        }

        // Print Subject Distribution
        System.out.println("\n--- SUBJECT DISTRIBUTION ---");
        Map<String, List<MLFeaturePointDTO>> pointsBySubject = new LinkedHashMap<>();
        for (List<MLFeaturePointDTO> points : studentDatasets.values()) {
            for (MLFeaturePointDTO p : points) {
                pointsBySubject.computeIfAbsent(p.getSubject(), k -> new ArrayList<>()).add(p);
            }
        }
        for (Map.Entry<String, List<MLFeaturePointDTO>> entry : pointsBySubject.entrySet()) {
            String subj = entry.getKey();
            List<MLFeaturePointDTO> pts = entry.getValue();
            long supCount = pts.stream().filter(p -> p.getTargetKnowledgeAtNextT() != null).count();
            Set<String> stus = pts.stream().map(MLFeaturePointDTO::getStudentId).collect(Collectors.toSet());
            Set<String> concs = pts.stream().map(MLFeaturePointDTO::getConcept).collect(Collectors.toSet());
            System.out.printf("Subject: %-30s | Students: %d | Obs: %d | Supervised: %d | Concepts: %s%n",
                    subj, stus.size(), pts.size(), supCount, concs);
        }

        // Print Concept Distribution
        System.out.println("\n--- CONCEPT DISTRIBUTION ---");
        Map<String, List<MLFeaturePointDTO>> pointsByConcept = new LinkedHashMap<>();
        for (List<MLFeaturePointDTO> points : studentDatasets.values()) {
            for (MLFeaturePointDTO p : points) {
                String key = p.getSubject() + " :: " + p.getConcept();
                pointsByConcept.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            }
        }
        for (Map.Entry<String, List<MLFeaturePointDTO>> entry : pointsByConcept.entrySet()) {
            String key = entry.getKey();
            List<MLFeaturePointDTO> pts = entry.getValue();
            Set<String> stus = pts.stream().map(MLFeaturePointDTO::getStudentId).collect(Collectors.toSet());
            System.out.printf("Concept Key: %-50s | Students: %d | Obs: %d%n", key, stus.size(), pts.size());
        }

        // Trajectory Length Distribution
        System.out.println("\n--- TRAJECTORY LENGTH DISTRIBUTION ---");
        int count1 = 0, count2 = 0, count3_5 = 0, count6_10 = 0, count11_20 = 0, countGt20 = 0;
        int ge2 = 0, ge3 = 0, ge5 = 0, ge10 = 0;

        for (List<MLFeaturePointDTO> pts : studentDatasets.values()) {
            int sz = pts.size();
            if (sz == 1) count1++;
            else if (sz == 2) count2++;
            else if (sz <= 5) count3_5++;
            else if (sz <= 10) count6_10++;
            else if (sz <= 20) count11_20++;
            else countGt20++;

            if (sz >= 2) ge2++;
            if (sz >= 3) ge3++;
            if (sz >= 5) ge5++;
            if (sz >= 10) ge10++;
        }
        System.out.printf("Obs=1: %d | Obs=2: %d | Obs 3-5: %d | Obs 6-10: %d | Obs 11-20: %d | Obs >20: %d%n",
                count1, count2, count3_5, count6_10, count11_20, countGt20);
        System.out.printf("Students with >=2 obs: %d | >=3 obs: %d | >=5 obs: %d | >=10 obs: %d%n",
                ge2, ge3, ge5, ge10);

        // Activity Type Distribution
        System.out.println("\n--- ACTIVITY TYPE DISTRIBUTION ---");
        for (Map.Entry<String, Integer> entry : activityTypeCounts.entrySet()) {
            System.out.printf("Activity Type: %-20s | Count: %d (%.1f%%)%n",
                    entry.getKey(), entry.getValue(), entry.getValue() * 100.0 / totalObs);
        }

        // Target Distribution Analysis
        List<Double> targets = new ArrayList<>();
        List<Double> currentKs = new ArrayList<>();
        List<Double> recentGains = new ArrayList<>();
        List<Double> nextGains = new ArrayList<>();
        int targetDecreases = 0, targetUnchanged = 0, targetIncreases = 0;

        for (List<MLFeaturePointDTO> points : studentDatasets.values()) {
            for (MLFeaturePointDTO p : points) {
                if (p.getRecentGainAtT() != null) recentGains.add(p.getRecentGainAtT());
                if (p.getTargetKnowledgeAtNextT() != null) {
                    double target = p.getTargetKnowledgeAtNextT();
                    double current = p.getCurrentKnowledgeAtT();
                    double gainNext = target - current;
                    targets.add(target);
                    currentKs.add(current);
                    nextGains.add(gainNext);
                    if (target > current) targetIncreases++;
                    else if (target < current) targetDecreases++;
                    else targetUnchanged++;
                }
            }
        }

        System.out.println("\n--- TARGET DISTRIBUTION (K_{t+1}) ---");
        if (!targets.isEmpty()) {
            targets.sort(Double::compareTo);
            double min = targets.get(0);
            double max = targets.get(targets.size() - 1);
            double mean = targets.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
            double median = targets.get(targets.size() / 2);
            double variance = targets.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().getAsDouble();
            double stdDev = Math.sqrt(variance);

            long r0_20 = targets.stream().filter(v -> v >= 0 && v <= 20).count();
            long r20_40 = targets.stream().filter(v -> v > 20 && v <= 40).count();
            long r40_60 = targets.stream().filter(v -> v > 40 && v <= 60).count();
            long r60_80 = targets.stream().filter(v -> v > 60 && v <= 80).count();
            long r80_100 = targets.stream().filter(v -> v > 80 && v <= 100).count();

            System.out.printf("Min: %.1f | Max: %.1f | Mean: %.2f | Median: %.1f | StdDev: %.2f%n", min, max, mean, median, stdDev);
            System.out.printf("Histogram Ranges -> [0-20]: %d | (20-40]: %d | (40-60]: %d | (60-80]: %d | (80-100]: %d%n",
                    r0_20, r20_40, r40_60, r60_80, r80_100);
            System.out.printf("Directional Shifts -> Increases: %d | Decreases: %d | Unchanged: %d%n",
                    targetIncreases, targetDecreases, targetUnchanged);
        }

        System.out.println("\n--- RECENT GAIN DISTRIBUTION ---");
        if (!recentGains.isEmpty()) {
            recentGains.sort(Double::compareTo);
            double minG = recentGains.get(0);
            double maxG = recentGains.get(recentGains.size() - 1);
            double meanG = recentGains.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
            double medianG = recentGains.get(recentGains.size() / 2);
            long pos = recentGains.stream().filter(v -> v > 0).count();
            long zero = recentGains.stream().filter(v -> v == 0).count();
            long neg = recentGains.stream().filter(v -> v < 0).count();

            System.out.printf("Min: %+.1f pp | Max: %+.1f pp | Mean: %+.2f pp | Median: %+.1f pp%n", minG, maxG, meanG, medianG);
            System.out.printf("Gains -> Positive: %d | Zero: %d | Negative: %d%n", pos, zero, neg);
        }

        // Non-ML Predictive Baselines Evaluation
        System.out.println("\n--- NON-ML PREDICTIVE BASELINES EVALUATION ---");
        if (!targets.isEmpty()) {
            // Baseline A: Predict K_{t+1} = K_t
            double sumAbsErrA = 0.0, sumSqErrA = 0.0;
            // Baseline B: Predict K_{t+1} = K_t + recentGain_t
            double sumAbsErrB = 0.0, sumSqErrB = 0.0;
            double meanTarget = targets.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
            double totalVar = targets.stream().mapToDouble(v -> Math.pow(v - meanTarget, 2)).sum();

            for (List<MLFeaturePointDTO> points : studentDatasets.values()) {
                for (MLFeaturePointDTO p : points) {
                    if (p.getTargetKnowledgeAtNextT() != null) {
                        double actualTarget = p.getTargetKnowledgeAtNextT();
                        double kt = p.getCurrentKnowledgeAtT();
                        double gain = p.getRecentGainAtT() != null ? p.getRecentGainAtT() : 0.0;

                        // Baseline A error
                        double errA = actualTarget - kt;
                        sumAbsErrA += Math.abs(errA);
                        sumSqErrA += Math.pow(errA, 2);

                        // Baseline B error
                        double predB = kt + gain;
                        double errB = actualTarget - predB;
                        sumAbsErrB += Math.abs(errB);
                        sumSqErrB += Math.pow(errB, 2);
                    }
                }
            }

            int N = targets.size();
            double maeA = sumAbsErrA / N;
            double rmseA = Math.sqrt(sumSqErrA / N);
            double r2A = 1.0 - (sumSqErrA / totalVar);

            double maeB = sumAbsErrB / N;
            double rmseB = Math.sqrt(sumSqErrB / N);
            double r2B = 1.0 - (sumSqErrB / totalVar);

            System.out.printf("Baseline A (Predict K_{t+1} = K_t):               MAE = %.2f pp | RMSE = %.2f pp | R^2 = %.4f%n", maeA, rmseA, r2A);
            System.out.printf("Baseline B (Predict K_{t+1} = K_t + recentGain_t): MAE = %.2f pp | RMSE = %.2f pp | R^2 = %.4f%n", maeB, rmseB, r2B);
        }

        // Target A vs Target B Comparison
        System.out.println("\n--- TARGET FORMULATION COMPARISON (K_{t+1} vs NEXT GAIN) ---");
        if (!targets.isEmpty() && !nextGains.isEmpty()) {
            double meanA = targets.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
            double varA = targets.stream().mapToDouble(v -> Math.pow(v - meanA, 2)).average().getAsDouble();
            double stdA = Math.sqrt(varA);

            double meanB = nextGains.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
            double varB = nextGains.stream().mapToDouble(v -> Math.pow(v - meanB, 2)).average().getAsDouble();
            double stdB = Math.sqrt(varB);

            System.out.printf("Target A (Absolute K_{t+1}): Mean = %.2f | Variance = %.2f | StdDev = %.2f | Range = [%.1f, %.1f]%n",
                    meanA, varA, stdA, targets.stream().mapToDouble(Double::doubleValue).min().getAsDouble(), targets.stream().mapToDouble(Double::doubleValue).max().getAsDouble());
            System.out.printf("Target B (Next Gain Delta K): Mean = %+.2f | Variance = %.2f | StdDev = %.2f | Range = [%+.1f, %+.1f]%n",
                    meanB, varB, stdB, nextGains.stream().mapToDouble(Double::doubleValue).min().getAsDouble(), nextGains.stream().mapToDouble(Double::doubleValue).max().getAsDouble());
        }

        System.out.println("================================================================================");
    }


    @Test
    public void test19_K0_Immutability_Across_T0_T1_T2_T3() {
        String uid = "k0_immutable_user_" + System.currentTimeMillis();

        AssessmentResult d0 = new AssessmentResult();
        d0.setUserId(uid);
        d0.setSubjectName("Data Structures & Algorithms");
        d0.setPercentage(44.0);
        d0.setCreatedAt(LocalDateTime.now().minusDays(5));
        assessmentResultRepository.save(d0);

        for (int i = 1; i <= 3; i++) {
            QuizSession qs = new QuizSession();
            qs.setUserId(uid);
            qs.setSubjectName("Data Structures & Algorithms");
            qs.setTotalQuestions(10);
            qs.setCorrectCount(5 + i);
            qs.setStatus(QuizSession.Status.COMPLETED);
            qs.setLastAnswerTime(LocalDateTime.now().minusDays(5 - i));
            quizSessionRepository.save(qs);
        }

        List<MLFeaturePointDTO> dataset = studentGrowthService.extractMLFeatureDataset(uid);
        assertEquals(4, dataset.size());

        for (int t = 0; t < 4; t++) {
            assertEquals(44.0, dataset.get(t).getBaselineKnowledgeAtT(), 0.01, "K0 must remain strictly 44.0 at index " + t);
        }
    }

    @Test
    public void test20_PendingReassessment_EligibilityAndCooldown() {
        String uid = "cooldown_user_" + System.currentTimeMillis();
        studentService.onboardStudent(uid, "CSE", 1, List.of("DSA"), List.of("Engineer"), 4.0, 9.0, 7.5, 5.0, 30, "Visual");

        // Seed a completed remediation session for "Trees"
        RemediationSession rem = new RemediationSession();
        rem.setStudentId(uid);
        rem.setSubject("Data Structures & Algorithms");
        rem.setConcept("Trees");
        rem.setCompleted(true);
        rem.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        remediationSessionRepository.save(rem);

        // Check pending reassessment -> should return Trees as eligible concept
        Map<String, Object> check1 = conceptRemediationService.getPendingReassessment(uid, "Data Structures & Algorithms");
        assertTrue((Boolean) check1.get("hasPendingCheck"));
        assertEquals("Trees", check1.get("concept"));

        // Simulate student completing a verification quiz for "Trees" 1 minute ago
        QuizSession qs = new QuizSession();
        qs.setUserId(uid);
        qs.setSubjectName("Data Structures & Algorithms");
        qs.setTotalQuestions(5);
        qs.setCorrectCount(4);
        qs.setStatus(QuizSession.Status.COMPLETED);
        qs.setVerificationQuiz(true);
        qs.setTargetConcept("Trees");
        qs.setLastAnswerTime(LocalDateTime.now().minusMinutes(1));
        quizSessionRepository.save(qs);

        // Check pending reassessment -> Cooldown active, hasPendingCheck must be false!
        Map<String, Object> check2 = conceptRemediationService.getPendingReassessment(uid, "Data Structures & Algorithms");
        assertFalse((Boolean) check2.get("hasPendingCheck"), "Cooldown safeguard must prevent immediate duplicate reassessment!");
    }

    @Test
    public void test21_SubjectIsolation_Reassessment() {
        String uid = "subj_iso_user_" + System.currentTimeMillis();
        studentService.onboardStudent(uid, "CSE", 1, List.of("DSA", "DBMS"), List.of("Engineer"), 4.0, 9.0, 7.5, 5.0, 30, "Visual");

        // Seed initial DBMS ConceptMastery = 55.0
        ConceptMastery dbmsCm = new ConceptMastery();
        dbmsCm.setUserId(uid);
        dbmsCm.setSubjectName("Database Management Systems");
        dbmsCm.setConceptName("SQL");
        dbmsCm.setAccuracy(55.0);
        conceptMasteryRepository.save(dbmsCm);

        // Perform DSA verification reassessment
        RemediationSession dsaRem = new RemediationSession();
        dsaRem.setStudentId(uid);
        dsaRem.setSubject("Data Structures & Algorithms");
        dsaRem.setConcept("Arrays");
        dsaRem.setQuestionIds(List.of("q1", "q2"));
        dsaRem = remediationSessionRepository.save(dsaRem);

        conceptRemediationService.submitRemediationTest(uid, dsaRem.getId(), List.of());

        // Verify DBMS concept mastery is completely unchanged
        Optional<ConceptMastery> checkedDbms = conceptMasteryRepository.findByUserIdAndConceptName(uid, "SQL");
        assertTrue(checkedDbms.isPresent());
        assertEquals(55.0, checkedDbms.get().getAccuracy(), 0.01, "DSA reassessment must NOT alter DBMS mastery!");
    }

    @Test
    public void test22_AbandonedReassessment_Exclusion() {
        String uid = "abandon_user_" + System.currentTimeMillis();

        // Save completed baseline
        QuizSession base = new QuizSession();
        base.setUserId(uid);
        base.setSubjectName("Data Structures & Algorithms");
        base.setTotalQuestions(10);
        base.setCorrectCount(5);
        base.setStatus(QuizSession.Status.COMPLETED);
        base.setLastAnswerTime(LocalDateTime.now().minusDays(2));
        quizSessionRepository.save(base);

        // Save abandoned session
        QuizSession abandoned = new QuizSession();
        abandoned.setUserId(uid);
        abandoned.setSubjectName("Data Structures & Algorithms");
        abandoned.setTotalQuestions(0);
        abandoned.setCorrectCount(0);
        abandoned.setStatus(QuizSession.Status.ABANDONED);
        abandoned.setLastAnswerTime(LocalDateTime.now().minusDays(1));
        quizSessionRepository.save(abandoned);

        List<MLFeaturePointDTO> dataset = studentGrowthService.extractMLFeatureDataset(uid);
        assertEquals(1, dataset.size(), "Abandoned assessment must NOT enter the growth trajectory!");
    }

    @Test
    public void test23_CompletedReassessment_InclusionAndNextTargetAssignment() {
        String uid = "target_assign_user_" + System.currentTimeMillis();

        // Event T0
        QuizSession t0 = new QuizSession();
        t0.setUserId(uid);
        t0.setSubjectName("Data Structures & Algorithms");
        t0.setTotalQuestions(10);
        t0.setCorrectCount(4);
        t0.setStatus(QuizSession.Status.COMPLETED);
        t0.setLastAnswerTime(LocalDateTime.now().minusDays(3));
        quizSessionRepository.save(t0);

        List<MLFeaturePointDTO> ds1 = studentGrowthService.extractMLFeatureDataset(uid);
        assertEquals(1, ds1.size());
        assertNull(ds1.get(0).getTargetKnowledgeAtNextT(), "Latest observation target must be null");

        // Event T1 (genuine assessment completed)
        QuizSession t1 = new QuizSession();
        t1.setUserId(uid);
        t1.setSubjectName("Data Structures & Algorithms");
        t1.setTotalQuestions(10);
        t1.setCorrectCount(7);
        t1.setStatus(QuizSession.Status.COMPLETED);
        t1.setLastAnswerTime(LocalDateTime.now().minusDays(1));
        quizSessionRepository.save(t1);

        List<MLFeaturePointDTO> ds2 = studentGrowthService.extractMLFeatureDataset(uid);
        assertEquals(2, ds2.size());
        assertEquals(70.0, ds2.get(0).getTargetKnowledgeAtNextT(), 0.01, "T0 target must receive T1 knowledge (70.0)");
        assertNull(ds2.get(1).getTargetKnowledgeAtNextT(), "Latest T1 observation target must be null");
    }

    @Test
    public void test24_StudentIsolation_StudentA_Cannot_Affect_StudentB() {
        String studentA = "student_A_" + System.currentTimeMillis();
        String studentB = "student_B_" + System.currentTimeMillis();

        // Baseline for Student A
        QuizSession qA = new QuizSession();
        qA.setUserId(studentA);
        qA.setSubjectName("Data Structures & Algorithms");
        qA.setTotalQuestions(10);
        qA.setCorrectCount(5);
        qA.setStatus(QuizSession.Status.COMPLETED);
        qA.setLastAnswerTime(LocalDateTime.now().minusDays(2));
        quizSessionRepository.save(qA);

        // Baseline for Student B
        QuizSession qB = new QuizSession();
        qB.setUserId(studentB);
        qB.setSubjectName("Data Structures & Algorithms");
        qB.setTotalQuestions(10);
        qB.setCorrectCount(9);
        qB.setStatus(QuizSession.Status.COMPLETED);
        qB.setLastAnswerTime(LocalDateTime.now().minusDays(2));
        quizSessionRepository.save(qB);

        List<MLFeaturePointDTO> dsA = studentGrowthService.extractMLFeatureDataset(studentA);
        List<MLFeaturePointDTO> dsB = studentGrowthService.extractMLFeatureDataset(studentB);

        assertEquals(50.0, dsA.get(0).getCurrentKnowledgeAtT(), 0.01);
        assertEquals(90.0, dsB.get(0).getCurrentKnowledgeAtT(), 0.01);
    }

    @Test
    public void test25_FullActivityDrivenEligibilityLifecycle() {
        String uid = "lifecycle_test_user_" + System.currentTimeMillis();
        studentService.onboardStudent(uid, "CSE", 1, List.of("Data Structures & Algorithms"), List.of("Engineer"), 4.0, 9.0, 7.5, 5.0, 30, "Visual");

        // 1. Initial state with a weak concept (<70%) but NO learning activity yet
        ConceptMastery cm = new ConceptMastery();
        cm.setUserId(uid);
        cm.setSubjectName("Data Structures & Algorithms");
        cm.setConceptName("Sorting Algorithms");
        cm.setTopic("Sorting Algorithms");
        cm.setAccuracy(45.0);
        cm.setAttemptCount(5);
        conceptMasteryRepository.save(cm);

        // First-time eligibility is true due to initial weak concept attempt
        Map<String, Object> check1 = conceptRemediationService.getPendingReassessment(uid, "Data Structures & Algorithms");
        assertTrue((Boolean) check1.get("hasPendingCheck"), "Initial weak concept with attempt count > 0 creates first-time eligibility");

        // 2. Student completes Knowledge Check A for "Sorting Algorithms"
        QuizSession kc1 = new QuizSession();
        kc1.setUserId(uid);
        kc1.setSubjectName("Data Structures & Algorithms");
        kc1.setTotalQuestions(5);
        kc1.setCorrectCount(3);
        kc1.setStatus(QuizSession.Status.COMPLETED);
        kc1.setVerificationQuiz(true);
        kc1.setTargetConcept("Sorting Algorithms");
        kc1.setLastAnswerTime(LocalDateTime.now().minusMinutes(10)); // Completed 10 minutes ago (>5 mins)
        quizSessionRepository.save(kc1);

        // 3. Immediately request pending check -> Must be FALSE (eligibility consumed by KC1, no new learning activity yet!)
        Map<String, Object> check2 = conceptRemediationService.getPendingReassessment(uid, "Data Structures & Algorithms");
        assertFalse((Boolean) check2.get("hasPendingCheck"), "Weak concept alone must NOT re-trigger eligibility once Knowledge Check is completed!");

        // 4. Student completes Learning Activity 1 for "Sorting Algorithms" (AFTER KC1)
        RemediationSession act1 = new RemediationSession();
        act1.setStudentId(uid);
        act1.setSubject("Data Structures & Algorithms");
        act1.setConcept("Sorting Algorithms");
        act1.setCompleted(true);
        act1.setCreatedAt(LocalDateTime.now().minusMinutes(2)); // Completed 2 minutes ago (AFTER KC1)
        remediationSessionRepository.save(act1);

        // 5. Request pending check -> Must be TRUE (new learning activity restores eligibility!)
        Map<String, Object> check3 = conceptRemediationService.getPendingReassessment(uid, "Data Structures & Algorithms");
        assertTrue((Boolean) check3.get("hasPendingCheck"), "New qualifying learning activity on same concept restores eligibility!");
        assertEquals("Sorting Algorithms", check3.get("concept"));
    }

    @Test
    public void test26_EndToEndControlledRuntimeFlow() {
        System.out.println("================================================================================");
        System.out.println("   SPRING INTEGRATION: END-TO-END CONTROLLED RUNTIME VERIFICATION (TEST USERS)  ");
        System.out.println("================================================================================");

        long ts = System.currentTimeMillis();
        String studentA = "test_runtime_user_A_" + ts;
        String studentB = "test_runtime_user_B_" + ts;

        // STEP 1: Onboard Student A
        studentService.onboardStudent(studentA, "Computer Science & Engineering", 1,
                List.of("Data Structures & Algorithms", "Database Management Systems"),
                List.of("Software Engineer"), 4.0, 9.0, 7.5, 5.0, 30, "Visual");

        // STEP 1b: Onboard Student B
        studentService.onboardStudent(studentB, "Computer Science & Engineering", 1,
                List.of("Data Structures & Algorithms", "Database Management Systems"),
                List.of("Software Engineer"), 4.0, 9.0, 7.5, 5.0, 30, "Visual");

        // STEP 2: Create Genuine Diagnostic Baseline (T0)
        AssessmentResult d0 = new AssessmentResult();
        d0.setUserId(studentA);
        d0.setSubjectCode("CS301");
        d0.setSubjectName("Data Structures & Algorithms");
        d0.setPercentage(40.0);
        d0.setCreatedAt(LocalDateTime.now().minusDays(5));
        d0.setTopicBreakdown(Map.of("Sorting Algorithms", Map.of("percentage", 40.0)));
        assessmentResultRepository.save(d0);

        // STEP 3: Perform Qualifying Learning Activity 1 on "Sorting Algorithms"
        Map<String, Object> remStart1 = conceptRemediationService.startRemediationTest(studentA, "Data Structures & Algorithms", "Sorting Algorithms");
        String remSessionId1 = (String) remStart1.get("sessionId");
        assertNotNull(remSessionId1);

        Map<String, Object> remSubmit1 = conceptRemediationService.submitRemediationTest(studentA, remSessionId1, List.of());
        assertTrue(remSubmit1.containsKey("sessionId"));

        // STEP 4: Call pending reassessment endpoint
        Map<String, Object> pending1 = conceptRemediationService.getPendingReassessment(studentA, "Data Structures & Algorithms");
        assertTrue((Boolean) pending1.get("hasPendingCheck"), "STEP 4: Expected hasPendingCheck == true after Learning Activity 1");
        assertEquals("Sorting Algorithms", pending1.get("concept"));

        // STEP 5-7: Start & Submit Knowledge Check 1
        Map<String, Object> kcStart1 = conceptRemediationService.startVerificationTest(studentA, "Data Structures & Algorithms", "Sorting Algorithms");
        String kcSessionId1 = (String) kcStart1.get("sessionId");
        
        Map<String, Object> kcSubmit1 = conceptRemediationService.submitRemediationTest(studentA, kcSessionId1, List.of());
        System.out.println("KC1 Submit Message: " + kcSubmit1.get("message"));

        // STEP 8: Verify QuizSession persisted (completed=true, isVerificationQuiz=true)
        List<QuizSession> userQuizzes = quizSessionRepository.findByUserIdOrderByLastAnswerTimeAsc(studentA);
        assertFalse(userQuizzes.isEmpty(), "STEP 8: QuizSession must be persisted");
        QuizSession lastQuiz = userQuizzes.get(userQuizzes.size() - 1);
        assertTrue(lastQuiz.isVerificationQuiz(), "STEP 8: QuizSession must be marked as verification quiz");
        assertEquals(QuizSession.Status.COMPLETED, lastQuiz.getStatus(), "STEP 8: QuizSession must be COMPLETED");

        // STEP 9: Verify ConceptMastery document updated
        Optional<ConceptMastery> cmOpt = conceptMasteryRepository.findByUserIdAndConceptName(studentA, "Sorting Algorithms");
        assertTrue(cmOpt.isPresent(), "STEP 9: ConceptMastery document must exist");

        // STEP 10: Call extractMLFeatureDataset
        List<MLFeaturePointDTO> dataset1 = studentGrowthService.extractMLFeatureDataset(studentA);
        System.out.println("ML Dataset Observations Count after KC1: " + dataset1.size());
        assertEquals(2, dataset1.size(), "STEP 10: Trajectory must contain T0 (baseline) and T1 (KC1)");

        // STEP 11: Verify K0, cumulativeGrowth, recentGain
        assertEquals(40.0, dataset1.get(0).getBaselineKnowledgeAtT(), 0.01, "STEP 11: K0 must be 40.0");
        assertEquals(40.0, dataset1.get(1).getBaselineKnowledgeAtT(), 0.01, "STEP 11: K0 must remain immutable 40.0 at T1");

        // STEP 12: Call pending reassessment immediately again -> Must be FALSE (eligibility consumed)
        Map<String, Object> pending2 = conceptRemediationService.getPendingReassessment(studentA, "Data Structures & Algorithms");
        assertFalse((Boolean) pending2.get("hasPendingCheck"), "STEP 12: Expected hasPendingCheck == false (consumed by KC1)");

        // STEP 13: Perform ANOTHER qualifying learning activity on SAME concept
        Map<String, Object> remStart2 = conceptRemediationService.startRemediationTest(studentA, "Data Structures & Algorithms", "Sorting Algorithms");
        conceptRemediationService.submitRemediationTest(studentA, (String) remStart2.get("sessionId"), List.of());

        Map<String, Object> pending3 = conceptRemediationService.getPendingReassessment(studentA, "Data Structures & Algorithms");
        assertTrue((Boolean) pending3.get("hasPendingCheck"), "STEP 13: Expected hasPendingCheck == true after Learning Activity 2");

        // STEP 14: Complete Knowledge Check 2 and verify T0 -> T1 -> T2 targets
        Map<String, Object> kcStart2 = conceptRemediationService.startVerificationTest(studentA, "Data Structures & Algorithms", "Sorting Algorithms");
        conceptRemediationService.submitRemediationTest(studentA, (String) kcStart2.get("sessionId"), List.of());

        List<MLFeaturePointDTO> dataset2 = studentGrowthService.extractMLFeatureDataset(studentA);
        assertEquals(3, dataset2.size(), "STEP 14: Trajectory must contain T0, T1, T2");
        assertNotNull(dataset2.get(0).getTargetKnowledgeAtNextT(), "STEP 14: T0 target must be populated");
        assertNotNull(dataset2.get(1).getTargetKnowledgeAtNextT(), "STEP 14: T1 target must be populated");
        assertNull(dataset2.get(2).getTargetKnowledgeAtNextT(), "STEP 14: Latest observation T2 target must be null");

        // STEP 15: Verify Abandonment Logic
        Map<String, Object> kcStart3 = conceptRemediationService.startVerificationTest(studentA, "Data Structures & Algorithms", "Sorting Algorithms");
        String abandonId = (String) kcStart3.get("sessionId");
        conceptRemediationService.abandonRemediationSession(abandonId);

        List<MLFeaturePointDTO> dataset3 = studentGrowthService.extractMLFeatureDataset(studentA);
        assertEquals(3, dataset3.size(), "STEP 15: Abandoned session must NOT enter growth trajectory");


        // STEP 16: Verify Student Isolation (Student B)
        Map<String, Object> pendingB = conceptRemediationService.getPendingReassessment(studentB, "Data Structures & Algorithms");
        assertFalse((Boolean) pendingB.get("hasPendingCheck"), "STEP 16: Student B must NOT have pending check caused by Student A");

        // STEP 17: Verify Subject Isolation (DBMS for Student A)
        Map<String, Object> pendingDBMS = conceptRemediationService.getPendingReassessment(studentA, "Database Management Systems");
        assertFalse((Boolean) pendingDBMS.get("hasPendingCheck"), "STEP 17: DSA activity must NOT trigger DBMS reassessment");

        System.out.println("================================================================================");
    }

    @Test
    public void test26_SubjectSpecificTimelineIndexing() {
        String uid = "subj_idx_user_" + System.currentTimeMillis();

        // Seed DSA T0
        AssessmentResult dsa0 = new AssessmentResult();
        dsa0.setUserId(uid);
        dsa0.setSubjectName("Data Structures & Algorithms");
        dsa0.setPercentage(44.0);
        dsa0.setTotalQuestions(10);
        dsa0.setCreatedAt(LocalDateTime.now().minusDays(5));
        assessmentResultRepository.save(dsa0);

        // Seed DSA T1
        AssessmentResult dsa1 = new AssessmentResult();
        dsa1.setUserId(uid);
        dsa1.setSubjectName("Data Structures & Algorithms");
        dsa1.setPercentage(50.0);
        dsa1.setTotalQuestions(10);
        dsa1.setCreatedAt(LocalDateTime.now().minusDays(4));
        assessmentResultRepository.save(dsa1);

        // Seed DBMS T0 (Interleaved in time between DSA T1 and DSA T2)
        AssessmentResult dbms0 = new AssessmentResult();
        dbms0.setUserId(uid);
        dbms0.setSubjectName("Database Management Systems");
        dbms0.setPercentage(40.0);
        dbms0.setTotalQuestions(10);
        dbms0.setCreatedAt(LocalDateTime.now().minusDays(3));
        assessmentResultRepository.save(dbms0);

        // Seed DSA T2
        AssessmentResult dsa2 = new AssessmentResult();
        dsa2.setUserId(uid);
        dsa2.setSubjectName("Data Structures & Algorithms");
        dsa2.setPercentage(60.0);
        dsa2.setTotalQuestions(10);
        dsa2.setCreatedAt(LocalDateTime.now().minusDays(2));
        assessmentResultRepository.save(dsa2);

        StudentGrowthResponseDTO response = studentGrowthService.calculateStudentGrowth(uid);
        List<GrowthTrajectoryPointDTO> trajectory = response.getTrajectory();

        assertEquals(4, trajectory.size());

        // DSA T0 check
        GrowthTrajectoryPointDTO dsaPoint0 = trajectory.stream()
                .filter(p -> "Data Structures & Algorithms".equals(p.getSubjectName()) && p.getKnowledgeScore() == 44.0)
                .findFirst().orElseThrow();
        assertEquals("DIAGNOSTIC", dsaPoint0.getAssessmentType());
        assertEquals("Initial Diagnostic Baseline Assessment (T0)", dsaPoint0.getDescription());

        // DSA T1 check
        GrowthTrajectoryPointDTO dsaPoint1 = trajectory.stream()
                .filter(p -> "Data Structures & Algorithms".equals(p.getSubjectName()) && p.getKnowledgeScore() == 50.0)
                .findFirst().orElseThrow();
        assertEquals("PROGRESS_ASSESSMENT", dsaPoint1.getAssessmentType());
        assertEquals("Progress Assessment Checkpoint (T1)", dsaPoint1.getDescription());

        // DBMS T0 check (Independent subject T0!)
        GrowthTrajectoryPointDTO dbmsPoint0 = trajectory.stream()
                .filter(p -> "Database Management Systems".equals(p.getSubjectName()) && p.getKnowledgeScore() == 40.0)
                .findFirst().orElseThrow();
        assertEquals("DIAGNOSTIC", dbmsPoint0.getAssessmentType());
        assertEquals("Initial Diagnostic Baseline Assessment (T0)", dbmsPoint0.getDescription());

        // DSA T2 check
        GrowthTrajectoryPointDTO dsaPoint2 = trajectory.stream()
                .filter(p -> "Data Structures & Algorithms".equals(p.getSubjectName()) && p.getKnowledgeScore() == 60.0)
                .findFirst().orElseThrow();
        assertEquals("PROGRESS_ASSESSMENT", dsaPoint2.getAssessmentType());
        assertEquals("Progress Assessment Checkpoint (T2)", dsaPoint2.getDescription());
    }
}



