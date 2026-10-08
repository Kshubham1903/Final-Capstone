package com.edupilot.service;

import com.edupilot.dto.AdminAnalyticsFilterCriteria;
import com.edupilot.dto.AdminAnalyticsOverviewDTO;
import com.edupilot.dto.AdminCohortAnalyticsDTO;
import com.edupilot.dto.AdminCohortComparisonDTO;
import com.edupilot.dto.AdminCohortComparisonRequest;
import com.edupilot.dto.AdminCohortSubjectAnalyticsDTO;
import com.edupilot.dto.AdminCohortSubjectAnalyticsDTO.SubjectResearchSummaryDTO;
import com.edupilot.dto.AdminResearchTrendsDTO;
import com.edupilot.dto.AdminStudentAnalyticsDTO;
import com.edupilot.dto.AdminStudentDirectoryDTO;
import com.edupilot.model.*;
import com.edupilot.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminAnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    @Autowired
    private StudentSatisfactionRepository satisfactionRepository;

    @Autowired
    private StudySessionRepository studySessionRepository;

    @Autowired
    private RemediationSessionRepository remediationSessionRepository;

    @Autowired
    private StudentStateSnapshotRepository snapshotRepository;

    @Autowired
    private SubjectRoadmapRepository subjectRoadmapRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LegacyAccountClassificationService legacyClassificationService;

    private String adminToken;
    private String studentToken;
    private String facultyToken;

    private User adminUser;
    private User studentUser;
    private User facultyUser;

    @BeforeEach
    public void setup() {
        // Isolated test cleanup for dedicated edupilot_test MongoDB database
        userRepository.deleteAll();
        studentProfileRepository.deleteAll();
        assessmentResultRepository.deleteAll();
        quizSessionRepository.deleteAll();
        conceptMasteryRepository.deleteAll();
        satisfactionRepository.deleteAll();
        studySessionRepository.deleteAll();
        remediationSessionRepository.deleteAll();
        snapshotRepository.deleteAll();
        subjectRoadmapRepository.deleteAll();

        long timestamp = System.currentTimeMillis();

        // 1. Create and persist Admin user
        adminUser = new User();
        adminUser.setEmail("admin_" + timestamp + "@edupilot.com");
        adminUser.setPassword(passwordEncoder.encode("adminpass123"));
        adminUser.setFullName("System Administrator");
        adminUser.setRole(User.Role.ADMIN);
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser = userRepository.save(adminUser);
        adminToken = jwtService.generateToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");

        // 2. Create and persist Student user
        studentUser = new User();
        studentUser.setEmail("student_" + timestamp + "@edupilot.com");
        studentUser.setPassword(passwordEncoder.encode("studentpass123"));
        studentUser.setFullName("Alice Student");
        studentUser.setRole(User.Role.STUDENT);
        studentUser.setAccountType(User.AccountType.GENUINE_STUDENT);
        studentUser.setCreatedAt(LocalDateTime.now());
        studentUser = userRepository.save(studentUser);
        studentToken = jwtService.generateToken(studentUser.getId(), studentUser.getEmail(), "STUDENT");

        // 3. Create and persist Faculty user
        facultyUser = new User();
        facultyUser.setEmail("faculty_" + timestamp + "@edupilot.com");
        facultyUser.setPassword(passwordEncoder.encode("facultypass123"));
        facultyUser.setFullName("Professor Smith");
        facultyUser.setRole(User.Role.FACULTY);
        facultyUser.setCreatedAt(LocalDateTime.now());
        facultyUser = userRepository.save(facultyUser);
        facultyToken = jwtService.generateToken(facultyUser.getId(), facultyUser.getEmail(), "FACULTY");
    }

    @Test
    public void testAdminCanAccessOverviewEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        AdminAnalyticsOverviewDTO dto = objectMapper.readValue(json, AdminAnalyticsOverviewDTO.class);
        assertNotNull(dto);
        assertEquals(1, dto.getTotalStudents(), "Should count exactly the 1 created student user");
    }

    @Test
    public void testAdminAnalyticsExcludesTestAutomationAndSyntheticAccounts() throws Exception {
        // Create test automation account
        User testAutomationUser = new User();
        testAutomationUser.setEmail("test_auto_" + System.currentTimeMillis() + "@edupilot.com");
        testAutomationUser.setPassword(passwordEncoder.encode("pass123"));
        testAutomationUser.setFullName("Automation Bot");
        testAutomationUser.setRole(User.Role.STUDENT);
        testAutomationUser.setAccountType(User.AccountType.TEST_AUTOMATION);
        testAutomationUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(testAutomationUser);

        // Create synthetic research seed account
        User syntheticSeedUser = new User();
        syntheticSeedUser.setEmail("seed_" + System.currentTimeMillis() + "@edupilot.com");
        syntheticSeedUser.setPassword(passwordEncoder.encode("pass123"));
        syntheticSeedUser.setFullName("Synthetic Seed");
        syntheticSeedUser.setRole(User.Role.STUDENT);
        syntheticSeedUser.setAccountType(User.AccountType.SYNTHETIC_RESEARCH_SEED);
        syntheticSeedUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(syntheticSeedUser);

        // Create unclassified legacy user (accountType is null)
        User legacyUser = new User();
        legacyUser.setEmail("legacy_" + System.currentTimeMillis() + "@edupilot.com");
        legacyUser.setPassword(passwordEncoder.encode("pass123"));
        legacyUser.setFullName("Legacy User");
        legacyUser.setRole(User.Role.STUDENT);
        legacyUser.setAccountType(null);
        legacyUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(legacyUser);

        MvcResult result = mockMvc.perform(get("/api/admin/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        AdminAnalyticsOverviewDTO dto = objectMapper.readValue(json, AdminAnalyticsOverviewDTO.class);
        assertNotNull(dto);
        assertEquals(1, dto.getTotalStudents(), "Admin analytics overview must exclude TEST_AUTOMATION, SYNTHETIC_RESEARCH_SEED, and unclassified null accountTypes");

        MvcResult dirResult = mockMvc.perform(get("/api/admin/analytics/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String dirJson = dirResult.getResponse().getContentAsString();
        List<AdminStudentDirectoryDTO> directory = objectMapper.readValue(dirJson, new TypeReference<List<AdminStudentDirectoryDTO>>() {});
        assertNotNull(directory);
        assertEquals(1, directory.size(), "Student Directory must contain only GENUINE_STUDENT accounts");
        assertEquals(studentUser.getId(), directory.get(0).getUserId());
    }

    @Test
    public void testAdminCanAccessCohortEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        AdminCohortAnalyticsDTO dto = objectMapper.readValue(json, AdminCohortAnalyticsDTO.class);
        assertNotNull(dto);
        assertEquals(1, dto.getTotalEnrolled());
        assertEquals(0, dto.getEvaluatedCohortSize());
        assertNotNull(dto.getGrowthDistribution());
        assertNotNull(dto.getSatisfaction());
        assertNotNull(dto.getDataSufficiencyNote());
    }

    @Test
    public void testStudentCannotAccessCohortEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testFacultyCannotAccessCohortEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUnauthenticatedCannotAccessCohortEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testStudentCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/overview")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testFacultyCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/overview")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUnauthenticatedCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testExistingStudentEndpointsStillWorkPermitAll() throws Exception {
        mockMvc.perform(get("/api/students/health")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student-growth/" + studentUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testAccurateAggregationWithoutFabrication() {
        String studentId = studentUser.getId();

        // 1. Create student profile
        studentService.onboardStudent(
                studentId, "CSE", 2, List.of("Data Structures & Algorithms"),
                List.of("Software Engineer"), 4.0, 8.5, 7.5, 4.0, 30, "Visual"
        );

        // 2. Add baseline diagnostic assessment (exact 40.0% initial)
        AssessmentResult diagnostic = new AssessmentResult();
        diagnostic.setUserId(studentId);
        diagnostic.setSubjectName("Data Structures & Algorithms");
        diagnostic.setTotalQuestions(10);
        diagnostic.setCorrectAnswers(4);
        diagnostic.setScore(40);
        diagnostic.setPercentage(40.0);
        diagnostic.setCreatedAt(LocalDateTime.now().minusDays(2));
        diagnostic.setTopicBreakdown(Map.of(
                "Arrays", Map.of("correct", 2, "total", 5, "percentage", 40.0),
                "Trees", Map.of("correct", 2, "total", 5, "percentage", 40.0)
        ));
        assessmentResultRepository.save(diagnostic);

        // 3. Add updated ConceptMastery (exact 80.0% current)
        ConceptMastery cm1 = new ConceptMastery();
        cm1.setUserId(studentId);
        cm1.setSubjectName("Data Structures & Algorithms");
        cm1.setTopic("Arrays");
        cm1.setConceptName("Arrays");
        cm1.setAccuracy(80.0);
        cm1.setMasteryScore(80.0);
        cm1.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm1.setLastAssessedAt(LocalDateTime.now());
        conceptMasteryRepository.save(cm1);

        ConceptMastery cm2 = new ConceptMastery();
        cm2.setUserId(studentId);
        cm2.setSubjectName("Data Structures & Algorithms");
        cm2.setTopic("Trees");
        cm2.setConceptName("Trees");
        cm2.setAccuracy(80.0);
        cm2.setMasteryScore(80.0);
        cm2.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm2.setLastAssessedAt(LocalDateTime.now());
        conceptMasteryRepository.save(cm2);

        // 4. Add completed QuizSession
        QuizSession qs = new QuizSession();
        qs.setUserId(studentId);
        qs.setSubjectName("Data Structures & Algorithms");
        qs.setTotalQuestions(5);
        qs.setCorrectCount(4);
        qs.setStatus(QuizSession.Status.COMPLETED);
        qs.setLastAnswerTime(LocalDateTime.now().minusHours(1));
        quizSessionRepository.save(qs);

        // 5. Add Satisfaction rating (5 stars)
        StudentSatisfaction sat = new StudentSatisfaction(studentId, 5, StudentSatisfaction.FeedbackType.AI_TUTOR, "Excellent guidance!");
        satisfactionRepository.save(sat);

        // Compute Overview
        AdminAnalyticsOverviewDTO overview = adminAnalyticsService.getAnalyticsOverview();
        assertNotNull(overview);

        // Deterministic assertions with appropriate floating-point tolerances
        assertEquals(1, overview.getTotalStudents(), "Total students must be exactly 1");
        assertEquals(1, overview.getActiveStudentsLast7Days(), "Active students last 7 days must be 1");
        assertEquals(1, overview.getTotalAssessmentsCompleted(), "Total completed assessments must be 1");
        assertEquals(1, overview.getTotalQuizzesCompleted(), "Total completed quizzes must be 1");

        // Baseline knowledge (exact 40.0)
        assertEquals(40.0, overview.getCohortAverageBaselineKnowledge(), 0.5, "Cohort baseline knowledge should be ~40.0%");

        // Current knowledge (exact 80.0)
        assertEquals(80.0, overview.getCohortAverageCurrentKnowledge(), 0.5, "Cohort current knowledge should be ~80.0%");

        // Learning gain: (0.80 - 0.40) / (1.0 - 0.40) = 0.67
        assertEquals(0.67, overview.getCohortAverageLearningGain(), 0.05, "Cohort average learning gain should be ~0.67");

        // Satisfaction: 5.0
        assertEquals(5.0, overview.getAverageSatisfactionRating(), 0.1, "Average satisfaction rating must be 5.0");
    }

    @Test
    public void testCohortAnalyticsCalculationWithMixedCohort() {
        // Create 2 additional student users (Bob and Charlie)
        User student2 = new User();
        student2.setEmail("bob_" + System.currentTimeMillis() + "@edupilot.com");
        student2.setPassword(passwordEncoder.encode("bobpass123"));
        student2.setFullName("Bob Student");
        student2.setRole(User.Role.STUDENT);
        student2.setAccountType(User.AccountType.GENUINE_STUDENT);
        student2.setCreatedAt(LocalDateTime.now().minusDays(20));
        student2 = userRepository.save(student2);

        User student3 = new User();
        student3.setEmail("charlie_" + System.currentTimeMillis() + "@edupilot.com");
        student3.setPassword(passwordEncoder.encode("charliepass123"));
        student3.setFullName("Charlie Student");
        student3.setRole(User.Role.STUDENT);
        student3.setAccountType(User.AccountType.GENUINE_STUDENT);
        student3.setCreatedAt(LocalDateTime.now().minusDays(30));
        student3 = userRepository.save(student3);

        String aliceId = studentUser.getId();
        String bobId = student2.getId();
        String charlieId = student3.getId();

        // 1. Alice setup: Authentic baseline (40%), current knowledge (80%), active 1 hour ago, 5-star AI_TUTOR review
        studentService.onboardStudent(
                aliceId, "CSE", 2, List.of("Data Structures & Algorithms"),
                List.of("Software Engineer"), 4.0, 8.5, 7.5, 4.0, 30, "Visual"
        );
        AssessmentResult diagnosticAlice = new AssessmentResult();
        diagnosticAlice.setUserId(aliceId);
        diagnosticAlice.setSubjectName("Data Structures & Algorithms");
        diagnosticAlice.setTotalQuestions(10);
        diagnosticAlice.setCorrectAnswers(4);
        diagnosticAlice.setScore(40);
        diagnosticAlice.setPercentage(40.0);
        diagnosticAlice.setCreatedAt(LocalDateTime.now().minusDays(2));
        diagnosticAlice.setTopicBreakdown(Map.of(
                "Arrays", Map.of("correct", 2, "total", 5, "percentage", 40.0),
                "Trees", Map.of("correct", 2, "total", 5, "percentage", 40.0)
        ));
        assessmentResultRepository.save(diagnosticAlice);

        ConceptMastery cm1 = new ConceptMastery();
        cm1.setUserId(aliceId);
        cm1.setSubjectName("Data Structures & Algorithms");
        cm1.setTopic("Arrays");
        cm1.setConceptName("Arrays");
        cm1.setAccuracy(80.0);
        cm1.setMasteryScore(80.0);
        cm1.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm1.setLastAssessedAt(LocalDateTime.now());
        conceptMasteryRepository.save(cm1);

        ConceptMastery cm2 = new ConceptMastery();
        cm2.setUserId(aliceId);
        cm2.setSubjectName("Data Structures & Algorithms");
        cm2.setTopic("Trees");
        cm2.setConceptName("Trees");
        cm2.setAccuracy(80.0);
        cm2.setMasteryScore(80.0);
        cm2.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm2.setLastAssessedAt(LocalDateTime.now());
        conceptMasteryRepository.save(cm2);

        QuizSession qsAlice = new QuizSession();
        qsAlice.setUserId(aliceId);
        qsAlice.setSubjectName("Data Structures & Algorithms");
        qsAlice.setTotalQuestions(5);
        qsAlice.setCorrectCount(4);
        qsAlice.setStatus(QuizSession.Status.COMPLETED);
        qsAlice.setLastAnswerTime(LocalDateTime.now().minusHours(1));
        quizSessionRepository.save(qsAlice);

        satisfactionRepository.save(new StudentSatisfaction(aliceId, 5, StudentSatisfaction.FeedbackType.AI_TUTOR, "Great AI tutor"));

        // 2. Bob setup: NO baseline assessment. At-risk activity (quiz completed 10 days ago).
        QuizSession qsBob = new QuizSession();
        qsBob.setUserId(bobId);
        qsBob.setSubjectName("Data Structures & Algorithms");
        qsBob.setTotalQuestions(5);
        qsBob.setCorrectCount(3);
        qsBob.setStatus(QuizSession.Status.COMPLETED);
        qsBob.setLastAnswerTime(LocalDateTime.now().minusDays(10));
        quizSessionRepository.save(qsBob);

        StudentSatisfaction satBob = new StudentSatisfaction(bobId, 4, StudentSatisfaction.FeedbackType.AI_TUTOR, "Good");
        satBob.setTimestamp(LocalDateTime.now().minusDays(10));
        satisfactionRepository.save(satBob);

        // 3. Charlie setup: NO baseline assessment, no activity (inactive).

        // Execute calculation
        var cohort = adminAnalyticsService.getCohortAnalytics();
        assertNotNull(cohort);

        // 1. Enrollment & Evaluated Cohort
        assertEquals(3, cohort.getTotalEnrolled(), "Total enrolled should be 3 students");
        assertEquals(1, cohort.getEvaluatedCohortSize(), "Only Alice has authentic baseline diagnostic");

        // 2. Activity Stratification (sum must equal totalEnrolled = 3)
        assertEquals(1, cohort.getActiveLast7Days(), "Alice is active (1 hr ago)");
        assertEquals(1, cohort.getAtRiskStudents(), "Bob is at-risk (10 days ago)");
        assertEquals(1, cohort.getInactiveStudents(), "Charlie is inactive (>14 days / no activity)");
        assertEquals(cohort.getTotalEnrolled(), cohort.getActiveLast7Days() + cohort.getAtRiskStudents() + cohort.getInactiveStudents());

        // 3. Knowledge & Gain (evaluated cohort only)
        assertEquals(40.0, cohort.getMeanBaselineKnowledge(), 0.5);
        assertEquals(80.0, cohort.getMeanCurrentKnowledge(), 0.5);
        assertEquals(0.67, cohort.getMeanNormalizedGain(), 0.05);

        // 4. Growth Distribution
        assertNotNull(cohort.getGrowthDistribution());
        assertEquals(1, cohort.getGrowthDistribution().getImprovedCount());
        assertEquals(100.0, cohort.getGrowthDistribution().getImprovedPercentage(), 0.1);
        assertEquals(0, cohort.getGrowthDistribution().getUnchangedCount());
        assertEquals(0.0, cohort.getGrowthDistribution().getUnchangedPercentage(), 0.1);
        assertEquals(0, cohort.getGrowthDistribution().getDeclinedCount());
        assertEquals(0.0, cohort.getGrowthDistribution().getDeclinedPercentage(), 0.1);

        // 5. Satisfaction
        assertNotNull(cohort.getSatisfaction());
        assertEquals(4.5, cohort.getSatisfaction().getAverageRating(), 0.1);
        assertEquals(2, cohort.getSatisfaction().getTotalReviews());
        assertEquals(4.5, cohort.getSatisfaction().getByCategory().get("AI_TUTOR"), 0.1);
        assertNull(cohort.getSatisfaction().getByCategory().get("RECOMMENDATION"));
        assertNull(cohort.getSatisfaction().getByCategory().get("LEARNING_ACTIVITY"));

        // 6. Data Sufficiency Note
        assertEquals("Based on 1 student(s) with verified diagnostic baselines.", cohort.getDataSufficiencyNote());
    }

    @Test
    public void testCohortAnalyticsEmptyDatabase() {
        // Clear all students
        userRepository.deleteAll();

        var cohort = adminAnalyticsService.getCohortAnalytics();
        assertNotNull(cohort);

        assertEquals(0, cohort.getTotalEnrolled());
        assertEquals(0, cohort.getEvaluatedCohortSize());
        assertEquals(0, cohort.getActiveLast7Days());
        assertEquals(0, cohort.getAtRiskStudents());
        assertEquals(0, cohort.getInactiveStudents());
        assertEquals(0.0, cohort.getMeanBaselineKnowledge(), 0.01);
        assertEquals(0.0, cohort.getMeanCurrentKnowledge(), 0.01);
        assertEquals(0.0, cohort.getMeanNormalizedGain(), 0.01);
        assertEquals(0, cohort.getGrowthDistribution().getImprovedCount());
        assertEquals(0.0, cohort.getGrowthDistribution().getImprovedPercentage(), 0.01);
        assertEquals(0.0, cohort.getSatisfaction().getAverageRating(), 0.01);
        assertEquals(0, cohort.getSatisfaction().getTotalReviews());
        assertNull(cohort.getSatisfaction().getByCategory().get("AI_TUTOR"));
        assertTrue(cohort.getDataSufficiencyNote().contains("Insufficient diagnostic data"));
    }

    @Test
    public void testAdminCohortEndpointWithRealisticData() throws Exception {
        // Create student 2 (unevaluated, at-risk)
        User student2 = new User();
        student2.setEmail("bob2_" + System.currentTimeMillis() + "@edupilot.com");
        student2.setPassword(passwordEncoder.encode("bobpass123"));
        student2.setFullName("Bob Student");
        student2.setRole(User.Role.STUDENT);
        student2.setAccountType(User.AccountType.GENUINE_STUDENT);
        student2.setCreatedAt(LocalDateTime.now().minusDays(20));
        student2 = userRepository.save(student2);

        String aliceId = studentUser.getId();
        String bobId = student2.getId();

        // 1. Alice setup: Authentic baseline (40%), current knowledge (80%), active 1 hour ago, 5-star AI_TUTOR review
        studentService.onboardStudent(
                aliceId, "CSE", 2, List.of("Data Structures & Algorithms"),
                List.of("Software Engineer"), 4.0, 8.5, 7.5, 4.0, 30, "Visual"
        );
        AssessmentResult diagnosticAlice = new AssessmentResult();
        diagnosticAlice.setUserId(aliceId);
        diagnosticAlice.setSubjectName("Data Structures & Algorithms");
        diagnosticAlice.setTotalQuestions(10);
        diagnosticAlice.setCorrectAnswers(4);
        diagnosticAlice.setScore(40);
        diagnosticAlice.setPercentage(40.0);
        diagnosticAlice.setCreatedAt(LocalDateTime.now().minusDays(2));
        diagnosticAlice.setTopicBreakdown(Map.of(
                "Arrays", Map.of("correct", 2, "total", 5, "percentage", 40.0),
                "Trees", Map.of("correct", 2, "total", 5, "percentage", 40.0)
        ));
        assessmentResultRepository.save(diagnosticAlice);

        ConceptMastery cm1 = new ConceptMastery();
        cm1.setUserId(aliceId);
        cm1.setSubjectName("Data Structures & Algorithms");
        cm1.setTopic("Arrays");
        cm1.setConceptName("Arrays");
        cm1.setAccuracy(80.0);
        cm1.setMasteryScore(80.0);
        cm1.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm1.setLastAssessedAt(LocalDateTime.now());
        conceptMasteryRepository.save(cm1);

        ConceptMastery cm2 = new ConceptMastery();
        cm2.setUserId(aliceId);
        cm2.setSubjectName("Data Structures & Algorithms");
        cm2.setTopic("Trees");
        cm2.setConceptName("Trees");
        cm2.setAccuracy(80.0);
        cm2.setMasteryScore(80.0);
        cm2.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm2.setLastAssessedAt(LocalDateTime.now());
        conceptMasteryRepository.save(cm2);

        QuizSession qsAlice = new QuizSession();
        qsAlice.setUserId(aliceId);
        qsAlice.setSubjectName("Data Structures & Algorithms");
        qsAlice.setTotalQuestions(5);
        qsAlice.setCorrectCount(4);
        qsAlice.setStatus(QuizSession.Status.COMPLETED);
        qsAlice.setLastAnswerTime(LocalDateTime.now().minusHours(1));
        quizSessionRepository.save(qsAlice);

        satisfactionRepository.save(new StudentSatisfaction(aliceId, 5, StudentSatisfaction.FeedbackType.AI_TUTOR, "Great AI tutor"));

        // 2. Bob setup: at-risk activity (10 days ago), satisfaction review (10 days ago)
        QuizSession qsBob = new QuizSession();
        qsBob.setUserId(bobId);
        qsBob.setSubjectName("Data Structures & Algorithms");
        qsBob.setTotalQuestions(5);
        qsBob.setCorrectCount(3);
        qsBob.setStatus(QuizSession.Status.COMPLETED);
        qsBob.setLastAnswerTime(LocalDateTime.now().minusDays(10));
        quizSessionRepository.save(qsBob);

        StudentSatisfaction satBob = new StudentSatisfaction(bobId, 4, StudentSatisfaction.FeedbackType.RECOMMENDATION, "Good recommendations");
        satBob.setTimestamp(LocalDateTime.now().minusDays(10));
        satisfactionRepository.save(satBob);

        // Perform HTTP GET /api/admin/analytics/cohort as ADMIN
        MvcResult result = mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        AdminCohortAnalyticsDTO dto = objectMapper.readValue(json, AdminCohortAnalyticsDTO.class);

        assertNotNull(dto);
        assertEquals(2, dto.getTotalEnrolled(), "Total enrolled must be 2");
        assertEquals(1, dto.getEvaluatedCohortSize(), "Evaluated cohort must be 1");
        assertEquals(1, dto.getActiveLast7Days(), "Active last 7 days must be 1");
        assertEquals(1, dto.getAtRiskStudents(), "At risk must be 1");
        assertEquals(0, dto.getInactiveStudents(), "Inactive must be 0");

        assertEquals(40.0, dto.getMeanBaselineKnowledge(), 0.5);
        assertEquals(80.0, dto.getMeanCurrentKnowledge(), 0.5);
        assertEquals(0.67, dto.getMeanNormalizedGain(), 0.05);

        assertNotNull(dto.getGrowthDistribution());
        assertEquals(1, dto.getGrowthDistribution().getImprovedCount());
        assertEquals(100.0, dto.getGrowthDistribution().getImprovedPercentage(), 0.1);
        assertEquals(0, dto.getGrowthDistribution().getUnchangedCount());
        assertEquals(0.0, dto.getGrowthDistribution().getUnchangedPercentage(), 0.1);
        assertEquals(0, dto.getGrowthDistribution().getDeclinedCount());
        assertEquals(0.0, dto.getGrowthDistribution().getDeclinedPercentage(), 0.1);

        assertNotNull(dto.getSatisfaction());
        assertEquals(4.5, dto.getSatisfaction().getAverageRating(), 0.1);
        assertEquals(2, dto.getSatisfaction().getTotalReviews());
        assertEquals(5.0, dto.getSatisfaction().getByCategory().get("AI_TUTOR"), 0.1);
        assertEquals(4.0, dto.getSatisfaction().getByCategory().get("RECOMMENDATION"), 0.1);
        assertNull(dto.getSatisfaction().getByCategory().get("LEARNING_ACTIVITY"));

        assertEquals("Based on 1 student(s) with verified diagnostic baselines.", dto.getDataSufficiencyNote());
    }

    @Test
    public void testAdminCanAccessStudentDirectoryEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/analytics/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        List<AdminStudentDirectoryDTO> directory = objectMapper.readValue(json, new TypeReference<List<AdminStudentDirectoryDTO>>() {});
        assertNotNull(directory);
        assertEquals(1, directory.size(), "Directory should contain exactly 1 student");
        assertEquals(studentUser.getId(), directory.get(0).getUserId());
        assertEquals("Alice Student", directory.get(0).getFullName());
        assertEquals(studentUser.getEmail(), directory.get(0).getEmail());
    }

    @Test
    public void testStudentCannotAccessStudentDirectoryEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testFacultyCannotAccessStudentDirectoryEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUnauthenticatedCannotAccessStudentDirectoryEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testStudentDirectoryContainsOnlyStudents() throws Exception {
        // Setup additional student
        User student2 = new User();
        student2.setEmail("bob_dir_" + System.currentTimeMillis() + "@edupilot.com");
        student2.setPassword(passwordEncoder.encode("bobpass123"));
        student2.setFullName("Bob Student");
        student2.setRole(User.Role.STUDENT);
        student2.setAccountType(User.AccountType.GENUINE_STUDENT);
        student2.setCreatedAt(LocalDateTime.now());
        userRepository.save(student2);

        MvcResult result = mockMvc.perform(get("/api/admin/analytics/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        List<AdminStudentDirectoryDTO> directory = objectMapper.readValue(json, new TypeReference<List<AdminStudentDirectoryDTO>>() {});
        assertNotNull(directory);
        assertEquals(2, directory.size(), "Should only contain the 2 STUDENT users and exclude ADMIN/FACULTY");

        for (AdminStudentDirectoryDTO item : directory) {
            assertNotEquals(adminUser.getId(), item.getUserId());
            assertNotEquals(facultyUser.getId(), item.getUserId());
        }
    }

    @Test
    public void testStudentWithoutAuthenticBaselineHasNullBaselineAndGrowth() throws Exception {
        // studentUser in setup has no diagnostic assessment
        MvcResult result = mockMvc.perform(get("/api/admin/analytics/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        List<AdminStudentDirectoryDTO> directory = objectMapper.readValue(json, new TypeReference<List<AdminStudentDirectoryDTO>>() {});
        assertNotNull(directory);
        assertEquals(1, directory.size());

        AdminStudentDirectoryDTO studentDto = directory.get(0);
        assertFalse(studentDto.isHasAuthenticBaseline(), "Student without diagnostic must not have authentic baseline");
        assertNull(studentDto.getBaselineKnowledge(), "Baseline knowledge must be null");
        assertNull(studentDto.getGrowthPp(), "Growth pp must be null");
        assertEquals("NO_ACTIVITY", studentDto.getActivityStatus());
    }

    @Test
    public void testStudentWithAuthenticBaselineCalculatesMetrics() throws Exception {
        String studentId = studentUser.getId();

        // 1. Create authentic diagnostic
        AssessmentResult diagnostic = new AssessmentResult();
        diagnostic.setUserId(studentId);
        diagnostic.setSubjectName("Data Structures & Algorithms");
        diagnostic.setScore(8);
        diagnostic.setTotalMarks(20);
        diagnostic.setPercentage(40.0);
        diagnostic.setTotalQuestions(20);
        diagnostic.setCreatedAt(LocalDateTime.now().minusDays(5));
        diagnostic.setTopicBreakdown(Map.of(
                "Trees", Map.of("percentage", 40.0, "correct", 4, "total", 10),
                "Graphs", Map.of("percentage", 40.0, "correct", 4, "total", 10)
        ));
        assessmentResultRepository.save(diagnostic);

        // 2. Create concept mastery records
        ConceptMastery cm1 = new ConceptMastery();
        cm1.setUserId(studentId);
        cm1.setSubjectName("Data Structures & Algorithms");
        cm1.setTopic("Trees");
        cm1.setConceptName("Trees");
        cm1.setAccuracy(80.0);
        cm1.setMasteryScore(80.0);
        cm1.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm1.setLastAssessedAt(LocalDateTime.now().minusDays(1));
        conceptMasteryRepository.save(cm1);

        ConceptMastery cm2 = new ConceptMastery();
        cm2.setUserId(studentId);
        cm2.setSubjectName("Data Structures & Algorithms");
        cm2.setTopic("Graphs");
        cm2.setConceptName("Graphs");
        cm2.setAccuracy(70.0);
        cm2.setMasteryScore(70.0);
        cm2.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm2.setLastAssessedAt(LocalDateTime.now().minusDays(1));
        conceptMasteryRepository.save(cm2);

        // 3. Quiz session for active status (2 days ago)
        QuizSession qs = new QuizSession();
        qs.setUserId(studentId);
        qs.setSubjectName("Data Structures & Algorithms");
        qs.setTotalQuestions(5);
        qs.setCorrectCount(4);
        qs.setStatus(QuizSession.Status.COMPLETED);
        qs.setLastAnswerTime(LocalDateTime.now().minusDays(2));
        quizSessionRepository.save(qs);

        MvcResult result = mockMvc.perform(get("/api/admin/analytics/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        List<AdminStudentDirectoryDTO> directory = objectMapper.readValue(json, new TypeReference<List<AdminStudentDirectoryDTO>>() {});
        assertNotNull(directory);
        assertEquals(1, directory.size());

        AdminStudentDirectoryDTO studentDto = directory.get(0);
        assertTrue(studentDto.isHasAuthenticBaseline(), "Should be authentic baseline");
        assertEquals(40.0, studentDto.getBaselineKnowledge(), 0.1);
        assertEquals(75.0, studentDto.getCurrentKnowledge(), 0.1);
        assertEquals(35.0, studentDto.getGrowthPp(), 0.1);
        assertEquals("ACTIVE", studentDto.getActivityStatus());
    }

    @Test
    public void testStudentDirectoryMissingProfileHandledGracefully() throws Exception {
        // Ensure studentProfileRepository is empty
        studentProfileRepository.deleteAll();

        MvcResult result = mockMvc.perform(get("/api/admin/analytics/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        List<AdminStudentDirectoryDTO> directory = objectMapper.readValue(json, new TypeReference<List<AdminStudentDirectoryDTO>>() {});
        assertNotNull(directory);
        assertEquals(1, directory.size());
        assertEquals("Alice Student", directory.get(0).getFullName());
        assertNull(directory.get(0).getBranch());
        assertNull(directory.get(0).getSemester());
    }

    @Test
    public void testAdminCanRetrieveIndividualStudentAnalyticsForValidStudent() {
        String studentId = studentUser.getId();

        // 1. Create StudentProfile
        StudentProfile profile = new StudentProfile();
        profile.setUserId(studentId);
        profile.setFullName("Alice Student");
        profile.setEmail(studentUser.getEmail());
        profile.setInstitution("EduPilot Institute of Technology");
        profile.setDegree("B.Tech");
        profile.setBranch("Computer Science");
        profile.setSemester(4);
        profile.setCareerGoals(List.of("AI Engineer", "Software Architect"));
        profile.setLearningStyle("Visual");
        profile.setSubjects(List.of("Data Structures & Algorithms"));
        studentProfileRepository.save(profile);

        // 2. Create Diagnostic Assessment (Baseline)
        AssessmentResult diagnostic = new AssessmentResult();
        diagnostic.setUserId(studentId);
        diagnostic.setSubjectCode("CS201");
        diagnostic.setSubjectName("Data Structures & Algorithms");
        diagnostic.setScore(8);
        diagnostic.setTotalMarks(20);
        diagnostic.setPercentage(40.0);
        diagnostic.setAccuracy(40.0);
        diagnostic.setMasteryLevel("NOVICE");
        diagnostic.setTotalQuestions(20);
        diagnostic.setCorrectAnswers(8);
        diagnostic.setIncorrectAnswers(12);
        diagnostic.setSkippedQuestions(0);
        diagnostic.setCreatedAt(LocalDateTime.now().minusDays(10));
        diagnostic.setTopicBreakdown(Map.of(
                "Trees", Map.of("percentage", 40.0, "correct", 4, "total", 10),
                "Graphs", Map.of("percentage", 40.0, "correct", 4, "total", 10)
        ));
        assessmentResultRepository.save(diagnostic);

        // 3. Create ConceptMastery
        ConceptMastery cm1 = new ConceptMastery();
        cm1.setUserId(studentId);
        cm1.setSubjectCode("CS201");
        cm1.setSubjectName("Data Structures & Algorithms");
        cm1.setTopic("Trees");
        cm1.setConceptName("Trees");
        cm1.setAccuracy(80.0);
        cm1.setMasteryScore(80.0);
        cm1.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm1.setMasteryLevel(ConceptMastery.MasteryLevel.PROFICIENT);
        cm1.setLastAssessedAt(LocalDateTime.now().minusDays(1));
        conceptMasteryRepository.save(cm1);

        ConceptMastery cm2 = new ConceptMastery();
        cm2.setUserId(studentId);
        cm2.setSubjectCode("CS201");
        cm2.setSubjectName("Data Structures & Algorithms");
        cm2.setTopic("Graphs");
        cm2.setConceptName("Graphs");
        cm2.setAccuracy(70.0);
        cm2.setMasteryScore(70.0);
        cm2.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm2.setMasteryLevel(ConceptMastery.MasteryLevel.INTERMEDIATE);
        cm2.setLastAssessedAt(LocalDateTime.now().minusDays(1));
        conceptMasteryRepository.save(cm2);

        // 4. Create Quiz Session
        QuizSession qs = new QuizSession();
        qs.setUserId(studentId);
        qs.setSubjectCode("CS201");
        qs.setSubjectName("Data Structures & Algorithms");
        qs.setTotalQuestions(10);
        qs.setCorrectCount(8);
        qs.setIncorrectCount(2);
        qs.setStatus(QuizSession.Status.COMPLETED);
        qs.setLastAnswerTime(LocalDateTime.now().minusDays(2));
        quizSessionRepository.save(qs);

        // 5. Create Study Session
        StudySession ss = new StudySession();
        ss.setUserId(studentId);
        ss.setSubjectCode("CS201");
        ss.setConceptName("Trees");
        ss.setActualDurationMinutes(45);
        ss.setStatus(StudySession.SessionStatus.COMPLETED);
        ss.setStartTime(LocalDateTime.now().minusDays(2));
        studySessionRepository.save(ss);

        // 6. Create Satisfaction
        StudentSatisfaction sat = new StudentSatisfaction(studentId, 5, StudentSatisfaction.FeedbackType.AI_TUTOR, "Excellent explanations");
        satisfactionRepository.save(sat);

        // 7. Create Remediation Session
        RemediationSession rem = new RemediationSession();
        rem.setStudentId(studentId);
        rem.setSubject("Data Structures & Algorithms");
        rem.setConcept("Trees");
        rem.setCompleted(true);
        rem.setCreatedAt(LocalDateTime.now().minusDays(3));
        remediationSessionRepository.save(rem);

        // 8. Create StudentStateSnapshot
        StudentStateSnapshot snap = new StudentStateSnapshot(studentId, 0.75, 0.85, Map.of("Trees", 0.8, "Graphs", 0.7));
        snapshotRepository.save(snap);

        // Call service method
        AdminStudentAnalyticsDTO result = adminAnalyticsService.getIndividualStudentAnalytics(studentId);

        assertNotNull(result);

        // Verify Student Info
        assertNotNull(result.getStudent());
        assertEquals(studentId, result.getStudent().getUserId());
        assertEquals("Alice Student", result.getStudent().getFullName());
        assertEquals("Computer Science", result.getStudent().getBranch());
        assertEquals(4, result.getStudent().getSemester());
        assertEquals("EduPilot Institute of Technology", result.getStudent().getInstitution());
        assertEquals("B.Tech", result.getStudent().getDegree());
        assertTrue(result.getStudent().getCareerGoals().contains("AI Engineer"));
        assertEquals("Visual", result.getStudent().getLearningStyle());

        // Verify Knowledge Growth
        assertNotNull(result.getKnowledge());
        assertTrue(result.getKnowledge().isHasAuthenticBaseline());
        assertEquals(40.0, result.getKnowledge().getBaselineKnowledge(), 0.1);
        assertEquals(75.0, result.getKnowledge().getCurrentKnowledge(), 0.1);
        assertEquals(35.0, result.getKnowledge().getGrowthPp(), 0.1);
        assertEquals(0.58, result.getKnowledge().getNormalizedLearningGain(), 0.05);

        // Verify Assessment Summary & History
        assertNotNull(result.getAssessmentSummary());
        assertEquals(1, result.getAssessmentSummary().getTotalAssessments());
        assertEquals(1, result.getAssessmentSummary().getCompletedAssessments());
        assertNotNull(result.getAssessmentSummary().getBaselineAssessmentDate());
        assertNotNull(result.getAssessmentSummary().getLatestAssessmentDate());
        assertEquals(1, result.getAssessmentHistory().size());
        assertEquals(40.0, result.getAssessmentHistory().get(0).getPercentage(), 0.1);

        // Verify Quiz Summary & History
        assertNotNull(result.getQuizSummary());
        assertEquals(1, result.getQuizSummary().getTotalCompletedQuizzes());
        assertEquals(10, result.getQuizSummary().getTotalQuestions());
        assertEquals(8, result.getQuizSummary().getTotalCorrect());
        assertEquals(80.0, result.getQuizSummary().getAccuracy(), 0.1);
        assertEquals(1, result.getQuizHistory().size());

        // Verify Subject Performance
        assertNotNull(result.getSubjectPerformance());
        assertFalse(result.getSubjectPerformance().isEmpty());
        assertEquals("Data Structures & Algorithms", result.getSubjectPerformance().get(0).getSubjectName());
        assertEquals(40.0, result.getSubjectPerformance().get(0).getBaselineScore(), 0.1);
        assertEquals(75.0, result.getSubjectPerformance().get(0).getCurrentScore(), 0.1);
        assertEquals(35.0, result.getSubjectPerformance().get(0).getGrowthPp(), 0.1);

        // Verify Concept Mastery
        assertNotNull(result.getConceptMastery());
        assertEquals(2, result.getConceptMastery().size());

        // Verify Activity
        assertNotNull(result.getActivity());
        assertEquals("ACTIVE", result.getActivity().getStatus());
        assertEquals(1, result.getActivity().getTotalStudySessions());
        assertEquals(45, result.getActivity().getTotalStudyMinutes());

        // Verify Satisfaction
        assertNotNull(result.getSatisfaction());
        assertEquals(5.0, result.getSatisfaction().getAverageRating(), 0.1);
        assertEquals(1, result.getSatisfaction().getTotalReviews());
        assertEquals(5.0, result.getSatisfaction().getByCategory().get("AI_TUTOR"), 0.1);

        // Verify Remediation
        assertNotNull(result.getRemediation());
        assertEquals(1, result.getRemediation().getTotalSessions());
        assertEquals(1, result.getRemediation().getCompletedSessions());
        assertEquals(0, result.getRemediation().getActiveSessions());

        // Verify Trajectory
        assertNotNull(result.getTrajectory());
        assertEquals(1, result.getTrajectory().size());
        assertEquals(0.75, result.getTrajectory().get(0).getOverallKnowledgeScore(), 0.01);
    }

    @Test
    public void testIndividualAnalyticsRejectsNonExistentUserId() {
        assertThrows(IllegalArgumentException.class, () -> {
            adminAnalyticsService.getIndividualStudentAnalytics("nonexistent_user_id_12345");
        });
    }

    @Test
    public void testIndividualAnalyticsRejectsAdminUserId() {
        assertThrows(IllegalArgumentException.class, () -> {
            adminAnalyticsService.getIndividualStudentAnalytics(adminUser.getId());
        });
    }

    @Test
    public void testIndividualAnalyticsRejectsFacultyUserId() {
        assertThrows(IllegalArgumentException.class, () -> {
            adminAnalyticsService.getIndividualStudentAnalytics(facultyUser.getId());
        });
    }

    @Test
    public void testIndividualAnalyticsStudentWithoutAuthenticBaselineHasNullBaselineAndGain() {
        String studentId = studentUser.getId();

        // No diagnostic assessment created
        AdminStudentAnalyticsDTO result = adminAnalyticsService.getIndividualStudentAnalytics(studentId);

        assertNotNull(result);
        assertNotNull(result.getKnowledge());
        assertFalse(result.getKnowledge().isHasAuthenticBaseline());
        assertNull(result.getKnowledge().getBaselineKnowledge());
        assertNull(result.getKnowledge().getGrowthPp());
        assertNull(result.getKnowledge().getNormalizedLearningGain());
        assertNull(result.getAssessmentSummary().getBaselineAssessmentDate());
    }

    @Test
    public void testIndividualAnalyticsMissingProfileHandledGracefully() {
        String studentId = studentUser.getId();
        studentProfileRepository.deleteAll();

        AdminStudentAnalyticsDTO result = adminAnalyticsService.getIndividualStudentAnalytics(studentId);

        assertNotNull(result);
        assertNotNull(result.getStudent());
        assertEquals(studentId, result.getStudent().getUserId());
        assertEquals("Alice Student", result.getStudent().getFullName());
        assertNull(result.getStudent().getBranch());
        assertNull(result.getStudent().getSemester());
        assertNull(result.getStudent().getInstitution());
    }

    @Test
    public void testIndividualAnalyticsMissingSatisfactionRemediationTrajectoryReturnsSafeDefaults() {
        String studentId = studentUser.getId();
        satisfactionRepository.deleteAll();
        remediationSessionRepository.deleteAll();
        snapshotRepository.deleteAll();
        studySessionRepository.deleteAll();

        AdminStudentAnalyticsDTO result = adminAnalyticsService.getIndividualStudentAnalytics(studentId);

        assertNotNull(result);
        assertNotNull(result.getSatisfaction());
        assertNull(result.getSatisfaction().getAverageRating());
        assertEquals(0, result.getSatisfaction().getTotalReviews());
        assertTrue(result.getSatisfaction().getReviews().isEmpty());

        assertNotNull(result.getRemediation());
        assertEquals(0, result.getRemediation().getTotalSessions());
        assertEquals(0, result.getRemediation().getCompletedSessions());
        assertTrue(result.getRemediation().getSessions().isEmpty());

        assertNotNull(result.getTrajectory());
        assertTrue(result.getTrajectory().isEmpty());

        assertNotNull(result.getActivity());
        assertEquals(0, result.getActivity().getTotalStudySessions());
        assertEquals(0, result.getActivity().getTotalStudyMinutes());
        assertEquals("NO_ACTIVITY", result.getActivity().getStatus());
    }

    @Test
    public void testIndividualAnalyticsSubjectWithoutDiagnosticHasNullBaselineAndGain() {
        String studentId = studentUser.getId();

        // 1. Diagnostic for Data Structures & Algorithms ONLY
        AssessmentResult diagnostic = new AssessmentResult();
        diagnostic.setUserId(studentId);
        diagnostic.setSubjectCode("CS201");
        diagnostic.setSubjectName("Data Structures & Algorithms");
        diagnostic.setScore(8);
        diagnostic.setTotalMarks(20);
        diagnostic.setPercentage(40.0);
        diagnostic.setCreatedAt(LocalDateTime.now().minusDays(10));
        diagnostic.setTopicBreakdown(Map.of("Trees", Map.of("percentage", 40.0, "correct", 4, "total", 10)));
        assessmentResultRepository.save(diagnostic);

        // 2. ConceptMastery for CS201 (DSA) and CS301 (DBMS)
        ConceptMastery cmDSA = new ConceptMastery();
        cmDSA.setUserId(studentId);
        cmDSA.setSubjectCode("CS201");
        cmDSA.setSubjectName("Data Structures & Algorithms");
        cmDSA.setTopic("Trees");
        cmDSA.setConceptName("Trees");
        cmDSA.setAccuracy(80.0);
        cmDSA.setMasteryScore(80.0);
        cmDSA.setStatus(ConceptMastery.ConceptStatus.STRONG);
        conceptMasteryRepository.save(cmDSA);

        ConceptMastery cmDBMS = new ConceptMastery();
        cmDBMS.setUserId(studentId);
        cmDBMS.setSubjectCode("CS301");
        cmDBMS.setSubjectName("Database Management Systems");
        cmDBMS.setTopic("Indexing");
        cmDBMS.setConceptName("B-Trees");
        cmDBMS.setAccuracy(65.0);
        cmDBMS.setMasteryScore(65.0);
        cmDBMS.setStatus(ConceptMastery.ConceptStatus.WEAK);
        conceptMasteryRepository.save(cmDBMS);

        AdminStudentAnalyticsDTO result = adminAnalyticsService.getIndividualStudentAnalytics(studentId);
        assertNotNull(result);
        assertNotNull(result.getSubjectPerformance());
        assertEquals(2, result.getSubjectPerformance().size());

        // DSA: has authentic diagnostic
        var dsa = result.getSubjectPerformance().stream()
                .filter(s -> "Data Structures & Algorithms".equals(s.getSubjectName()))
                .findFirst().orElseThrow();
        assertEquals(40.0, dsa.getBaselineScore(), 0.1);
        assertEquals(80.0, dsa.getCurrentScore(), 0.1);
        assertEquals(40.0, dsa.getGrowthPp(), 0.1);
        assertEquals(0.67, dsa.getNormalizedGain(), 0.05);
        assertEquals(0, dsa.getWeakConcepts());

        // DBMS: no authentic diagnostic -> baselineScore, growth, gain must be null
        var dbms = result.getSubjectPerformance().stream()
                .filter(s -> "Database Management Systems".equals(s.getSubjectName()))
                .findFirst().orElseThrow();
        assertNull(dbms.getBaselineScore(), "DBMS must NOT have fabricated baseline score");
        assertEquals(65.0, dbms.getCurrentScore(), 0.1);
        assertNull(dbms.getGrowthPp(), "DBMS must NOT have fabricated growth");
        assertNull(dbms.getNormalizedGain(), "DBMS must NOT have fabricated normalized gain");
        assertEquals(1, dbms.getWeakConcepts(), "DBMS concept accuracy 65% is < 70% and marked WEAK");
    }

    @Test
    public void testIndividualAnalyticsTrajectoryPreservesChronologicalOrder() {
        String studentId = studentUser.getId();

        LocalDateTime t1 = LocalDateTime.now().minusDays(5);
        LocalDateTime t2 = LocalDateTime.now().minusDays(3);
        LocalDateTime t3 = LocalDateTime.now().minusDays(1);

        StudentStateSnapshot s1 = new StudentStateSnapshot(studentId, 0.50, 0.60, Map.of("Trees", 0.5));
        s1.setTimestamp(t1);
        snapshotRepository.save(s1);

        StudentStateSnapshot s2 = new StudentStateSnapshot(studentId, 0.70, 0.80, Map.of("Trees", 0.7));
        s2.setTimestamp(t2);
        snapshotRepository.save(s2);

        StudentStateSnapshot s3 = new StudentStateSnapshot(studentId, 0.85, 0.90, Map.of("Trees", 0.85));
        s3.setTimestamp(t3);
        snapshotRepository.save(s3);

        AdminStudentAnalyticsDTO result = adminAnalyticsService.getIndividualStudentAnalytics(studentId);
        assertNotNull(result);
        assertNotNull(result.getTrajectory());
        assertEquals(3, result.getTrajectory().size());

        assertEquals(0.50, result.getTrajectory().get(0).getOverallKnowledgeScore(), 0.01);
        assertEquals(0.70, result.getTrajectory().get(1).getOverallKnowledgeScore(), 0.01);
        assertEquals(0.85, result.getTrajectory().get(2).getOverallKnowledgeScore(), 0.01);
        assertTrue(result.getTrajectory().get(0).getTimestamp().isBefore(result.getTrajectory().get(1).getTimestamp()));
        assertTrue(result.getTrajectory().get(1).getTimestamp().isBefore(result.getTrajectory().get(2).getTimestamp()));
    }

    @Test
    public void testAdminCanAccessIndividualStudentAnalyticsEndpoint() throws Exception {
        String studentId = studentUser.getId();

        MvcResult result = mockMvc.perform(get("/api/admin/analytics/students/" + studentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        AdminStudentAnalyticsDTO dto = objectMapper.readValue(json, AdminStudentAnalyticsDTO.class);
        assertNotNull(dto);
        assertNotNull(dto.getStudent());
        assertEquals(studentId, dto.getStudent().getUserId());
        assertNotNull(dto.getKnowledge());
        assertNotNull(dto.getAssessmentSummary());
        assertNotNull(dto.getAssessmentHistory());
        assertNotNull(dto.getQuizSummary());
        assertNotNull(dto.getQuizHistory());
        assertNotNull(dto.getSubjectPerformance());
        assertNotNull(dto.getConceptMastery());
        assertNotNull(dto.getActivity());
        assertNotNull(dto.getSatisfaction());
        assertNotNull(dto.getRemediation());
        assertNotNull(dto.getTrajectory());
    }

    @Test
    public void testStudentCannotAccessIndividualStudentAnalyticsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students/" + studentUser.getId())
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testFacultyCannotAccessIndividualStudentAnalyticsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students/" + studentUser.getId())
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUnauthenticatedCannotAccessIndividualStudentAnalyticsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students/" + studentUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testAdminRequestingAdminUserIdReturns4xx() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students/" + adminUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testAdminRequestingFacultyUserIdReturns4xx() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students/" + facultyUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testAdminRequestingNonExistentUserIdReturns404() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students/non_existent_id_99999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // PHASE 2C-1: HISTORICAL RESEARCH TRENDS TESTS
    // ==========================================

    @Test
    public void testAdminCanAccessResearchTrendsEndpoint() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/analytics/trends")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        AdminResearchTrendsDTO dto = objectMapper.readValue(json, AdminResearchTrendsDTO.class);
        assertNotNull(dto);
        assertNotNull(dto.getObservations());
        assertNotNull(dto.getDataSufficiencyNote());
    }

    @Test
    public void testStudentCannotAccessResearchTrendsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/trends")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testFacultyCannotAccessResearchTrendsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/trends")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUnauthenticatedCannotAccessResearchTrendsEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/trends")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testResearchTrendsDeterministicAggregationAndChronologicalOrder() {
        String studentId1 = studentUser.getId();

        // Create second student
        User student2 = new User();
        student2.setEmail("student2_trend@edupilot.com");
        student2.setPassword(passwordEncoder.encode("studentpass123"));
        student2.setFullName("Second Student");
        student2.setRole(User.Role.STUDENT);
        student2.setAccountType(User.AccountType.GENUINE_STUDENT);
        student2 = userRepository.save(student2);
        String studentId2 = student2.getId();

        LocalDateTime tMinus10 = LocalDateTime.now().minusDays(10);
        LocalDateTime tMinus5 = LocalDateTime.now().minusDays(5);
        LocalDateTime tMinus1 = LocalDateTime.now().minusDays(1);

        // 1. T-10: Student 1 takes diagnostic assessment (score 50.0%)
        AssessmentResult ar1 = new AssessmentResult();
        ar1.setUserId(studentId1);
        ar1.setSubjectCode("CS101");
        ar1.setSubjectName("Introduction to Programming");
        ar1.setScore(10);
        ar1.setTotalMarks(20);
        ar1.setPercentage(50.0);
        ar1.setAccuracy(50.0);
        ar1.setCreatedAt(tMinus10);
        ar1.setTopicBreakdown(Map.of("Variables", Map.of("percentage", 50.0, "correct", 5, "total", 10)));
        assessmentResultRepository.save(ar1);

        // 2. T-5: Student 1 takes module assessment (70.0%)
        AssessmentResult ar2 = new AssessmentResult();
        ar2.setUserId(studentId1);
        ar2.setSubjectCode("CS101");
        ar2.setSubjectName("Introduction to Programming");
        ar2.setScore(14);
        ar2.setTotalMarks(20);
        ar2.setPercentage(70.0);
        ar2.setAccuracy(70.0);
        ar2.setCreatedAt(tMinus5);
        assessmentResultRepository.save(ar2);

        // 3. T-5: Student 2 records state snapshot (overallKnowledge = 0.80 -> 80.0%, engagement = 0.75 -> 75.0%)
        StudentStateSnapshot snap2 = new StudentStateSnapshot(studentId2, 0.80, 0.75, Map.of("Trees", 0.80));
        snap2.setTimestamp(tMinus5);
        snapshotRepository.save(snap2);

        // 4. T-1: Student 2 completes quiz (accuracy 90.0%)
        QuizSession q2 = new QuizSession();
        q2.setUserId(studentId2);
        q2.setSubjectCode("CS201");
        q2.setSubjectName("Data Structures");
        q2.setStatus(QuizSession.Status.COMPLETED);
        q2.setTotalQuestions(10);
        q2.setCorrectCount(9);
        q2.setLastAnswerTime(tMinus1);
        quizSessionRepository.save(q2);

        AdminResearchTrendsDTO trends = adminAnalyticsService.getResearchTrends();
        assertNotNull(trends);
        assertEquals(4, trends.getTotalObservations(), "Total observation events must equal 4");
        assertEquals(2, trends.getUniqueStudentsCount(), "Unique students represented must equal 2");
        assertEquals(1, trends.getEvaluatedStudentsWithBaseline(), "Only student 1 has authentic baseline");
        assertEquals(3, trends.getObservations().size(), "Must have exactly 3 date points (no synthetic dates)");

        // Verify strictly chronological ordering
        var obsList = trends.getObservations();
        assertEquals(tMinus10.toLocalDate().toString(), obsList.get(0).getDate());
        assertEquals(tMinus5.toLocalDate().toString(), obsList.get(1).getDate());
        assertEquals(tMinus1.toLocalDate().toString(), obsList.get(2).getDate());

        // Point 1 (T-10): 1 assessment with 50.0%
        var p0 = obsList.get(0);
        assertEquals(50.0, p0.getMeanAssessmentScore(), 0.01);
        assertEquals(50.0, p0.getMeanKnowledgeScore(), 0.01);
        assertNull(p0.getMeanQuizAccuracy(), "Quiz accuracy must be null when no quizzes occurred");
        assertNull(p0.getMeanSnapshotKnowledgeScore(), "Snapshot score must be null when no snapshots occurred");
        assertNull(p0.getMeanEngagementScore(), "Engagement score must be null when no snapshots occurred");
        assertEquals(1, p0.getObservationCount());
        assertEquals(1, p0.getStudentCount());
        assertEquals(1, p0.getAssessmentCount());
        assertEquals(0, p0.getQuizCount());
        assertEquals(0, p0.getSnapshotCount());

        // Point 2 (T-5): 1 assessment (70.0%) + 1 snapshot (80.0% knowledge, 75.0% engagement)
        // Primary Assessment score MUST be 70.0% (NOT averaged with 80.0% snapshot)
        var p1 = obsList.get(1);
        assertEquals(70.0, p1.getMeanAssessmentScore(), 0.01, "Primary assessment score must strictly reflect AssessmentResult only");
        assertEquals(70.0, p1.getMeanKnowledgeScore(), 0.01);
        assertNull(p1.getMeanQuizAccuracy(), "Quiz accuracy must be null when no quizzes occurred");
        assertEquals(80.0, p1.getMeanSnapshotKnowledgeScore(), 0.01, "Snapshot knowledge metric must be separate");
        assertEquals(75.0, p1.getMeanEngagementScore(), 0.01, "Engagement metric must be separate");
        assertEquals(2, p1.getObservationCount());
        assertEquals(2, p1.getStudentCount());
        assertEquals(1, p1.getAssessmentCount());
        assertEquals(0, p1.getQuizCount());
        assertEquals(1, p1.getSnapshotCount());

        // Point 3 (T-1): 1 quiz (90.0%)
        // Assessment score must be null on dates with zero assessments
        var p2 = obsList.get(2);
        assertNull(p2.getMeanAssessmentScore(), "Assessment score must be null on dates without assessments");
        assertNull(p2.getMeanKnowledgeScore(), "Knowledge score alias must be null on dates without assessments");
        assertEquals(90.0, p2.getMeanQuizAccuracy(), 0.01, "Quiz accuracy must strictly reflect QuizSession only");
        assertNull(p2.getMeanSnapshotKnowledgeScore());
        assertNull(p2.getMeanEngagementScore());
        assertEquals(1, p2.getObservationCount());
        assertEquals(1, p2.getStudentCount());
        assertEquals(0, p2.getAssessmentCount());
        assertEquals(1, p2.getQuizCount());
        assertEquals(0, p2.getSnapshotCount());
    }

    @Test
    public void testStudentWithoutAuthenticBaselineIsNotAssigned50PercentResearchBaseline() {
        String studentId = studentUser.getId();

        // Student has only completed a quiz (no diagnostic assessment)
        QuizSession q = new QuizSession();
        q.setUserId(studentId);
        q.setSubjectCode("CS101");
        q.setSubjectName("Intro to CS");
        q.setStatus(QuizSession.Status.COMPLETED);
        q.setTotalQuestions(10);
        q.setCorrectCount(7);
        q.setLastAnswerTime(LocalDateTime.now().minusDays(1));
        quizSessionRepository.save(q);

        // 1. Verify Trends response evaluatedStudentsWithBaseline count
        AdminResearchTrendsDTO trends = adminAnalyticsService.getResearchTrends();
        assertNotNull(trends);
        assertEquals(0, trends.getEvaluatedStudentsWithBaseline(),
                "Student without authentic diagnostic baseline must NOT be counted as baseline-evaluated (no 50% fallback baseline)");

        // 2. Verify Individual Analytics
        AdminStudentAnalyticsDTO individual = adminAnalyticsService.getIndividualStudentAnalytics(studentId);
        assertNotNull(individual);
        assertNotNull(individual.getKnowledge());
        assertFalse(individual.getKnowledge().isHasAuthenticBaseline(),
                "hasAuthenticBaseline must be false");
        assertNull(individual.getKnowledge().getBaselineKnowledge(),
                "baselineKnowledge must remain null instead of fabricating 50.0%");
        assertNull(individual.getKnowledge().getGrowthPp(),
                "growthPp must remain null without authentic baseline");
        assertNull(individual.getKnowledge().getNormalizedLearningGain(),
                "normalizedLearningGain must remain null without authentic baseline");
    }

    @Test
    public void testResearchTrendsEmptyStateHandledCleanly() {
        // No assessments, snapshots, or quizzes persisted
        AdminResearchTrendsDTO trends = adminAnalyticsService.getResearchTrends();
        assertNotNull(trends);
        assertEquals(0, trends.getTotalObservations());
        assertEquals(0, trends.getUniqueStudentsCount());
        assertEquals(0, trends.getObservations().size());
        assertNull(trends.getEarliestObservationDate());
        assertNull(trends.getLatestObservationDate());
        assertTrue(trends.getDataSufficiencyNote().toLowerCase().contains("no historical"));
    }

    // ==========================================
    // PHASE 2C-2: SUBJECT RESEARCH ANALYTICS TESTS
    // ==========================================

    @Test
    public void testAdminCanAccessSubjectAnalyticsEndpoint_200() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/subjects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testStudentCannotAccessSubjectAnalyticsEndpoint_403() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/subjects")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testFacultyCannotAccessSubjectAnalyticsEndpoint_403() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/subjects")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUnauthenticatedCannotAccessSubjectAnalyticsEndpoint_401() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/subjects")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testSubjectAnalyticsDeterministicCalculations() throws Exception {
        String s1Id = studentUser.getId();

        // Create Student 2
        User student2 = new User();
        student2.setEmail("student2_subj@ritindia.edu");
        student2.setPassword(passwordEncoder.encode("password123"));
        student2.setRole(User.Role.STUDENT);
        student2.setAccountType(User.AccountType.GENUINE_STUDENT);
        student2 = userRepository.save(student2);
        String s2Id = student2.getId();

        // Student 1 Profile: Enrolled in "Mathematics" and "Physics"
        StudentProfile sp1 = new StudentProfile();
        sp1.setUserId(s1Id);
        sp1.setFullName("Student One");
        sp1.setSubjects(List.of("Mathematics", "Physics"));
        studentProfileRepository.save(sp1);

        // Student 2 Profile: Enrolled in "Mathematics"
        StudentProfile sp2 = new StudentProfile();
        sp2.setUserId(s2Id);
        sp2.setFullName("Student Two");
        sp2.setSubjects(List.of("Mathematics"));
        studentProfileRepository.save(sp2);

        // Student 1 Math Diagnostic (Earliest): 40.0%
        AssessmentResult ar1 = new AssessmentResult();
        ar1.setUserId(s1Id);
        ar1.setSubjectCode("MATH101");
        ar1.setSubjectName("Mathematics");
        ar1.setPercentage(40.0);
        ar1.setScore(40);
        ar1.setTotalMarks(100);
        ar1.setTotalQuestions(20);
        ar1.setCreatedAt(LocalDateTime.now().minusDays(10));
        assessmentResultRepository.save(ar1);

        // Student 2 Math Diagnostic (Earliest): 60.0%
        AssessmentResult ar2 = new AssessmentResult();
        ar2.setUserId(s2Id);
        ar2.setSubjectCode("MATH101");
        ar2.setSubjectName("Mathematics");
        ar2.setPercentage(60.0);
        ar2.setScore(60);
        ar2.setTotalMarks(100);
        ar2.setTotalQuestions(20);
        ar2.setCreatedAt(LocalDateTime.now().minusDays(10));
        assessmentResultRepository.save(ar2);

        // Student 1 Math Concepts: Calculus (80%), Algebra (70%) -> mean Kt = 75.0%
        ConceptMastery cm1 = new ConceptMastery();
        cm1.setUserId(s1Id);
        cm1.setSubjectCode("MATH101");
        cm1.setSubjectName("Mathematics");
        cm1.setTopic("Calculus");
        cm1.setConceptName("Limits");
        cm1.setAccuracy(80.0);
        cm1.setMasteryScore(80.0);
        cm1.setMasteryLevel(ConceptMastery.MasteryLevel.PROFICIENT);
        cm1.setStatus(ConceptMastery.ConceptStatus.STRONG);
        conceptMasteryRepository.save(cm1);

        ConceptMastery cm2 = new ConceptMastery();
        cm2.setUserId(s1Id);
        cm2.setSubjectCode("MATH101");
        cm2.setSubjectName("Mathematics");
        cm2.setTopic("Algebra");
        cm2.setConceptName("Matrices");
        cm2.setAccuracy(70.0);
        cm2.setMasteryScore(70.0);
        cm2.setMasteryLevel(ConceptMastery.MasteryLevel.INTERMEDIATE);
        cm2.setStatus(ConceptMastery.ConceptStatus.UNCERTAIN);
        conceptMasteryRepository.save(cm2);

        // Student 2 Math Concept: Calculus (90%) -> mean Kt = 90.0%
        ConceptMastery cm3 = new ConceptMastery();
        cm3.setUserId(s2Id);
        cm3.setSubjectCode("MATH101");
        cm3.setSubjectName("Mathematics");
        cm3.setTopic("Calculus");
        cm3.setConceptName("Derivatives");
        cm3.setAccuracy(90.0);
        cm3.setMasteryScore(90.0);
        cm3.setMasteryLevel(ConceptMastery.MasteryLevel.MASTER);
        cm3.setStatus(ConceptMastery.ConceptStatus.STRONG);
        conceptMasteryRepository.save(cm3);

        // Mathematics Quizzes: 1 completed quiz (10 questions, 8 correct)
        QuizSession qMath = new QuizSession();
        qMath.setUserId(s1Id);
        qMath.setSubjectCode("MATH101");
        qMath.setSubjectName("Mathematics");
        qMath.setStatus(QuizSession.Status.COMPLETED);
        qMath.setTotalQuestions(10);
        qMath.setCorrectCount(8);
        quizSessionRepository.save(qMath);

        // Mathematics Roadmap: 1 student, 4 topics, 2 completed
        SubjectRoadmap rmMath = new SubjectRoadmap();
        rmMath.setUserId(s1Id);
        rmMath.setSubjectCode("MATH101");
        rmMath.setSubjectName("Mathematics");
        SubjectRoadmap.RoadmapTopicNode t1 = new SubjectRoadmap.RoadmapTopicNode();
        t1.setCompleted(true);
        SubjectRoadmap.RoadmapTopicNode t2 = new SubjectRoadmap.RoadmapTopicNode();
        t2.setCompleted(true);
        SubjectRoadmap.RoadmapTopicNode t3 = new SubjectRoadmap.RoadmapTopicNode();
        t3.setCompleted(false);
        SubjectRoadmap.RoadmapTopicNode t4 = new SubjectRoadmap.RoadmapTopicNode();
        t4.setCompleted(false);
        rmMath.setTopics(List.of(t1, t2, t3, t4));
        subjectRoadmapRepository.save(rmMath);

        // Call Service
        AdminCohortSubjectAnalyticsDTO result = adminAnalyticsService.getCohortSubjectAnalytics();
        assertNotNull(result);
        assertEquals(2, result.getTotalEnrolledStudents());
        assertTrue(result.getTotalSubjectsCount() >= 2);

        SubjectResearchSummaryDTO mathSummary = result.getSubjects().stream()
                .filter(s -> s.getSubjectName().equalsIgnoreCase("Mathematics"))
                .findFirst().orElse(null);
        assertNotNull(mathSummary);

        // Student 1 Math: K0 = 40.0, Kt = 75.0, Growth = +35.0 pp, Gain = (0.75 - 0.40)/(1.0 - 0.40) = 0.35/0.60 = 0.5833 (0.58)
        // Student 2 Math: K0 = 60.0, Kt = 90.0, Growth = +30.0 pp, Gain = (0.90 - 0.60)/(1.0 - 0.60) = 0.30/0.40 = 0.75
        // Mean K0 = (40 + 60)/2 = 50.0
        // Mean Kt = (75 + 90)/2 = 82.5
        // Mean Growth = (35 + 30)/2 = 32.5 pp
        // Mean Gain = (0.58 + 0.75)/2 = 0.665 -> 0.67
        assertEquals(2, mathSummary.getStudentsRepresented());
        assertEquals(2, mathSummary.getStudentsWithAuthenticBaseline());
        assertEquals(2, mathSummary.getStudentsWithObservedKnowledge());
        assertEquals(50.0, mathSummary.getMeanBaselineKnowledge());
        assertEquals(82.5, mathSummary.getMeanCurrentKnowledge());
        assertEquals(32.5, mathSummary.getMeanGrowthPp());
        assertNotNull(mathSummary.getMeanNormalizedGain());
        assertEquals(0.67, mathSummary.getMeanNormalizedGain());

        // Concept Metrics: 3 unique concepts (Limits, Matrices, Derivatives)
        assertEquals(3, mathSummary.getConceptCount());
        // Weak count: accuracy < 70 OR status == WEAK (Limits: 80, Matrices: 70, Derivatives: 90 -> 0 weak)
        assertEquals(0, mathSummary.getWeakConceptCount());

        // Quiz Metrics
        assertEquals(1, mathSummary.getQuizSessionsCount());
        assertEquals(10, mathSummary.getTotalQuizQuestions());
        assertEquals(8, mathSummary.getCorrectQuizAnswers());
        assertEquals(80.0, mathSummary.getMeanQuizAccuracy());

        // Roadmap Metrics
        assertEquals(1, mathSummary.getStudentsWithRoadmap());
        assertEquals(4, mathSummary.getTotalRoadmapTopics());
        assertEquals(2, mathSummary.getCompletedRoadmapTopics());
        assertEquals(50.0, mathSummary.getRoadmapCompletionPercentage());
    }

    @Test
    public void testAuthenticSubjectBaselineExtractionOnlyFromEarliestDiagnostic() {
        String studentId = studentUser.getId();

        // Earliest diagnostic for Physics is 45.0%
        AssessmentResult diag = new AssessmentResult();
        diag.setUserId(studentId);
        diag.setSubjectCode("PHYS101");
        diag.setSubjectName("Physics");
        diag.setPercentage(45.0);
        diag.setScore(45);
        diag.setTotalMarks(100);
        diag.setTotalQuestions(20);
        diag.setCreatedAt(LocalDateTime.now().minusDays(20));
        assessmentResultRepository.save(diag);

        // Later assessment for Physics (85.0%) - should NOT overwrite baseline
        AssessmentResult later = new AssessmentResult();
        later.setUserId(studentId);
        later.setSubjectCode("PHYS101");
        later.setSubjectName("Physics");
        later.setPercentage(85.0);
        later.setScore(85);
        later.setTotalMarks(100);
        later.setTotalQuestions(20);
        later.setCreatedAt(LocalDateTime.now().minusDays(5));
        assessmentResultRepository.save(later);

        AdminCohortSubjectAnalyticsDTO result = adminAnalyticsService.getCohortSubjectAnalytics();
        SubjectResearchSummaryDTO phys = result.getSubjects().stream()
                .filter(s -> s.getSubjectName().equalsIgnoreCase("Physics"))
                .findFirst().orElse(null);

        assertNotNull(phys);
        assertEquals(1, phys.getStudentsWithAuthenticBaseline());
        assertEquals(45.0, phys.getMeanBaselineKnowledge(),
                "Baseline K0 must be strictly 45.0% from earliest diagnostic, NOT the later 85.0% assessment");
    }

    @Test
    public void testSubjectWithoutDiagnosticBaselineRemainsNull() {
        String studentId = studentUser.getId();

        // Student has only ConceptMastery for Chemistry (no assessment diagnostic)
        ConceptMastery cm = new ConceptMastery();
        cm.setUserId(studentId);
        cm.setSubjectCode("CHEM101");
        cm.setSubjectName("Chemistry");
        cm.setTopic("Organic Chemistry");
        cm.setConceptName("Hydrocarbons");
        cm.setAccuracy(75.0);
        cm.setMasteryScore(75.0);
        cm.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm.setMasteryLevel(ConceptMastery.MasteryLevel.PROFICIENT);
        conceptMasteryRepository.save(cm);

        AdminCohortSubjectAnalyticsDTO result = adminAnalyticsService.getCohortSubjectAnalytics();
        SubjectResearchSummaryDTO chem = result.getSubjects().stream()
                .filter(s -> s.getSubjectName().equalsIgnoreCase("Chemistry"))
                .findFirst().orElse(null);

        assertNotNull(chem);
        assertEquals(0, chem.getStudentsWithAuthenticBaseline());
        assertEquals(1, chem.getStudentsWithObservedKnowledge());
        assertNull(chem.getMeanBaselineKnowledge(), "Subject without diagnostic must have NULL baseline (never 50% fallback)");
        assertEquals(75.0, chem.getMeanCurrentKnowledge());
        assertNull(chem.getMeanGrowthPp(), "Subject without diagnostic must have NULL growth");
        assertNull(chem.getMeanNormalizedGain(), "Subject without diagnostic must have NULL normalized gain");
    }

    @Test
    public void testWeakConceptCountAndMasteryDistribution() {
        String studentId = studentUser.getId();

        // Concept 1: Weak status
        ConceptMastery c1 = new ConceptMastery();
        c1.setUserId(studentId);
        c1.setSubjectCode("BIO101");
        c1.setSubjectName("Biology");
        c1.setConceptName("Photosynthesis");
        c1.setAccuracy(65.0);
        c1.setMasteryScore(65.0);
        c1.setStatus(ConceptMastery.ConceptStatus.WEAK);
        c1.setMasteryLevel(ConceptMastery.MasteryLevel.BEGINNER);
        conceptMasteryRepository.save(c1);

        // Concept 2: Accuracy < 70%
        ConceptMastery c2 = new ConceptMastery();
        c2.setUserId(studentId);
        c2.setSubjectCode("BIO101");
        c2.setSubjectName("Biology");
        c2.setConceptName("Cell Division");
        c2.setAccuracy(60.0);
        c2.setMasteryScore(60.0);
        c2.setStatus(ConceptMastery.ConceptStatus.UNCERTAIN);
        c2.setMasteryLevel(ConceptMastery.MasteryLevel.INTERMEDIATE);
        conceptMasteryRepository.save(c2);

        // Concept 3: Strong
        ConceptMastery c3 = new ConceptMastery();
        c3.setUserId(studentId);
        c3.setSubjectCode("BIO101");
        c3.setSubjectName("Biology");
        c3.setConceptName("Genetics");
        c3.setAccuracy(95.0);
        c3.setMasteryScore(95.0);
        c3.setStatus(ConceptMastery.ConceptStatus.STRONG);
        c3.setMasteryLevel(ConceptMastery.MasteryLevel.MASTER);
        conceptMasteryRepository.save(c3);

        AdminCohortSubjectAnalyticsDTO result = adminAnalyticsService.getCohortSubjectAnalytics();
        SubjectResearchSummaryDTO bio = result.getSubjects().stream()
                .filter(s -> s.getSubjectName().equalsIgnoreCase("Biology"))
                .findFirst().orElse(null);

        assertNotNull(bio);
        assertEquals(3, bio.getConceptCount());
        assertEquals(2, bio.getWeakConceptCount(), "Concepts with status WEAK or accuracy < 70 must be counted as weak");
        assertNotNull(bio.getMasteryDistribution());
        assertEquals(1, bio.getMasteryDistribution().get("BEGINNER"));
        assertEquals(1, bio.getMasteryDistribution().get("INTERMEDIATE"));
        assertEquals(1, bio.getMasteryDistribution().get("MASTER"));
        assertEquals(0, bio.getMasteryDistribution().get("PROFICIENT"));
    }

    @Test
    public void testMissingSubjectDataIsNotConvertedToZero() {
        String studentId = studentUser.getId();

        // Student only in profile with subject "History" but no assessments, concepts, quizzes, or roadmaps
        StudentProfile sp = new StudentProfile();
        sp.setUserId(studentId);
        sp.setFullName("Test Student");
        sp.setSubjects(List.of("History"));
        studentProfileRepository.save(sp);

        AdminCohortSubjectAnalyticsDTO result = adminAnalyticsService.getCohortSubjectAnalytics();
        SubjectResearchSummaryDTO history = result.getSubjects().stream()
                .filter(s -> s.getSubjectName().equalsIgnoreCase("History"))
                .findFirst().orElse(null);

        assertNotNull(history);
        assertEquals(1, history.getStudentsRepresented());
        assertEquals(0, history.getStudentsWithAuthenticBaseline());
        assertEquals(0, history.getStudentsWithObservedKnowledge());
        assertNull(history.getMeanBaselineKnowledge());
        assertNull(history.getMeanCurrentKnowledge());
        assertNull(history.getMeanGrowthPp());
        assertNull(history.getMeanNormalizedGain());
        assertNull(history.getMeanQuizAccuracy());
        assertNull(history.getRoadmapCompletionPercentage());
        assertEquals(0, history.getConceptCount());
        assertEquals(0, history.getWeakConceptCount());
    }

    // ==========================================
    // PHASE 2C-3A FILTERING CORE TESTS
    // ==========================================

    @Test
    public void testEmptyCriteriaReturnsAllEligibleStudents() {
        String studentId = studentUser.getId();

        // Create profile for student
        StudentProfile profile = new StudentProfile();
        profile.setUserId(studentId);
        profile.setFullName("Alice Student");
        profile.setBranch("CSE");
        profile.setSemester(4);
        studentProfileRepository.save(profile);

        AdminAnalyticsFilterCriteria criteria = new AdminAnalyticsFilterCriteria();
        assertTrue(criteria.isEmpty());

        List<AdminStudentDirectoryDTO> directory = adminAnalyticsService.getStudentDirectory(criteria);
        assertNotNull(directory);
        assertEquals(1, directory.size());
        assertEquals(studentId, directory.get(0).getUserId());

        AdminCohortAnalyticsDTO cohort = adminAnalyticsService.getCohortAnalytics(criteria);
        assertNotNull(cohort);
        assertEquals(1, cohort.getTotalEnrolled());
    }

    @Test
    public void testBranchFilterSelectsOnlyMatchingProfileBranch() {
        // Alice in CSE
        StudentProfile p1 = new StudentProfile();
        p1.setUserId(studentUser.getId());
        p1.setFullName("Alice Student");
        p1.setBranch("CSE");
        p1.setSemester(4);
        studentProfileRepository.save(p1);

        // Bob in ECE
        User bob = new User();
        bob.setEmail("bob_branch_" + System.currentTimeMillis() + "@edupilot.com");
        bob.setPassword(passwordEncoder.encode("bobpass123"));
        bob.setFullName("Bob Student");
        bob.setRole(User.Role.STUDENT);
        bob.setAccountType(User.AccountType.GENUINE_STUDENT);
        bob.setCreatedAt(LocalDateTime.now());
        bob = userRepository.save(bob);

        StudentProfile p2 = new StudentProfile();
        p2.setUserId(bob.getId());
        p2.setFullName("Bob Student");
        p2.setBranch("ECE");
        p2.setSemester(4);
        studentProfileRepository.save(p2);

        // Filter for CSE
        AdminAnalyticsFilterCriteria cseCriteria = new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null);
        List<AdminStudentDirectoryDTO> cseDir = adminAnalyticsService.getStudentDirectory(cseCriteria);
        assertEquals(1, cseDir.size());
        assertEquals(studentUser.getId(), cseDir.get(0).getUserId());

        // Filter for ECE (case-insensitive)
        AdminAnalyticsFilterCriteria eceCriteria = new AdminAnalyticsFilterCriteria(null, null, "ece", null, null, null, null);
        List<AdminStudentDirectoryDTO> eceDir = adminAnalyticsService.getStudentDirectory(eceCriteria);
        assertEquals(1, eceDir.size());
        assertEquals(bob.getId(), eceDir.get(0).getUserId());

        // Filter for ME (non-existent branch)
        AdminAnalyticsFilterCriteria meCriteria = new AdminAnalyticsFilterCriteria(null, null, "ME", null, null, null, null);
        List<AdminStudentDirectoryDTO> meDir = adminAnalyticsService.getStudentDirectory(meCriteria);
        assertEquals(0, meDir.size());
    }

    @Test
    public void testCurrentSemesterFilterSelectsOnlyMatchingProfileSemester() {
        // Alice: Semester 4
        StudentProfile p1 = new StudentProfile();
        p1.setUserId(studentUser.getId());
        p1.setFullName("Alice Student");
        p1.setBranch("CSE");
        p1.setSemester(4);
        studentProfileRepository.save(p1);

        // Bob: Semester 6
        User bob = new User();
        bob.setEmail("bob_sem_" + System.currentTimeMillis() + "@edupilot.com");
        bob.setPassword(passwordEncoder.encode("bobpass123"));
        bob.setFullName("Bob Student");
        bob.setRole(User.Role.STUDENT);
        bob.setAccountType(User.AccountType.GENUINE_STUDENT);
        bob.setCreatedAt(LocalDateTime.now());
        bob = userRepository.save(bob);

        StudentProfile p2 = new StudentProfile();
        p2.setUserId(bob.getId());
        p2.setFullName("Bob Student");
        p2.setBranch("CSE");
        p2.setSemester(6);
        studentProfileRepository.save(p2);

        // Filter for Semester 4
        AdminAnalyticsFilterCriteria sem4 = new AdminAnalyticsFilterCriteria(null, null, null, 4, null, null, null);
        List<AdminStudentDirectoryDTO> dir4 = adminAnalyticsService.getStudentDirectory(sem4);
        assertEquals(1, dir4.size());
        assertEquals(studentUser.getId(), dir4.get(0).getUserId());

        // Filter for Semester 6
        AdminAnalyticsFilterCriteria sem6 = new AdminAnalyticsFilterCriteria(null, null, null, 6, null, null, null);
        List<AdminStudentDirectoryDTO> dir6 = adminAnalyticsService.getStudentDirectory(sem6);
        assertEquals(1, dir6.size());
        assertEquals(bob.getId(), dir6.get(0).getUserId());

        // Filter for Semester 8 (no students)
        AdminAnalyticsFilterCriteria sem8 = new AdminAnalyticsFilterCriteria(null, null, null, 8, null, null, null);
        List<AdminStudentDirectoryDTO> dir8 = adminAnalyticsService.getStudentDirectory(sem8);
        assertEquals(0, dir8.size());
    }

    @Test
    public void testMissingBranchDoesNotBecomeFakeBranch() {
        // Alice has no profile (branch is null)
        studentProfileRepository.deleteAll();

        AdminAnalyticsFilterCriteria branchFilter = new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null);
        List<AdminStudentDirectoryDTO> dir = adminAnalyticsService.getStudentDirectory(branchFilter);
        assertEquals(0, dir.size(), "Student with null/missing branch must not match 'CSE'");

        AdminCohortAnalyticsDTO cohort = adminAnalyticsService.getCohortAnalytics(branchFilter);
        assertEquals(0, cohort.getTotalEnrolled());
    }

    @Test
    public void testMissingSemesterHandledSafely() {
        // Alice has no profile, so semester is null
        studentProfileRepository.deleteAll();

        AdminAnalyticsFilterCriteria semFilter = new AdminAnalyticsFilterCriteria(null, null, null, 4, null, null, null);
        List<AdminStudentDirectoryDTO> dir = adminAnalyticsService.getStudentDirectory(semFilter);
        assertEquals(0, dir.size(), "Student with null semester must safely not match semester 4");
    }

    @Test
    public void testAuthenticBaselineFilterWithAndWithoutBaseline() {
        String aliceId = studentUser.getId();

        // 1. Alice has authentic diagnostic baseline
        AssessmentResult diagnostic = new AssessmentResult();
        diagnostic.setUserId(aliceId);
        diagnostic.setSubjectName("Computer Science");
        diagnostic.setScore(10);
        diagnostic.setTotalMarks(20);
        diagnostic.setPercentage(50.0);
        diagnostic.setTotalQuestions(20);
        diagnostic.setCreatedAt(LocalDateTime.now().minusDays(10));
        diagnostic.setTopicBreakdown(Map.of("OS", Map.of("percentage", 50.0, "correct", 5, "total", 10)));
        assessmentResultRepository.save(diagnostic);

        // 2. Bob has NO baseline (only quiz)
        User bob = new User();
        bob.setEmail("bob_baseline_" + System.currentTimeMillis() + "@edupilot.com");
        bob.setPassword(passwordEncoder.encode("bobpass123"));
        bob.setFullName("Bob NoBaseline");
        bob.setRole(User.Role.STUDENT);
        bob.setAccountType(User.AccountType.GENUINE_STUDENT);
        bob.setCreatedAt(LocalDateTime.now());
        bob = userRepository.save(bob);

        QuizSession qs = new QuizSession();
        qs.setUserId(bob.getId());
        qs.setSubjectName("Computer Science");
        qs.setTotalQuestions(5);
        qs.setCorrectCount(4);
        qs.setStatus(QuizSession.Status.COMPLETED);
        quizSessionRepository.save(qs);

        // Filter: hasAuthenticBaseline = true
        AdminAnalyticsFilterCriteria trueCriteria = new AdminAnalyticsFilterCriteria(null, null, null, null, null, null, true);
        List<AdminStudentDirectoryDTO> withBaseline = adminAnalyticsService.getStudentDirectory(trueCriteria);
        assertEquals(1, withBaseline.size());
        assertEquals(aliceId, withBaseline.get(0).getUserId());
        assertTrue(withBaseline.get(0).isHasAuthenticBaseline());

        // Filter: hasAuthenticBaseline = false
        AdminAnalyticsFilterCriteria falseCriteria = new AdminAnalyticsFilterCriteria(null, null, null, null, null, null, false);
        List<AdminStudentDirectoryDTO> withoutBaseline = adminAnalyticsService.getStudentDirectory(falseCriteria);
        assertEquals(1, withoutBaseline.size());
        assertEquals(bob.getId(), withoutBaseline.get(0).getUserId());
        assertFalse(withoutBaseline.get(0).isHasAuthenticBaseline());
    }

    @Test
    public void testActivityStatusFilterAcrossCategories() {
        // Setup 4 students: Active, At-Risk, Inactive, No Activity
        String aliceId = studentUser.getId(); // Active (study session 2 days ago)
        StudySession ssAlice = new StudySession();
        ssAlice.setUserId(aliceId);
        ssAlice.setStartTime(LocalDateTime.now().minusDays(2));
        ssAlice.setEndTime(LocalDateTime.now().minusDays(2).plusHours(1));
        studySessionRepository.save(ssAlice);

        User bob = new User(); // At Risk (quiz 10 days ago)
        bob.setEmail("bob_act_" + System.currentTimeMillis() + "@edupilot.com");
        bob.setPassword(passwordEncoder.encode("bobpass123"));
        bob.setFullName("Bob AtRisk");
        bob.setRole(User.Role.STUDENT);
        bob.setAccountType(User.AccountType.GENUINE_STUDENT);
        bob.setCreatedAt(LocalDateTime.now().minusDays(20));
        bob = userRepository.save(bob);
        QuizSession qsBob = new QuizSession();
        qsBob.setUserId(bob.getId());
        qsBob.setLastAnswerTime(LocalDateTime.now().minusDays(10));
        qsBob.setStatus(QuizSession.Status.COMPLETED);
        quizSessionRepository.save(qsBob);

        User charlie = new User(); // Inactive (assessment 25 days ago)
        charlie.setEmail("charlie_act_" + System.currentTimeMillis() + "@edupilot.com");
        charlie.setPassword(passwordEncoder.encode("charliepass123"));
        charlie.setFullName("Charlie Inactive");
        charlie.setRole(User.Role.STUDENT);
        charlie.setAccountType(User.AccountType.GENUINE_STUDENT);
        charlie.setCreatedAt(LocalDateTime.now().minusDays(30));
        charlie = userRepository.save(charlie);
        AssessmentResult arCharlie = new AssessmentResult();
        arCharlie.setUserId(charlie.getId());
        arCharlie.setCreatedAt(LocalDateTime.now().minusDays(25));
        arCharlie.setScore(50);
        assessmentResultRepository.save(arCharlie);

        User david = new User(); // No activity
        david.setEmail("david_act_" + System.currentTimeMillis() + "@edupilot.com");
        david.setPassword(passwordEncoder.encode("davidpass123"));
        david.setFullName("David NoAct");
        david.setRole(User.Role.STUDENT);
        david.setAccountType(User.AccountType.GENUINE_STUDENT);
        david.setCreatedAt(LocalDateTime.now());
        david = userRepository.save(david);

        // ACTIVE
        AdminAnalyticsFilterCriteria actCriteria = new AdminAnalyticsFilterCriteria(null, null, null, null, null, "ACTIVE", null);
        List<AdminStudentDirectoryDTO> actList = adminAnalyticsService.getStudentDirectory(actCriteria);
        assertEquals(1, actList.size());
        assertEquals(aliceId, actList.get(0).getUserId());

        // AT_RISK
        AdminAnalyticsFilterCriteria riskCriteria = new AdminAnalyticsFilterCriteria(null, null, null, null, null, "AT_RISK", null);
        List<AdminStudentDirectoryDTO> riskList = adminAnalyticsService.getStudentDirectory(riskCriteria);
        assertEquals(1, riskList.size());
        assertEquals(bob.getId(), riskList.get(0).getUserId());

        // INACTIVE
        AdminAnalyticsFilterCriteria inactCriteria = new AdminAnalyticsFilterCriteria(null, null, null, null, null, "INACTIVE", null);
        List<AdminStudentDirectoryDTO> inactList = adminAnalyticsService.getStudentDirectory(inactCriteria);
        assertEquals(1, inactList.size());
        assertEquals(charlie.getId(), inactList.get(0).getUserId());

        // NO_ACTIVITY
        AdminAnalyticsFilterCriteria noActCriteria = new AdminAnalyticsFilterCriteria(null, null, null, null, null, "NO_ACTIVITY", null);
        List<AdminStudentDirectoryDTO> noActList = adminAnalyticsService.getStudentDirectory(noActCriteria);
        assertEquals(1, noActList.size());
        assertEquals(david.getId(), noActList.get(0).getUserId());
    }

    @Test
    public void testDateCriteriaBoundsOnHistoricalTrends() {
        String studentId = studentUser.getId();

        // 3 assessments: Day -30, Day -15, Day -5
        AssessmentResult a1 = new AssessmentResult();
        a1.setUserId(studentId);
        a1.setSubjectName("Math");
        a1.setScore(40);
        a1.setTotalMarks(100);
        a1.setPercentage(40.0);
        a1.setCreatedAt(LocalDateTime.now().minusDays(30));
        assessmentResultRepository.save(a1);

        AssessmentResult a2 = new AssessmentResult();
        a2.setUserId(studentId);
        a2.setSubjectName("Math");
        a2.setScore(60);
        a2.setTotalMarks(100);
        a2.setPercentage(60.0);
        a2.setCreatedAt(LocalDateTime.now().minusDays(15));
        assessmentResultRepository.save(a2);

        AssessmentResult a3 = new AssessmentResult();
        a3.setUserId(studentId);
        a3.setSubjectName("Math");
        a3.setScore(80);
        a3.setTotalMarks(100);
        a3.setPercentage(80.0);
        a3.setCreatedAt(LocalDateTime.now().minusDays(5));
        assessmentResultRepository.save(a3);

        // 1. startDate only (Day -20 to now) -> includes a2 (Day -15) and a3 (Day -5)
        AdminAnalyticsFilterCriteria startOnly = new AdminAnalyticsFilterCriteria();
        startOnly.setStartDate(LocalDate.now().minusDays(20));
        AdminResearchTrendsDTO trendsStart = adminAnalyticsService.getResearchTrends(startOnly);
        assertEquals(2, trendsStart.getTotalAssessments());
        assertEquals(2, trendsStart.getObservations().size());

        // 2. endDate only (up to Day -10) -> includes a1 (Day -30) and a2 (Day -15)
        AdminAnalyticsFilterCriteria endOnly = new AdminAnalyticsFilterCriteria();
        endOnly.setEndDate(LocalDate.now().minusDays(10));
        AdminResearchTrendsDTO trendsEnd = adminAnalyticsService.getResearchTrends(endOnly);
        assertEquals(2, trendsEnd.getTotalAssessments());
        assertEquals(2, trendsEnd.getObservations().size());

        // 3. Both start and end (Day -20 to Day -10) -> includes only a2 (Day -15)
        AdminAnalyticsFilterCriteria both = new AdminAnalyticsFilterCriteria();
        both.setStartDate(LocalDate.now().minusDays(20));
        both.setEndDate(LocalDate.now().minusDays(10));
        AdminResearchTrendsDTO trendsBoth = adminAnalyticsService.getResearchTrends(both);
        assertEquals(1, trendsBoth.getTotalAssessments());
        assertEquals(1, trendsBoth.getObservations().size());
        assertEquals(60.0, trendsBoth.getObservations().get(0).getMeanAssessmentScore());

        // 4. No date filter -> includes all 3
        AdminResearchTrendsDTO trendsAll = adminAnalyticsService.getResearchTrends(new AdminAnalyticsFilterCriteria());
        assertEquals(3, trendsAll.getTotalAssessments());
        assertEquals(3, trendsAll.getObservations().size());
    }

    @Test
    public void testInvalidDateRangeThrowsException() {
        AdminAnalyticsFilterCriteria criteria = new AdminAnalyticsFilterCriteria();
        criteria.setStartDate(LocalDate.now());

        assertThrows(IllegalArgumentException.class, () -> {
            criteria.setEndDate(LocalDate.now().minusDays(5));
        });

        assertThrows(IllegalArgumentException.class, () -> {
            new AdminAnalyticsFilterCriteria(LocalDate.now(), LocalDate.now().minusDays(1), null, null, null, null, null);
        });
    }

    @Test
    public void testK0ImmutabilityUnderDateFiltering() {
        String studentId = studentUser.getId();

        // 1. Diagnostic assessment in January (Day -120) with score 45.0%
        AssessmentResult diagnostic = new AssessmentResult();
        diagnostic.setUserId(studentId);
        diagnostic.setSubjectCode("CS101");
        diagnostic.setSubjectName("Computer Science");
        diagnostic.setScore(45);
        diagnostic.setTotalMarks(100);
        diagnostic.setPercentage(45.0);
        diagnostic.setTotalQuestions(20);
        diagnostic.setCreatedAt(LocalDateTime.now().minusDays(120));
        diagnostic.setTopicBreakdown(Map.of("Algorithms", Map.of("percentage", 45.0, "correct", 9, "total", 20)));
        assessmentResultRepository.save(diagnostic);

        // 2. Later assessment in August (Day -40) with score 75.0%
        AssessmentResult laterAssessment = new AssessmentResult();
        laterAssessment.setUserId(studentId);
        laterAssessment.setSubjectCode("CS101");
        laterAssessment.setSubjectName("Computer Science");
        laterAssessment.setScore(75);
        laterAssessment.setTotalMarks(100);
        laterAssessment.setPercentage(75.0);
        laterAssessment.setTotalQuestions(20);
        laterAssessment.setCreatedAt(LocalDateTime.now().minusDays(40));
        assessmentResultRepository.save(laterAssessment);

        // 3. Current concept mastery: 85.0%
        ConceptMastery cm = new ConceptMastery();
        cm.setUserId(studentId);
        cm.setSubjectCode("CS101");
        cm.setSubjectName("Computer Science");
        cm.setConceptName("Algorithms");
        cm.setAccuracy(85.0);
        cm.setMasteryScore(85.0);
        cm.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm.setLastAssessedAt(LocalDateTime.now().minusDays(10));
        conceptMasteryRepository.save(cm);

        // Apply a date filter restricted to July–October (Day -60 to now), which excludes the January diagnostic
        AdminAnalyticsFilterCriteria filterJulyOctober = new AdminAnalyticsFilterCriteria();
        filterJulyOctober.setStartDate(LocalDate.now().minusDays(60));
        filterJulyOctober.setEndDate(LocalDate.now());

        // Verify Subject Research Analytics under date filtering
        AdminCohortSubjectAnalyticsDTO subjectAnalytics = adminAnalyticsService.getCohortSubjectAnalytics(filterJulyOctober);
        assertNotNull(subjectAnalytics);
        assertEquals(1, subjectAnalytics.getTotalSubjectsCount());

        SubjectResearchSummaryDTO csSummary = subjectAnalytics.getSubjects().get(0);
        assertEquals("CS101", csSummary.getSubjectCode());

        // CRITICAL RESEARCH INTEGRITY ASSERTION:
        // Baseline K0 MUST REMAIN 45.0% from the January diagnostic.
        // It must NEVER be redefined or changed to 75.0% because of the date range filter!
        assertEquals(45.0, csSummary.getMeanBaselineKnowledge(),
                "K0 must be permanently derived from globally earliest diagnostic (45.0%), never redefined inside date filter range");
        assertEquals(85.0, csSummary.getMeanCurrentKnowledge());
        assertEquals(40.0, csSummary.getMeanGrowthPp());
    }

    @Test
    public void testSubjectCodeFilterMatchingIsDeterministic() {
        String studentId = studentUser.getId();

        // S1 enrolled in CS101 and MATH101
        StudentProfile p = new StudentProfile();
        p.setUserId(studentId);
        p.setSubjects(List.of("Data Structures", "Mathematics"));
        studentProfileRepository.save(p);

        // CS101 Concept Mastery
        ConceptMastery cm1 = new ConceptMastery();
        cm1.setUserId(studentId);
        cm1.setSubjectCode("CS101");
        cm1.setSubjectName("Data Structures");
        cm1.setConceptName("Queues");
        cm1.setAccuracy(80.0);
        cm1.setMasteryScore(80.0);
        cm1.setLastAssessedAt(LocalDateTime.now());
        conceptMasteryRepository.save(cm1);

        // MATH101 Concept Mastery
        ConceptMastery cm2 = new ConceptMastery();
        cm2.setUserId(studentId);
        cm2.setSubjectCode("MATH101");
        cm2.setSubjectName("Mathematics");
        cm2.setConceptName("Calculus");
        cm2.setAccuracy(90.0);
        cm2.setMasteryScore(90.0);
        cm2.setLastAssessedAt(LocalDateTime.now());
        conceptMasteryRepository.save(cm2);

        // Filter: subjectCode = "CS101"
        AdminAnalyticsFilterCriteria csCriteria = new AdminAnalyticsFilterCriteria();
        csCriteria.setSubjectCode("CS101");

        AdminCohortSubjectAnalyticsDTO csResult = adminAnalyticsService.getCohortSubjectAnalytics(csCriteria);
        assertEquals(1, csResult.getTotalSubjectsCount());
        assertEquals("CS101", csResult.getSubjects().get(0).getSubjectCode());

        // Filter: subjectCode = "MATH101"
        AdminAnalyticsFilterCriteria mathCriteria = new AdminAnalyticsFilterCriteria();
        mathCriteria.setSubjectCode("MATH101");

        AdminCohortSubjectAnalyticsDTO mathResult = adminAnalyticsService.getCohortSubjectAnalytics(mathCriteria);
        assertEquals(1, mathResult.getTotalSubjectsCount());
        assertEquals("MATH101", mathResult.getSubjects().get(0).getSubjectCode());
    }

    @Test
    public void testNoSyntheticObservationsCreatedWhenEmpty() {
        // Date range with zero activity
        AdminAnalyticsFilterCriteria futureFilter = new AdminAnalyticsFilterCriteria();
        futureFilter.setStartDate(LocalDate.now().plusDays(10));
        futureFilter.setEndDate(LocalDate.now().plusDays(20));

        AdminResearchTrendsDTO trends = adminAnalyticsService.getResearchTrends(futureFilter);
        assertNotNull(trends);
        assertEquals(0, trends.getTotalObservations());
        assertEquals(0, trends.getTotalAssessments());
        assertEquals(0, trends.getObservations().size(), "No fabricated or synthetic trend points should be created");
    }

    // ==========================================
    // PHASE 2C-3B: CONTROLLER QUERY PARAMETER INTEGRATION TESTS
    // ==========================================

    @Test
    public void testController_ExistingEndpointsWithoutQueryParamsReturn200() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/analytics/trends")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/analytics/subjects")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/analytics/students")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testController_OverviewAcceptsBranchFilter() throws Exception {
        studentService.onboardStudent(
                studentUser.getId(), "CSE", 3, List.of("CS101"),
                List.of("Engineer"), 3.0, 8.0, 7.0, 4.0, 25, "Visual"
        );

        MvcResult resultMatch = mockMvc.perform(get("/api/admin/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("branch", "CSE"))
                .andExpect(status().isOk())
                .andReturn();

        AdminAnalyticsOverviewDTO dtoMatch = objectMapper.readValue(resultMatch.getResponse().getContentAsString(), AdminAnalyticsOverviewDTO.class);
        assertEquals(1, dtoMatch.getTotalStudents());

        MvcResult resultMismatch = mockMvc.perform(get("/api/admin/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("branch", "ECE"))
                .andExpect(status().isOk())
                .andReturn();

        AdminAnalyticsOverviewDTO dtoMismatch = objectMapper.readValue(resultMismatch.getResponse().getContentAsString(), AdminAnalyticsOverviewDTO.class);
        assertEquals(0, dtoMismatch.getTotalStudents());
    }

    @Test
    public void testController_CohortAcceptsBranchAndSemesterFilter() throws Exception {
        studentService.onboardStudent(
                studentUser.getId(), "IT", 4, List.of("IT201"),
                List.of("Analyst"), 3.0, 8.0, 7.0, 4.0, 25, "Visual"
        );

        // Branch match
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("branch", "IT"))
                .andExpect(status().isOk());

        // Semester match
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("semester", "4"))
                .andExpect(status().isOk());

        // Semester mismatch
        MvcResult semMismatchResult = mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("semester", "6"))
                .andExpect(status().isOk())
                .andReturn();
        AdminCohortAnalyticsDTO semDto = objectMapper.readValue(semMismatchResult.getResponse().getContentAsString(), AdminCohortAnalyticsDTO.class);
        assertEquals(0, semDto.getTotalEnrolled());
    }

    @Test
    public void testController_TrendsAcceptsDateRange() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/trends")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    public void testController_SubjectsAcceptsSubjectCode() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/subjects")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("subjectCode", "CS101"))
                .andExpect(status().isOk());
    }

    @Test
    public void testController_StudentDirectoryAcceptsActivityStatus() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("activityStatus", "NO_ACTIVITY"))
                .andExpect(status().isOk());
    }

    @Test
    public void testController_HasAuthenticBaselineFilter() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("hasAuthenticBaseline", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("hasAuthenticBaseline", "false"))
                .andExpect(status().isOk());
    }

    @Test
    public void testController_MultipleFiltersCanBeCombined() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("branch", "CSE")
                        .param("semester", "3")
                        .param("subjectCode", "CS101")
                        .param("activityStatus", "ACTIVE")
                        .param("hasAuthenticBaseline", "true")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    public void testController_InvalidStartDateReturns400() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testController_InvalidEndDateReturns400() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("endDate", "2026-99-99"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testController_StartDateAfterEndDateReturns400() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-09-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testController_InvalidSemesterReturns400() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("semester", "third-semester"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testController_InvalidActivityStatusReturns400() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("activityStatus", "SUPER_ACTIVE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testController_InvalidHasAuthenticBaselineReturns400() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("hasAuthenticBaseline", "not-a-bool"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testController_SecurityAuthorizationOnFilteredEndpoints() throws Exception {
        // Student role -> 403 Forbidden
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + studentToken)
                        .param("branch", "CSE"))
                .andExpect(status().isForbidden());

        // Faculty role -> 403 Forbidden
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + facultyToken)
                        .param("branch", "CSE"))
                .andExpect(status().isForbidden());

        // Unauthenticated -> 401 Unauthorized
        mockMvc.perform(get("/api/admin/analytics/cohort")
                        .param("branch", "CSE"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testController_K0RemainsUnchangedUnderDateFilteringViaHttp() throws Exception {
        String studentId = studentUser.getId();
        studentService.onboardStudent(
                studentId, "CSE", 3, List.of("CS101"),
                List.of("Dev"), 3.0, 8.0, 7.0, 4.0, 25, "Visual"
        );

        // 1. Diagnostic in January 2026 (K0 = 40%)
        AssessmentResult diagnostic = new AssessmentResult();
        diagnostic.setUserId(studentId);
        diagnostic.setSubjectName("CS101");
        diagnostic.setSubjectCode("CS101");
        diagnostic.setScore(40);
        diagnostic.setPercentage(40.0);
        diagnostic.setCreatedAt(LocalDateTime.of(2026, 1, 15, 10, 0));
        diagnostic.setTopicBreakdown(Map.of(
                "TopicA", Map.of("correct", 4, "total", 10, "percentage", 40.0)
        ));
        assessmentResultRepository.save(diagnostic);

        // 2. Current ConceptMastery in September 2026 (Kt = 80%)
        ConceptMastery cm = new ConceptMastery();
        cm.setUserId(studentId);
        cm.setSubjectName("CS101");
        cm.setTopic("TopicA");
        cm.setConceptName("TopicA");
        cm.setAccuracy(80.0);
        cm.setMasteryScore(80.0);
        cm.setStatus(ConceptMastery.ConceptStatus.STRONG);
        cm.setLastAssessedAt(LocalDateTime.of(2026, 9, 15, 10, 30));
        conceptMasteryRepository.save(cm);

        // Request cohort filtered to July–October 2026
        MvcResult mvcResult = mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-10-31"))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortAnalyticsDTO cohort = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), AdminCohortAnalyticsDTO.class);
        assertNotNull(cohort);
        assertNotNull(cohort.getGrowthDistribution());
        // Mean baseline knowledge should remain 40.0% (derived globally from January diagnostic)
        assertEquals(40.0, cohort.getMeanBaselineKnowledge(), 0.01);
    }

    @Test
    public void testController_ExistingUnfilteredResponseSchemasRemainUnchanged() throws Exception {
        MvcResult overviewRes = mockMvc.perform(get("/api/admin/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        AdminAnalyticsOverviewDTO overview = objectMapper.readValue(overviewRes.getResponse().getContentAsString(), AdminAnalyticsOverviewDTO.class);
        assertNotNull(overview);
        assertTrue(overview.getTotalStudents() >= 0);

        MvcResult cohortRes = mockMvc.perform(get("/api/admin/analytics/cohort")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        AdminCohortAnalyticsDTO cohort = objectMapper.readValue(cohortRes.getResponse().getContentAsString(), AdminCohortAnalyticsDTO.class);
        assertNotNull(cohort);
        assertNotNull(cohort.getGrowthDistribution());
        assertNotNull(cohort.getSatisfaction());

        MvcResult trendsRes = mockMvc.perform(get("/api/admin/analytics/trends")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        AdminResearchTrendsDTO trends = objectMapper.readValue(trendsRes.getResponse().getContentAsString(), AdminResearchTrendsDTO.class);
        assertNotNull(trends);
        assertNotNull(trends.getObservations());

        MvcResult subjectsRes = mockMvc.perform(get("/api/admin/analytics/subjects")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        AdminCohortSubjectAnalyticsDTO subjects = objectMapper.readValue(subjectsRes.getResponse().getContentAsString(), AdminCohortSubjectAnalyticsDTO.class);
        assertNotNull(subjects);
        assertNotNull(subjects.getSubjects());

        MvcResult studentsRes = mockMvc.perform(get("/api/admin/analytics/students")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<AdminStudentDirectoryDTO> directory = objectMapper.readValue(studentsRes.getResponse().getContentAsString(), new TypeReference<List<AdminStudentDirectoryDTO>>() {});
        assertNotNull(directory);
    }

    // =========================================================================
    // PHASE 2C-3D: DESCRIPTIVE COHORT COMPARISON INTEGRATION TESTS
    // =========================================================================

    private String createStudentWithProfile(String email, String branch, int semester) {
        User u = new User();
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("pass123"));
        u.setFullName("Student " + email);
        u.setRole(User.Role.STUDENT);
        u.setAccountType(User.AccountType.GENUINE_STUDENT);
        u.setCreatedAt(LocalDateTime.now());
        u = userRepository.save(u);

        StudentProfile profile = new StudentProfile();
        profile.setUserId(u.getId());
        profile.setEmail(email);
        profile.setFullName("Student " + email);
        profile.setBranch(branch);
        profile.setSemester(semester);
        profile.setSubjects(List.of("CS101", "Mathematics"));
        studentProfileRepository.save(profile);

        return u.getId();
    }

    private void seedAuthenticBaseline(String userId, String subject, double score) {
        AssessmentResult ar = new AssessmentResult();
        ar.setUserId(userId);
        ar.setSubjectName(subject);
        ar.setSubjectCode(subject);
        ar.setScore((int) score);
        ar.setTotalMarks(100);
        ar.setPercentage(score);
        ar.setTotalQuestions(10);
        ar.setCorrectAnswers((int) (score / 10));
        ar.setCreatedAt(LocalDateTime.of(2026, 1, 15, 10, 0));
        ar.setTopicBreakdown(Map.of(
                "Algorithms", Map.of("correct", (int) (score / 10), "total", 10, "percentage", score)
        ));
        assessmentResultRepository.save(ar);
    }

    private void seedConceptMastery(String userId, String subject, String concept, double score) {
        ConceptMastery cm = new ConceptMastery();
        cm.setUserId(userId);
        cm.setSubjectName(subject);
        cm.setSubjectCode(subject);
        cm.setTopic(concept);
        cm.setConceptName(concept);
        cm.setAccuracy(score);
        cm.setMasteryScore(score);
        cm.setStatus(score >= 70 ? ConceptMastery.ConceptStatus.STRONG : ConceptMastery.ConceptStatus.UNCERTAIN);
        cm.setLastAssessedAt(LocalDateTime.of(2026, 9, 15, 10, 0));
        conceptMasteryRepository.save(cm);
    }

    @Test
    public void testCompare_AdminAuthorized_Returns200() throws Exception {
        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "IT", null, null, null, null),
                null, null
        );

        mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    public void testCompare_StudentForbidden_Returns403() throws Exception {
        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(),
                new AdminAnalyticsFilterCriteria(),
                null, null
        );

        mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCompare_FacultyForbidden_Returns403() throws Exception {
        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(),
                new AdminAnalyticsFilterCriteria(),
                null, null
        );

        mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + facultyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCompare_Unauthenticated_Returns401() throws Exception {
        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(),
                new AdminAnalyticsFilterCriteria(),
                null, null
        );

        mockMvc.perform(post("/api/admin/analytics/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCompare_SameCohortComparedWithItself_ZeroDifferences() throws Exception {
        String studentId = createStudentWithProfile("same@edupilot.com", "CSE", 7);
        seedAuthenticBaseline(studentId, "CS101", 50.0);
        seedConceptMastery(studentId, "CS101", "Algorithms", 80.0);

        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", 7, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "CSE", 7, null, null, null),
                null, null
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertEquals(0, comp.getDifferences().getTotalEnrolledDiff());
        assertEquals(0, comp.getDifferences().getEvaluatedCohortSizeDiff());
        assertEquals(0.0, comp.getDifferences().getBaselineKnowledgeDiffPp(), 0.01);
        assertEquals(0.0, comp.getDifferences().getCurrentKnowledgeDiffPp(), 0.01);
        assertNotNull(comp.getMethodologyNote());
        assertTrue(comp.getMethodologyNote().contains("Descriptive cohort comparison"));
    }

    @Test
    public void testCompare_EmptyCohortA_GracefulWarning() throws Exception {
        createStudentWithProfile("b@edupilot.com", "IT", 7);

        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "NON_EXISTENT", null, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "IT", null, null, null, null),
                null, null
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertEquals(0, comp.getCohortA().getMetrics().getTotalEnrolled());
        assertEquals(1, comp.getCohortB().getMetrics().getTotalEnrolled());
        assertFalse(comp.getDataSufficiency().isComparisonValid());
        assertTrue(comp.getDataSufficiency().getWarnings().stream().anyMatch(w -> w.contains("Cohort A has 0 enrolled students")));
    }

    @Test
    public void testCompare_EmptyCohortB_GracefulWarning() throws Exception {
        createStudentWithProfile("a@edupilot.com", "CSE", 7);

        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "NON_EXISTENT", null, null, null, null),
                null, null
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertEquals(1, comp.getCohortA().getMetrics().getTotalEnrolled());
        assertEquals(0, comp.getCohortB().getMetrics().getTotalEnrolled());
        assertFalse(comp.getDataSufficiency().isComparisonValid());
        assertTrue(comp.getDataSufficiency().getWarnings().stream().anyMatch(w -> w.contains("Cohort B has 0 enrolled students")));
    }

    @Test
    public void testCompare_NoAuthenticBaseline_GainNullUnavailable() throws Exception {
        String studentId = createStudentWithProfile("nobaseline@edupilot.com", "CSE", 7);
        seedConceptMastery(studentId, "CS101", "Algorithms", 75.0);

        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                null, null
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertEquals(1, comp.getCohortA().getMetrics().getTotalEnrolled());
        assertEquals(0, comp.getCohortA().getMetrics().getBaselineSampleSize());
        assertNull(comp.getCohortA().getMetrics().getMeanBaselineKnowledge());
        assertNull(comp.getCohortA().getMetrics().getMeanNormalizedGain());
        assertNull(comp.getDifferences().getBaselineKnowledgeDiffPp());
        assertNull(comp.getDifferences().getNormalizedGainDiff());
    }

    @Test
    public void testCompare_MissingKt_ExcludedFromCurrentGainDenominators() throws Exception {
        String studentId = createStudentWithProfile("nokt@edupilot.com", "CSE", 7);
        seedAuthenticBaseline(studentId, "CS101", 40.0);

        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                null, null
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertEquals(1, comp.getCohortA().getMetrics().getTotalEnrolled());
        assertEquals(1, comp.getCohortA().getMetrics().getBaselineSampleSize());
        assertEquals(40.0, comp.getCohortA().getMetrics().getMeanBaselineKnowledge(), 0.01);
    }

    @Test
    public void testCompare_DifferentBranches_ComputesIndependentPopulations() throws Exception {
        String sCse = createStudentWithProfile("cse@edupilot.com", "CSE", 7);
        seedAuthenticBaseline(sCse, "CS101", 40.0);
        seedConceptMastery(sCse, "CS101", "Algorithms", 80.0);

        String sIt = createStudentWithProfile("it@edupilot.com", "IT", 7);
        seedAuthenticBaseline(sIt, "CS101", 50.0);
        seedConceptMastery(sIt, "CS101", "Algorithms", 70.0);

        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "IT", null, null, null, null),
                null, null
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertEquals(1, comp.getCohortA().getMetrics().getTotalEnrolled());
        assertEquals(1, comp.getCohortB().getMetrics().getTotalEnrolled());
        assertEquals(40.0, comp.getCohortA().getMetrics().getMeanBaselineKnowledge(), 0.01);
        assertEquals(50.0, comp.getCohortB().getMetrics().getMeanBaselineKnowledge(), 0.01);
        assertEquals(-10.0, comp.getDifferences().getBaselineKnowledgeDiffPp(), 0.01);
    }

    @Test
    public void testCompare_DifferentSemesters_ComputesIndependentPopulations() throws Exception {
        String sSem5 = createStudentWithProfile("sem5@edupilot.com", "CSE", 5);
        seedAuthenticBaseline(sSem5, "CS101", 30.0);

        String sSem7 = createStudentWithProfile("sem7@edupilot.com", "CSE", 7);
        seedAuthenticBaseline(sSem7, "CS101", 60.0);

        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", 5, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "CSE", 7, null, null, null),
                null, null
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertEquals(1, comp.getCohortA().getMetrics().getTotalEnrolled());
        assertEquals(1, comp.getCohortB().getMetrics().getTotalEnrolled());
        assertEquals(30.0, comp.getCohortA().getMetrics().getMeanBaselineKnowledge(), 0.01);
        assertEquals(60.0, comp.getCohortB().getMetrics().getMeanBaselineKnowledge(), 0.01);
        assertEquals(-30.0, comp.getDifferences().getBaselineKnowledgeDiffPp(), 0.01);
    }

    @Test
    public void testCompare_SubjectFilteredComparison_EvaluatesCanonicalSubject() throws Exception {
        String s1 = createStudentWithProfile("subj1@edupilot.com", "CSE", 7);
        seedAuthenticBaseline(s1, "CS101", 45.0);
        seedConceptMastery(s1, "CS101", "Algorithms", 85.0);

        String s2 = createStudentWithProfile("subj2@edupilot.com", "IT", 7);
        seedAuthenticBaseline(s2, "CS101", 55.0);
        seedConceptMastery(s2, "CS101", "Algorithms", 75.0);

        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, "CS101", null, null),
                new AdminAnalyticsFilterCriteria(null, null, "IT", null, "CS101", null, null),
                null, null
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertEquals(1, comp.getCohortA().getMetrics().getTotalEnrolled());
        assertEquals(1, comp.getCohortB().getMetrics().getTotalEnrolled());
        assertEquals(45.0, comp.getCohortA().getMetrics().getMeanBaselineKnowledge(), 0.01);
        assertEquals(55.0, comp.getCohortB().getMetrics().getMeanBaselineKnowledge(), 0.01);
        assertEquals(-10.0, comp.getDifferences().getBaselineKnowledgeDiffPp(), 0.01);
    }

    @Test
    public void testCompare_SharedDateFilter_AppliesSameObservationWindow() throws Exception {
        String s1 = createStudentWithProfile("date1@edupilot.com", "CSE", 7);
        seedAuthenticBaseline(s1, "CS101", 40.0);

        // Seed satisfaction inside window
        StudentSatisfaction sat = new StudentSatisfaction();
        sat.setStudentId(s1);
        sat.setRating(5);
        sat.setFeedbackType(StudentSatisfaction.FeedbackType.AI_TUTOR);
        sat.setTimestamp(LocalDateTime.of(2026, 8, 15, 12, 0));
        satisfactionRepository.save(sat);

        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertEquals(1, comp.getCohortA().getMetrics().getSatisfaction().getTotalReviews());
        assertEquals(5.0, comp.getCohortA().getMetrics().getSatisfaction().getAverageRating(), 0.01);
    }

    @Test
    public void testCompare_K0RemainsGloballyAnchored_WhenDateExcludesOriginalDiagnostic() throws Exception {
        String s1 = createStudentWithProfile("globalk0@edupilot.com", "CSE", 7);

        // Earliest diagnostic in January 2026 (K0 = 35.0%)
        AssessmentResult diag = new AssessmentResult();
        diag.setUserId(s1);
        diag.setSubjectName("CS101");
        diag.setScore(35);
        diag.setTotalMarks(100);
        diag.setPercentage(35.0);
        diag.setTotalQuestions(10);
        diag.setCorrectAnswers(3);
        diag.setCreatedAt(LocalDateTime.of(2026, 1, 10, 10, 0));
        diag.setTopicBreakdown(Map.of(
                "Algorithms", Map.of("correct", 3, "total", 10, "percentage", 35.0)
        ));
        assessmentResultRepository.save(diag);

        // Observation window: July - October 2026
        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 10, 31)
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertEquals(35.0, comp.getCohortA().getMetrics().getMeanBaselineKnowledge(), 0.01);
    }

    @Test
    public void testCompare_InvalidDateRange_Returns400() throws Exception {
        String invalidDateJson = "{\"cohortA\":{},\"cohortB\":{},\"startDate\":\"2026-10-31\",\"endDate\":\"2026-01-01\"}";

        mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidDateJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCompare_InvalidCriteria_Returns400() throws Exception {
        String invalidCriteriaJson = "{\"cohortA\":{\"activityStatus\":\"INVALID_STATUS\"},\"cohortB\":{}}";

        mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCriteriaJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCompare_MissingRequestBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCompare_NoSynthetic50PercentBaseline() throws Exception {
        createStudentWithProfile("nosynth@edupilot.com", "CSE", 7);
        // Student with no diagnostic assessment
        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                null, null
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertNull(comp.getCohortA().getMetrics().getMeanBaselineKnowledge());
        assertEquals(0, comp.getCohortA().getMetrics().getBaselineSampleSize());
    }

    @Test
    public void testCompare_MetricSpecificDenominatorsAreTracked() throws Exception {
        // Student 1: Has baseline + current knowledge + satisfaction
        String s1 = createStudentWithProfile("denom1@edupilot.com", "CSE", 7);
        seedAuthenticBaseline(s1, "CS101", 50.0);
        seedConceptMastery(s1, "CS101", "Algorithms", 80.0);
        StudentSatisfaction sat = new StudentSatisfaction();
        sat.setStudentId(s1);
        sat.setRating(5);
        satisfactionRepository.save(sat);

        // Student 2: Only concept mastery (no baseline)
        String s2 = createStudentWithProfile("denom2@edupilot.com", "CSE", 7);
        seedConceptMastery(s2, "CS101", "Algorithms", 70.0);

        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                null, null
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertEquals(2, comp.getCohortA().getMetrics().getTotalEnrolled());
        assertEquals(1, comp.getCohortA().getMetrics().getBaselineSampleSize());
        assertEquals(2, comp.getCohortA().getMetrics().getCurrentKnowledgeSampleSize());
        assertEquals(1, comp.getCohortA().getMetrics().getLearningGainSampleSize());
        assertEquals(1, comp.getCohortA().getMetrics().getSatisfactionSampleSize());
    }

    @Test
    public void testCompare_SmallCohort_ProducesWarningNotSignificanceClaim() throws Exception {
        String s1 = createStudentWithProfile("small@edupilot.com", "CSE", 7);
        seedAuthenticBaseline(s1, "CS101", 50.0);

        AdminCohortComparisonRequest req = new AdminCohortComparisonRequest(
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                new AdminAnalyticsFilterCriteria(null, null, "CSE", null, null, null, null),
                null, null
        );

        MvcResult result = mockMvc.perform(post("/api/admin/analytics/compare")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AdminCohortComparisonDTO comp = objectMapper.readValue(result.getResponse().getContentAsString(), AdminCohortComparisonDTO.class);
        assertNotNull(comp);
        assertTrue(comp.getDataSufficiency().getWarnings().stream().anyMatch(w -> w.contains("small evaluated sample size (N < 5)")));
        assertTrue(comp.getMethodologyNote().contains("should not be interpreted as causal effects"));
    }

    // =========================================================================
    // EXPORT INTEGRATION TESTS (PHASE 2C-4)
    // =========================================================================

    @Test
    public void testExportExcel_Admin_ReturnsOkAndValidXlsxWorkbook() throws Exception {
        String s1 = createStudentWithProfile("excel_export1@edupilot.com", "CSE", 3);
        seedAuthenticBaseline(s1, "CS101", 60.0);
        seedConceptMastery(s1, "CS101", "Trees", 80.0);

        MvcResult result = mockMvc.perform(get("/api/admin/analytics/export/excel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertNotNull(content);
        assertTrue(content.length > 0);

        String contentType = result.getResponse().getContentType();
        assertNotNull(contentType);
        assertTrue(contentType.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        String disposition = result.getResponse().getHeader("Content-Disposition");
        assertNotNull(disposition);
        assertTrue(disposition.contains("attachment"));
        assertTrue(disposition.contains(".xlsx"));

        // Verify workbook structure via Apache POI
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(content))) {
            assertEquals(5, wb.getNumberOfSheets());
            assertNotNull(wb.getSheet("Research Summary"));
            assertNotNull(wb.getSheet("Student Analytics"));
            assertNotNull(wb.getSheet("Subject Analytics"));
            assertNotNull(wb.getSheet("Assessment History"));
            assertNotNull(wb.getSheet("Quiz Analytics"));
        }
    }

    @Test
    public void testExportPdf_Admin_ReturnsOkAndValidPdfDocument() throws Exception {
        String s1 = createStudentWithProfile("pdf_export1@edupilot.com", "CSE", 3);
        seedAuthenticBaseline(s1, "CS101", 65.0);

        MvcResult result = mockMvc.perform(get("/api/admin/analytics/export/pdf")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertNotNull(content);
        assertTrue(content.length > 0);

        String contentType = result.getResponse().getContentType();
        assertNotNull(contentType);
        assertTrue(contentType.contains("application/pdf"));

        String disposition = result.getResponse().getHeader("Content-Disposition");
        assertNotNull(disposition);
        assertTrue(disposition.contains("attachment"));
        assertTrue(disposition.contains(".pdf"));

        // Verify PDF magic header bytes "%PDF"
        String pdfHeader = new String(content, 0, Math.min(content.length, 5));
        assertTrue(pdfHeader.startsWith("%PDF"), "PDF file must start with %PDF magic header");
    }

    @Test
    public void testExport_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/export/excel"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/analytics/export/pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testExport_Student_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/export/excel")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/analytics/export/pdf")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testExport_Faculty_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/export/excel")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/analytics/export/pdf")
                        .header("Authorization", "Bearer " + facultyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testExport_InvalidFilter_ReturnsBadRequest() throws Exception {
        // startDate > endDate on excel export
        mockMvc.perform(get("/api/admin/analytics/export/excel")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest());

        // startDate > endDate on pdf export
        mockMvc.perform(get("/api/admin/analytics/export/pdf")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("startDate", "2026-03-01")
                        .param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest());

        // invalid activity status on excel export
        mockMvc.perform(get("/api/admin/analytics/export/excel")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("activityStatus", "NOT_A_VALID_STATUS"))
                .andExpect(status().isBadRequest());

        // invalid activity status on pdf export
        mockMvc.perform(get("/api/admin/analytics/export/pdf")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("activityStatus", "NOT_A_VALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testExportExcel_FilteredPopulation_RespectsBranchAndDate() throws Exception {
        String sCse = createStudentWithProfile("cse_export@edupilot.com", "CSE", 3);
        seedAuthenticBaseline(sCse, "CS101", 70.0);

        String sIt = createStudentWithProfile("it_export@edupilot.com", "IT", 3);
        seedAuthenticBaseline(sIt, "IT101", 60.0);

        MvcResult result = mockMvc.perform(get("/api/admin/analytics/export/excel")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("branch", "CSE"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(content))) {
            org.apache.poi.ss.usermodel.Sheet studentSheet = wb.getSheet("Student Analytics");
            assertNotNull(studentSheet);

            boolean foundCse = false;
            boolean foundIt = false;
            for (int r = 1; r <= studentSheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = studentSheet.getRow(r);
                if (row != null) {
                    String email = row.getCell(2) != null ? row.getCell(2).getStringCellValue() : "";
                    if ("cse_export@edupilot.com".equals(email)) foundCse = true;
                    if ("it_export@edupilot.com".equals(email)) foundIt = true;
                }
            }
            assertTrue(foundCse, "Filtered CSE student must be present in export");
            assertFalse(foundIt, "IT student must not be included when filtered by branch=CSE");
        }
    }

    @Test
    public void testExportExcel_ResearchIntegrity_MissingBaselineStaysMissing() throws Exception {
        String sNoBaseline = createStudentWithProfile("no_k0@edupilot.com", "MECH", 2);
        // Student only has current mastery but NO diagnostic
        seedConceptMastery(sNoBaseline, "MECH101", "Thermodynamics", 85.0);

        MvcResult result = mockMvc.perform(get("/api/admin/analytics/export/excel")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("branch", "MECH"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(content))) {
            org.apache.poi.ss.usermodel.Sheet studentSheet = wb.getSheet("Student Analytics");
            assertNotNull(studentSheet);

            boolean verified = false;
            for (int r = 1; r <= studentSheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = studentSheet.getRow(r);
                if (row != null && "no_k0@edupilot.com".equals(row.getCell(2).getStringCellValue())) {
                    String k0Val = row.getCell(5).getStringCellValue();
                    String gainVal = row.getCell(8).getStringCellValue();
                    assertEquals("N/A", k0Val, "Missing K0 must remain N/A instead of being fabricated as 50%");
                    assertEquals("N/A", gainVal, "Missing gain must remain N/A instead of being fabricated as 0");
                    verified = true;
                }
            }
            assertTrue(verified);
        }
    }

    @Test
    public void testExportExcel_ValidZeroValuesPreserved() throws Exception {
        String sZero = createStudentWithProfile("zero_k0@edupilot.com", "CSE", 1);
        // Student has authentic diagnostic with 0.0% score
        seedAuthenticBaseline(sZero, "CS100", 0.0);
        seedConceptMastery(sZero, "CS100", "Intro", 0.0);

        MvcResult result = mockMvc.perform(get("/api/admin/analytics/export/excel")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("branch", "CSE"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(content))) {
            org.apache.poi.ss.usermodel.Sheet studentSheet = wb.getSheet("Student Analytics");
            assertNotNull(studentSheet);

            boolean verified = false;
            for (int r = 1; r <= studentSheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = studentSheet.getRow(r);
                if (row != null && "zero_k0@edupilot.com".equals(row.getCell(2).getStringCellValue())) {
                    // Cell 5 is K0 (numeric 0.0)
                    assertEquals(0.0, row.getCell(5).getNumericCellValue(), 0.001, "Observed 0.0 K0 must remain numeric 0.0, not N/A");
                    // Cell 6 is Kt (numeric 0.0)
                    assertEquals(0.0, row.getCell(6).getNumericCellValue(), 0.001, "Observed 0.0 Kt must remain numeric 0.0, not N/A");
                    // Cell 7 is Growth (numeric 0.0)
                    assertEquals(0.0, row.getCell(7).getNumericCellValue(), 0.001, "Observed 0.0 Growth must remain numeric 0.0, not N/A");
                    // Cell 8 is Gain (numeric 0.0)
                    assertEquals(0.0, row.getCell(8).getNumericCellValue(), 0.001, "Observed 0.0 Gain must remain numeric 0.0, not N/A");
                    verified = true;
                }
            }
            assertTrue(verified);
        }
    }

    @Test
    public void testExport_ExactSubjectCodeMatching_NoSubstringOverlap() throws Exception {
        String sDbms = createStudentWithProfile("dbms_std@edupilot.com", "IT", 2);
        seedAuthenticBaseline(sDbms, "DBMS", 75.0);

        String sRdbms = createStudentWithProfile("rdbms_std@edupilot.com", "IT", 2);
        seedAuthenticBaseline(sRdbms, "RDBMS", 85.0);

        // Export with subjectCode = DBMS
        MvcResult result = mockMvc.perform(get("/api/admin/analytics/export/excel")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("subjectCode", "DBMS"))
                .andExpect(status().isOk())
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(content))) {
            org.apache.poi.ss.usermodel.Sheet historySheet = wb.getSheet("Assessment History");
            assertNotNull(historySheet);

            boolean foundDbms = false;
            boolean foundRdbms = false;
            for (int r = 1; r <= historySheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = historySheet.getRow(r);
                if (row != null) {
                    String subCode = row.getCell(3) != null ? row.getCell(3).getStringCellValue() : "";
                    if ("DBMS".equalsIgnoreCase(subCode)) foundDbms = true;
                    if ("RDBMS".equalsIgnoreCase(subCode)) foundRdbms = true;
                }
            }
            assertTrue(foundDbms, "Exact subjectCode DBMS must be matched");
            assertFalse(foundRdbms, "Substring matching must NOT include RDBMS when filtered by DBMS");
        }
    }

    @Test
    public void testExportPdf_NoGarbledCharactersOrMojibake() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/analytics/export/pdf")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        String pdfString = new String(content, java.nio.charset.StandardCharsets.ISO_8859_1);

        assertFalse(pdfString.contains("â€”"), "PDF content must not contain UTF-8 mojibake em-dash");
        assertFalse(pdfString.contains("â€¢"), "PDF content must not contain UTF-8 mojibake bullet");
    }

    @Test
    public void testExportExcel_MeanDifferenceLabeling() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/admin/analytics/export/excel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(content))) {
            org.apache.poi.ss.usermodel.Sheet summarySheet = wb.getSheet("Research Summary");
            assertNotNull(summarySheet);

            boolean foundMeanDiffLabel = false;
            for (int r = 0; r <= summarySheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = summarySheet.getRow(r);
                if (row != null && row.getCell(0) != null) {
                    String label = row.getCell(0).getStringCellValue();
                    if ("Mean Knowledge Difference (Kt - K0)".equals(label)) {
                        foundMeanDiffLabel = true;
                        String note = row.getCell(3).getStringCellValue();
                        assertTrue(note.contains("Difference between cohort mean current knowledge and cohort mean baseline knowledge"),
                                "Note must clearly state difference between cohort means and potential sample size differences");
                    }
                }
            }
            assertTrue(foundMeanDiffLabel, "Summary sheet must have metric labeled 'Mean Knowledge Difference (Kt - K0)'");
        }
    }

    // =========================================================================
    // ENVIRONMENT-INDEPENDENT GENUINE STUDENT CLASSIFICATION SCENARIO TESTS
    // =========================================================================

    @Test
    public void testScenarioA_freshDatabaseReturnsZeroStudents() {
        userRepository.deleteAll();
        studentProfileRepository.deleteAll();
        assessmentResultRepository.deleteAll();
        quizSessionRepository.deleteAll();

        AdminAnalyticsService.FilteredStudentContext ctx = adminAnalyticsService.resolveFilteredPopulation(null);
        assertEquals(0, ctx.validStudentIds.size(), "Scenario A: Fresh database with 0 users must produce 0 Admin genuine students");
    }

    @Test
    public void testScenarioB_newRealUserRegistrationGetsGenuineStudent() throws Exception {
        String regEmail = "new_real_student_" + System.currentTimeMillis() + "@gmail.com";
        Map<String, Object> req = Map.of(
            "email", regEmail,
            "password", "Password123!",
            "fullName", "New Real Student",
            "role", "STUDENT"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        User created = userRepository.findByEmail(regEmail).orElseThrow();
        assertEquals(User.AccountType.GENUINE_STUDENT, created.getAccountType(),
                "Scenario B: New registration via /api/auth/register MUST default to GENUINE_STUDENT");
    }

    @Test
    public void testScenarioC_newAutomatedTestUserGetsTestAutomation() {
        User testBot = new User();
        testBot.setEmail("auto_bot_" + System.currentTimeMillis() + "@edupilot.com");
        testBot.setFullName("Automation Bot");
        testBot.setRole(User.Role.STUDENT);
        testBot.setAccountType(User.AccountType.TEST_AUTOMATION);
        testBot = userRepository.save(testBot);

        AdminAnalyticsService.FilteredStudentContext ctx = adminAnalyticsService.resolveFilteredPopulation(null);
        assertFalse(ctx.validStudentIds.contains(testBot.getId()),
                "Scenario C: TEST_AUTOMATION accounts MUST be excluded from Admin Analytics");
    }

    @Test
    public void testScenarioD_legacyUserWithGenuineActivityClassifiedAsGenuineStudent() {
        long ts = System.currentTimeMillis();
        String legacyEmail = "genuine_legacy_" + ts + "@gmail.com";
        User legacyUser = new User();
        legacyUser.setEmail(legacyEmail);
        legacyUser.setFullName("Genuine Legacy Student");
        legacyUser.setRole(User.Role.STUDENT);
        legacyUser.setAccountType(null); // Unclassified
        legacyUser = userRepository.save(legacyUser);

        // Add real academic activity
        AssessmentResult ar = new AssessmentResult();
        ar.setUserId(legacyUser.getId());
        ar.setSubjectName("Data Structures & Algorithms");
        ar.setTotalQuestions(10);
        ar.setPercentage(85.0);
        ar.setCreatedAt(LocalDateTime.now());
        assessmentResultRepository.save(ar);

        int updated = legacyClassificationService.classifyLegacyStudentAccounts();
        assertTrue(updated >= 1, "Legacy classifier must update legacy candidate");

        User updatedUser = userRepository.findById(legacyUser.getId()).orElseThrow();
        assertEquals(User.AccountType.GENUINE_STUDENT, updatedUser.getAccountType(),
                "Scenario D: Legacy user with academic activity and no automation provenance MUST be classified as GENUINE_STUDENT");
    }

    @Test
    public void testScenarioE_legacyUserWithNoActivityRemainsNullUnclassified() {
        long ts = System.currentTimeMillis();
        String inactiveEmail = "inactive_legacy_" + ts + "@gmail.com";
        User inactiveUser = new User();
        inactiveUser.setEmail(inactiveEmail);
        inactiveUser.setFullName("Inactive Legacy Student");
        inactiveUser.setRole(User.Role.STUDENT);
        inactiveUser.setAccountType(null); // Unclassified
        inactiveUser = userRepository.save(inactiveUser);

        legacyClassificationService.classifyLegacyStudentAccounts();

        User reloaded = userRepository.findById(inactiveUser.getId()).orElseThrow();
        assertNull(reloaded.getAccountType(),
                "Scenario E: Legacy user with 0 academic activity MUST remain null / unclassified");
    }

    @Test
    public void testScenarioF_legacyUserWithAutomationProvenanceClassifiedAsTestAutomation() {
        long ts = System.currentTimeMillis();
        String testRunnerEmail = "growth_test_runner_" + ts + "@edupilot.com";
        User testUser = new User();
        testUser.setEmail(testRunnerEmail);
        testUser.setFullName("Growth Test Runner");
        testUser.setRole(User.Role.STUDENT);
        testUser.setAccountType(null); // Unclassified
        testUser = userRepository.save(testUser);

        legacyClassificationService.classifyLegacyStudentAccounts();

        User reloaded = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals(User.AccountType.TEST_AUTOMATION, reloaded.getAccountType(),
                "Scenario F: Legacy user with test automation provenance MUST be classified as TEST_AUTOMATION");
    }

    @Test
    public void testScenarioG_explicitAccountTypesPreservedUnchanged() {
        long ts = System.currentTimeMillis();
        User gUser = new User(null, "g_" + ts + "@edupilot.com", "pass", "G", User.Role.STUDENT, User.AccountType.GENUINE_STUDENT, LocalDateTime.now());
        User tUser = new User(null, "t_" + ts + "@edupilot.com", "pass", "T", User.Role.STUDENT, User.AccountType.TEST_AUTOMATION, LocalDateTime.now());
        User sUser = new User(null, "s_" + ts + "@edupilot.com", "pass", "S", User.Role.STUDENT, User.AccountType.SYNTHETIC_RESEARCH_SEED, LocalDateTime.now());

        gUser = userRepository.save(gUser);
        tUser = userRepository.save(tUser);
        sUser = userRepository.save(sUser);

        legacyClassificationService.classifyLegacyStudentAccounts();

        assertEquals(User.AccountType.GENUINE_STUDENT, userRepository.findById(gUser.getId()).orElseThrow().getAccountType());
        assertEquals(User.AccountType.TEST_AUTOMATION, userRepository.findById(tUser.getId()).orElseThrow().getAccountType());
        assertEquals(User.AccountType.SYNTHETIC_RESEARCH_SEED, userRepository.findById(sUser.getId()).orElseThrow().getAccountType());
    }

    @Test
    public void testScenarioH_migrationExecutionIsIdempotent() {
        long ts = System.currentTimeMillis();
        User candidate = new User(null, "candidate_" + ts + "@gmail.com", "pass", "Candidate", User.Role.STUDENT, null, LocalDateTime.now());
        candidate = userRepository.save(candidate);

        AssessmentResult ar = new AssessmentResult();
        ar.setUserId(candidate.getId());
        ar.setSubjectName("Database Management Systems");
        ar.setCreatedAt(LocalDateTime.now());
        assessmentResultRepository.save(ar);

        int pass1 = legacyClassificationService.classifyLegacyStudentAccounts();
        assertTrue(pass1 >= 1, "First migration pass must classify user");

        int pass2 = legacyClassificationService.classifyLegacyStudentAccounts();
        assertEquals(0, pass2, "Scenario H: Second migration pass MUST be idempotent and return 0 updates");
    }
}
