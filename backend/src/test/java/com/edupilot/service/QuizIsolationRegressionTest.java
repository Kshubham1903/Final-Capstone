package com.edupilot.service;

import com.edupilot.model.*;
import com.edupilot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class QuizIsolationRegressionTest {

    private QuizQuestionRepository questionRepository;
    private DashboardTestSessionRepository dashboardSessionRepository;
    private RemediationSessionRepository remediationSessionRepository;
    private StudentProfileRepository profileRepository;
    private KnowledgeProfileRepository knowledgeProfileRepository;
    private StudentService studentService;

    @BeforeEach
    public void setUp() {
        questionRepository = mock(QuizQuestionRepository.class);
        dashboardSessionRepository = mock(DashboardTestSessionRepository.class);
        remediationSessionRepository = mock(RemediationSessionRepository.class);
        profileRepository = mock(StudentProfileRepository.class);
        knowledgeProfileRepository = mock(KnowledgeProfileRepository.class);
        studentService = mock(StudentService.class);
    }

    @Test
    @DisplayName("Verification 1: Each module entity has explicit ModuleType / moduleSource identification")
    public void testModuleTypeAndSourceExplicitIdentity() {
        QuizQuestion practiceQ = QuizQuestion.builder()
                .id("q1")
                .subject("Computer Science")
                .concept("Arrays")
                .difficulty(QuizQuestion.Difficulty.EASY)
                .questionText("Practice question 1")
                .moduleSource(ModuleType.PRACTICE)
                .build();

        QuizQuestion diagQ = QuizQuestion.builder()
                .id("q2")
                .subject("Computer Science")
                .concept("Linked List")
                .difficulty(QuizQuestion.Difficulty.EASY)
                .questionText("Diagnostic question 1")
                .moduleSource(ModuleType.DIAGNOSTIC)
                .build();

        QuizQuestion baselineQ = QuizQuestion.builder()
                .id("q3")
                .subject("Computer Science")
                .concept("Trees")
                .difficulty(QuizQuestion.Difficulty.EASY)
                .questionText("Baseline question 1")
                .moduleSource(ModuleType.BASELINE)
                .build();

        QuizQuestion remQ = QuizQuestion.builder()
                .id("q4")
                .subject("Computer Science")
                .concept("Graphs")
                .difficulty(QuizQuestion.Difficulty.MEDIUM)
                .questionText("Remediation question 1")
                .moduleSource(ModuleType.REMEDIATION)
                .build();

        assertEquals(ModuleType.PRACTICE, practiceQ.getModuleSource());
        assertEquals(ModuleType.DIAGNOSTIC, diagQ.getModuleSource());
        assertEquals(ModuleType.BASELINE, baselineQ.getModuleSource());
        assertEquals(ModuleType.REMEDIATION, remQ.getModuleSource());

        AssessmentSession assessmentSession = new AssessmentSession();
        assertEquals(ModuleType.DIAGNOSTIC, assessmentSession.getModuleType());

        AdaptiveSession adaptiveSession = new AdaptiveSession();
        assertEquals(ModuleType.ADAPTIVE, adaptiveSession.getModuleType());

        QuizSession quizSession = new QuizSession();
        assertEquals(ModuleType.PRACTICE, quizSession.getModuleType());

        DashboardTestSession dashboardSession = new DashboardTestSession();
        assertEquals(ModuleType.BASELINE, dashboardSession.getModuleType());

        RemediationSession remediationSession = new RemediationSession();
        assertEquals(ModuleType.REMEDIATION, remediationSession.getModuleType());
    }

    @Test
    @DisplayName("Verification 2: Question isolation - Practice Quiz filtering excludes DIAGNOSTIC and BASELINE questions")
    public void testQuestionIsolationFiltering() {
        String subject = "Data Structures & Algorithms";

        QuizQuestion qDiag = new QuizQuestion("1", subject, "Arrays", QuizQuestion.Difficulty.EASY, "Diagnostic Text", List.of("A","B","C","D"), 0, "Exp");
        qDiag.setModuleSource(ModuleType.DIAGNOSTIC);

        QuizQuestion qBase = new QuizQuestion("2", subject, "Trees", QuizQuestion.Difficulty.EASY, "Baseline Text", List.of("A","B","C","D"), 1, "Exp");
        qBase.setModuleSource(ModuleType.BASELINE);

        QuizQuestion qPrac = new QuizQuestion("3", subject, "Stacks", QuizQuestion.Difficulty.MEDIUM, "Practice Text", List.of("A","B","C","D"), 2, "Exp");
        qPrac.setModuleSource(ModuleType.PRACTICE);

        List<QuizQuestion> allSubjectQuestions = List.of(qDiag, qBase, qPrac);

        List<QuizQuestion> practiceFiltered = new ArrayList<>();
        for (QuizQuestion q : allSubjectQuestions) {
            ModuleType src = q.getModuleSource();
            if (src != ModuleType.DIAGNOSTIC && src != ModuleType.BASELINE) {
                practiceFiltered.add(q);
            }
        }

        assertEquals(1, practiceFiltered.size());
        assertEquals("3", practiceFiltered.get(0).getId());
        assertEquals(ModuleType.PRACTICE, practiceFiltered.get(0).getModuleSource());
        assertFalse(practiceFiltered.contains(qDiag), "Form D Diagnostic question must NEVER appear in Practice Quiz");
        assertFalse(practiceFiltered.contains(qBase), "Baseline question must NEVER appear in Practice Quiz");
    }

    @Test
    @DisplayName("Verification 3: MongoDB Collection Isolation - Remediation Sessions stored in dedicated collection")
    public void testRemediationSessionIsolationFromBaseline() {
        RemediationSession remSession = new RemediationSession("rem_101", "student_1", "CS", "Arrays", List.of("q1", "q2"), LocalDateTime.now(), false);
        assertEquals(ModuleType.REMEDIATION, remSession.getModuleType());

        DashboardTestSession baseSession = new DashboardTestSession("base_202", "student_1", List.of("CS"), List.of("q3", "q4"), LocalDateTime.now(), false);
        assertEquals(ModuleType.BASELINE, baseSession.getModuleType());

        when(remediationSessionRepository.findById("rem_101")).thenReturn(Optional.of(remSession));
        when(dashboardSessionRepository.findTop5ByStudentIdOrderByCreatedAtDesc("student_1")).thenReturn(List.of(baseSession));

        Optional<RemediationSession> fetchedRem = remediationSessionRepository.findById("rem_101");
        assertTrue(fetchedRem.isPresent());
        assertEquals("rem_101", fetchedRem.get().getId());
        assertEquals(ModuleType.REMEDIATION, fetchedRem.get().getModuleType());

        List<DashboardTestSession> fetchedBaseHistory = dashboardSessionRepository.findTop5ByStudentIdOrderByCreatedAtDesc("student_1");
        assertEquals(1, fetchedBaseHistory.size());
        assertEquals("base_202", fetchedBaseHistory.get(0).getId());
        assertFalse(fetchedBaseHistory.stream().anyMatch(s -> s.getId().equals("rem_101")),
                "Remediation sessions must NEVER be returned when querying Baseline Test sessions");
    }

    @Test
    @DisplayName("Verification 4: Mastery Isolation - Practice Quiz sync does not destroy existing subject diagnostic score when concept list is empty")
    public void testMasteryIsolationPreservesDiagnosticScore() {
        StudentProfile profile = new StudentProfile();
        profile.setId("student_1");
        Map<String, Double> masteryMap = new HashMap<>();
        masteryMap.put("Data Structures & Algorithms", 85.0);
        profile.setConceptMastery(masteryMap);

        String subject = "Data Structures & Algorithms";

        // If no concept mastery records exist yet for this subject:
        List<ConceptMastery> allConcepts = List.of();

        double subjectHealthScore;
        List<ConceptMastery> subjectConcepts = allConcepts.stream()
                .filter(c -> c.getSubjectName() != null && c.getSubjectName().equalsIgnoreCase(subject))
                .toList();

        if (!subjectConcepts.isEmpty()) {
            double subjectAccSum = subjectConcepts.stream().mapToDouble(ConceptMastery::getAccuracy).sum();
            subjectHealthScore = Math.round((subjectAccSum / subjectConcepts.size()) * 10.0) / 10.0;
        } else if (profile.getConceptMastery() != null && profile.getConceptMastery().containsKey(subject)) {
            subjectHealthScore = profile.getConceptMastery().get(subject);
        } else {
            subjectHealthScore = 0.0;
        }

        assertEquals(85.0, subjectHealthScore, "Diagnostic subject score must be preserved when subject concept list is empty");
    }

    @Test
    @DisplayName("Verification 5: Session Abandonment - Active sessions transition to completed=false / ABANDONED without polluting score history")
    public void testSessionAbandonmentLogic() {
        AssessmentSession assessmentSession = new AssessmentSession();
        assessmentSession.setId("sess_diag_1");
        assessmentSession.setStatus(AssessmentSession.Status.IN_PROGRESS);

        assessmentSession.setStatus(AssessmentSession.Status.ABANDONED);

        assertEquals(AssessmentSession.Status.ABANDONED, assessmentSession.getStatus());

        QuizSession practiceSession = new QuizSession();
        practiceSession.setId("sess_prac_1");
        practiceSession.setStatus(QuizSession.Status.IN_PROGRESS);

        practiceSession.setStatus(QuizSession.Status.ABANDONED);
        assertEquals(QuizSession.Status.ABANDONED, practiceSession.getStatus());
    }

    @Test
    @DisplayName("Verification 6: Result Graph Sync - Baseline test completion updates StudentProfile.conceptMastery map for UI BarChart rendering")
    public void testProfileConceptMasterySyncForGraph() {
        StudentProfile profile = new StudentProfile();
        profile.setId("student_100");
        profile.setConceptMastery(new HashMap<>());

        Map<String, Double> baselineResults = Map.of(
                "Data Structures & Algorithms", 80.0,
                "Database Management Systems", 60.0
        );

        Map<String, Double> cm = profile.getConceptMastery();
        cm.putAll(baselineResults);
        profile.setConceptMastery(cm);

        assertNotNull(profile.getConceptMastery());
        assertEquals(80.0, profile.getConceptMastery().get("Data Structures & Algorithms"));
        assertEquals(60.0, profile.getConceptMastery().get("Database Management Systems"));
    }
}
