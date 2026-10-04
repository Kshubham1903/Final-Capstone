package com.edupilot.service;

import com.edupilot.dto.AdminAnalyticsOverviewDTO;
import com.edupilot.model.AssessmentResult;
import com.edupilot.model.ConceptMastery;
import com.edupilot.model.QuizSession;
import com.edupilot.model.StudentSatisfaction;
import com.edupilot.model.User;
import com.edupilot.repository.AssessmentResultRepository;
import com.edupilot.repository.ConceptMasteryRepository;
import com.edupilot.repository.QuizSessionRepository;
import com.edupilot.repository.StudentProfileRepository;
import com.edupilot.repository.StudentSatisfactionRepository;
import com.edupilot.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private StudentService studentService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

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
}
