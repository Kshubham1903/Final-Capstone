package com.edupilot.service;

import com.edupilot.controller.AssessmentController;
import com.edupilot.dto.AssessmentSessionResponse;
import com.edupilot.dto.AssessmentStartRequest;
import com.edupilot.exception.GroqKeyRequiredException;
import com.edupilot.model.AssessmentSession;
import com.edupilot.model.QuizQuestion;
import com.edupilot.model.StudentProfile;
import com.edupilot.repository.*;
import com.edupilot.service.llm.GroqProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssessmentGroqKeyHandlingTest {

    @Mock
    private AssessmentSessionRepository sessionRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private StudentProfileRepository profileRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private QuizGenerationService quizGenerationService;

    @Mock
    private GroqProvider groqProvider;

    @InjectMocks
    private AssessmentService assessmentService;

    @InjectMocks
    private AssessmentController assessmentController;

    private StudentProfile testProfile;

    @BeforeEach
    public void setUp() {
        testProfile = new StudentProfile();
        testProfile.setUserId("student_test_1");
        testProfile.setBranch("Computer Science & Engineering");
        testProfile.setSemester(3);

        ReflectionTestUtils.setField(assessmentController, "assessmentService", assessmentService);
        ReflectionTestUtils.setField(assessmentController, "studentService", studentService);
    }

    @Test
    public void testAssessmentStartThrowsGroqKeyRequiredExceptionWhenKeyMissing() {
        when(studentService.resolveUserId("student_test_1")).thenReturn("student_test_1");
        when(studentService.findOrCreateProfile("student_test_1")).thenReturn(testProfile);
        when(quizGenerationService.generateSingleDiagnosticQuestion(anyString(), any(), anyMap(), eq(1), anyInt()))
                .thenThrow(new GroqKeyRequiredException("Personal Groq API Key is required to generate this assessment."));

        AssessmentStartRequest request = new AssessmentStartRequest();
        request.setUserId("student_test_1");
        request.setBranch("Computer Science & Engineering");
        request.setSemester(3);
        request.setSubjectCode("CS301");
        request.setSubjectName("Data Structures & Algorithms");

        assertThrows(GroqKeyRequiredException.class, () -> {
            assessmentService.startAssessmentSession(request);
        });
    }

    @Test
    public void testAssessmentControllerReturns400BadRequestWithGroqKeyRequired() {
        when(studentService.resolveUserId("student_test_1")).thenReturn("student_test_1");
        when(studentService.findOrCreateProfile("student_test_1")).thenReturn(testProfile);
        when(quizGenerationService.generateSingleDiagnosticQuestion(anyString(), any(), anyMap(), eq(1), anyInt()))
                .thenThrow(new GroqKeyRequiredException("Personal Groq API Key is required to generate this assessment."));

        AssessmentStartRequest request = new AssessmentStartRequest();
        request.setUserId("student_test_1");
        request.setBranch("Computer Science & Engineering");
        request.setSemester(3);
        request.setSubjectCode("CS301");
        request.setSubjectName("Data Structures & Algorithms");

        ResponseEntity<?> response = assessmentController.startAssessmentSession(request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("GROQ_KEY_REQUIRED", body.get("error"));
        assertEquals("Personal Groq API Key is required to generate this assessment.", body.get("message"));
    }

    @Test
    public void testAssessmentStartSucceedsWhenValidGroqGenerationReturnsQuestion() {
        when(studentService.resolveUserId("student_test_1")).thenReturn("student_test_1");
        when(studentService.findOrCreateProfile("student_test_1")).thenReturn(testProfile);

        QuizQuestion q1 = new QuizQuestion(
                "q_diag_1",
                "Data Structures & Algorithms",
                "Arrays & Linked Lists",
                QuizQuestion.Difficulty.EASY,
                "What is the head insertion time complexity in a singly linked list?",
                List.of("O(1)", "O(N)", "O(log N)", "O(N^2)"),
                0,
                "Head insertion takes O(1) time."
        );

        when(quizGenerationService.generateSingleDiagnosticQuestion(anyString(), any(), anyMap(), eq(1), anyInt()))
                .thenReturn(q1);
        when(quizQuestionRepository.save(any(QuizQuestion.class))).thenReturn(q1);

        AssessmentSession savedSession = new AssessmentSession();
        savedSession.setId("sess_diag_100");
        savedSession.setUserId("student_test_1");
        savedSession.setBranch("Computer Science & Engineering");
        savedSession.setSemester(3);
        savedSession.setSubjectCode("CS301");
        savedSession.setSubjectName("Data Structures & Algorithms");
        savedSession.setTotalQuestions(25);
        when(sessionRepository.save(any(AssessmentSession.class))).thenReturn(savedSession);

        AssessmentStartRequest request = new AssessmentStartRequest();
        request.setUserId("student_test_1");
        request.setBranch("Computer Science & Engineering");
        request.setSemester(3);
        request.setSubjectCode("CS301");
        request.setSubjectName("Data Structures & Algorithms");
        request.setQuestionCount(25);

        ResponseEntity<?> response = assessmentController.startAssessmentSession(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof AssessmentSessionResponse);

        AssessmentSessionResponse sessionResp = (AssessmentSessionResponse) response.getBody();
        assertEquals("sess_diag_100", sessionResp.getSessionId());
        assertEquals(25, sessionResp.getTotalQuestions());
        assertNotNull(sessionResp.getQuestions());
        assertEquals(1, sessionResp.getQuestions().size());
        assertEquals("q_diag_1", sessionResp.getQuestions().get(0).getQuestionId());
    }

    @Test
    public void testUnexpectedInternalExceptionIsNotConvertedToGroqKeyRequired() {
        when(studentService.resolveUserId("student_test_1")).thenReturn("student_test_1");
        when(studentService.findOrCreateProfile("student_test_1")).thenReturn(testProfile);
        when(quizGenerationService.generateSingleDiagnosticQuestion(anyString(), any(), anyMap(), eq(1), anyInt()))
                .thenThrow(new NullPointerException("Simulated unexpected internal database failure"));

        AssessmentStartRequest request = new AssessmentStartRequest();
        request.setUserId("student_test_1");
        request.setBranch("Computer Science & Engineering");
        request.setSemester(3);
        request.setSubjectCode("CS301");
        request.setSubjectName("Data Structures & Algorithms");

        ResponseEntity<?> response = assessmentController.startAssessmentSession(request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);

        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("ASSESSMENT_START_FAILED", body.get("error"));
        assertNotEquals("GROQ_KEY_REQUIRED", body.get("error"));
    }

    @Test
    public void testQuizGenerationServiceThrowsGroqKeyRequiredExceptionOnUnauthenticated() throws Exception {
        QuizGenerationService qgService = new QuizGenerationService();
        ReflectionTestUtils.setField(qgService, "groqProvider", groqProvider);

        when(groqProvider.generateResponse(anyString(), anyString(), anyMap()))
                .thenReturn("{\"error\":\"UNAUTHENTICATED\",\"message\":\"Personal Groq API key is required. Please configure your key in your profile before using AI features.\",\"details\":\"Please add your personal Groq API key in your Profile.\"}");

        QuizGenerationService.QuestionBlueprintSpec spec =
                new QuizGenerationService.QuestionBlueprintSpec(1, "Linked Lists", QuizQuestion.Difficulty.EASY);

        Method genMethod = QuizGenerationService.class.getDeclaredMethod(
                "generateSingleQuestionWithRetry", String.class, QuizGenerationService.QuestionBlueprintSpec.class, Map.class, int.class, int.class
        );
        genMethod.setAccessible(true);

        try {
            genMethod.invoke(qgService, "Data Structures & Algorithms", spec, Map.of(), 1, 25);
            fail("Expected InvocationTargetException wrapping GroqKeyRequiredException");
        } catch (java.lang.reflect.InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof GroqKeyRequiredException,
                    "Cause should be GroqKeyRequiredException but was: " + ite.getCause());
            assertEquals("Personal Groq API Key is required to generate this assessment.", ite.getCause().getMessage());
        }
    }
}
