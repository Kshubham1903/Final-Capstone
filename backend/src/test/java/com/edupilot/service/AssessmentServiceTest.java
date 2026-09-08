package com.edupilot.service;

import com.edupilot.dto.AdaptiveAssessmentDTOs;
import com.edupilot.model.AdaptiveSession;
import com.edupilot.model.AssessmentSession;
import com.edupilot.model.QuizQuestion;
import com.edupilot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssessmentServiceTest {

    @Mock
    private AssessmentSessionRepository sessionRepository;

    @Mock
    private AdaptiveSessionRepository adaptiveSessionRepository;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private ConceptMasteryRepository conceptRepository;

    @Mock
    private KnowledgeService knowledgeService;

    @Mock
    private AssessmentResultRepository resultRepository;

    @InjectMocks
    private AssessmentService assessmentService;

    private QuizQuestion arrayQuestion;

    @BeforeEach
    public void setUp() {
        arrayQuestion = new QuizQuestion(
                "q_array_1",
                "Data Structures & Algorithms",
                "Arrays & Linked Lists",
                QuizQuestion.Difficulty.EASY,
                "Which of the following operations is more efficient for a singly linked list compared to a standard array?",
                List.of(
                        "Accessing an element by index",
                        "Inserting a new element at the head of the structure",
                        "Binary Search",
                        "Cache locality optimization"
                ),
                1,
                "Inserting at the head of a singly linked list takes O(1) time without element shifting."
        );
    }

    @Test
    public void testSubmitInitialAnswerReturnsExactQuestionExplanation() {
        AssessmentSession session = new AssessmentSession();
        session.setId("session_100");
        session.setUserId("user_1");
        session.setStudentProfileId("user_1");
        session.setSubjectCode("CS301");
        session.setSubjectName("Data Structures & Algorithms");
        session.setStatus(AssessmentSession.Status.IN_PROGRESS);
        session.setCurrentQuestionId("q_array_1");
        session.setActiveQuestionSubmitted(false);
        session.setQuestionCount(0);
        session.setTotalQuestions(10);

        when(sessionRepository.findById("session_100")).thenReturn(Optional.of(session));
        when(quizQuestionRepository.findById("q_array_1")).thenReturn(Optional.of(arrayQuestion));

        AdaptiveAssessmentDTOs.AdaptiveSubmitRequest req = new AdaptiveAssessmentDTOs.AdaptiveSubmitRequest("session_100", "q_array_1", 1, 5.0);

        AdaptiveAssessmentDTOs.AdaptiveSubmitResponse resp = assessmentService.submitInitialAnswer(req, "user_1");

        assertNotNull(resp);
        assertTrue(resp.isIsCorrect());
        assertEquals(1, resp.getCorrectOptionIndex());
        assertEquals("Inserting at the head of a singly linked list takes O(1) time without element shifting.", resp.getExplanation());
        assertFalse(resp.getExplanation().contains("BST"));
    }

    @Test
    public void testSubmitAdaptiveAnswerReturnsExactQuestionExplanation() {
        AdaptiveSession session = new AdaptiveSession();
        session.setId("adaptive_session_100");
        session.setUserId("user_1");
        session.setStudentProfileId("user_1");
        session.setSubjectCode("CS301");
        session.setSubjectName("Data Structures & Algorithms");
        session.setStatus(AdaptiveSession.Status.IN_PROGRESS);
        session.setCurrentQuestionId("q_array_1");
        session.setCurrentDifficulty(QuizQuestion.Difficulty.EASY);
        session.setActiveQuestionSubmitted(false);
        session.setQuestionCount(0);
        session.setMaxQuestions(10);

        when(adaptiveSessionRepository.findById("adaptive_session_100")).thenReturn(Optional.of(session));
        when(quizQuestionRepository.findById("q_array_1")).thenReturn(Optional.of(arrayQuestion));

        AdaptiveAssessmentDTOs.AdaptiveSubmitRequest req = new AdaptiveAssessmentDTOs.AdaptiveSubmitRequest("adaptive_session_100", "q_array_1", 1, 5.0);

        AdaptiveAssessmentDTOs.AdaptiveSubmitResponse resp = assessmentService.submitAdaptiveAnswer(req, "user_1");

        assertNotNull(resp);
        assertTrue(resp.isIsCorrect());
        assertEquals(1, resp.getCorrectOptionIndex());
        assertEquals("Inserting at the head of a singly linked list takes O(1) time without element shifting.", resp.getExplanation());
        assertFalse(resp.getExplanation().contains("BST"));
    }

    @Test
    public void testSubmitInitialAnswerAdvancesQuestionId() {
        AssessmentSession session = new AssessmentSession();
        session.setId("session_100");
        session.setUserId("user_1");
        session.setStudentProfileId("user_1");
        session.setSubjectCode("CS301");
        session.setSubjectName("Data Structures & Algorithms");
        session.setStatus(AssessmentSession.Status.IN_PROGRESS);
        session.setCurrentQuestionId("q_array_1");
        session.setActiveQuestionSubmitted(false);
        session.setQuestionCount(0);
        session.setTotalQuestions(10);
        session.setQuestionIds(List.of("q_array_1", "q_array_2"));

        QuizQuestion q2 = new QuizQuestion(
                "q_array_2",
                "Data Structures & Algorithms",
                "Stacks & Queues",
                QuizQuestion.Difficulty.MEDIUM,
                "Question 2 Text",
                List.of("A", "B", "C", "D"),
                0,
                "Explanation 2"
        );

        when(sessionRepository.findById("session_100")).thenReturn(Optional.of(session));
        when(quizQuestionRepository.findById("q_array_1")).thenReturn(Optional.of(arrayQuestion));
        when(quizQuestionRepository.findById("q_array_2")).thenReturn(Optional.of(q2));

        AdaptiveAssessmentDTOs.AdaptiveSubmitRequest req = new AdaptiveAssessmentDTOs.AdaptiveSubmitRequest("session_100", "q_array_1", 1, 5.0);

        AdaptiveAssessmentDTOs.AdaptiveSubmitResponse resp = assessmentService.submitInitialAnswer(req, "user_1");

        assertNotNull(resp);
        assertTrue(resp.isIsCorrect());
        assertEquals(1, resp.getCorrectOptionIndex());
        assertEquals(1, session.getQuestionCount(), "Session questionCount should advance to 1 (indicating Q2 next)");

        AdaptiveAssessmentDTOs.AdaptiveNextResponse nextResp = assessmentService.getInitialNextQuestion(
                new AdaptiveAssessmentDTOs.AdaptiveNextRequest("session_100"), "user_1"
        );

        assertNotNull(nextResp);
        assertNotNull(nextResp.getQuestion());
        assertEquals("q_array_2", nextResp.getQuestion().getQuestionId(), "Next question ID should advance to q_array_2");
        assertNotEquals("q_array_1", nextResp.getQuestion().getQuestionId(), "Next question ID must NOT be identical to Q1");
        assertEquals("q_array_2", session.getCurrentQuestionId(), "Session currentQuestionId should be updated to q_array_2");
    }
}
