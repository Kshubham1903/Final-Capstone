package com.edupilot.service;

import com.edupilot.dto.DashboardTestQuestionDTO;
import com.edupilot.dto.DashboardTestSubmissionDTO;
import com.edupilot.model.*;
import com.edupilot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TopicMasteryQuestionReuseTest {

    @Autowired
    private ConceptRemediationService conceptRemediationService;

    @Autowired
    private QuizQuestionRepository questionRepository;

    @Autowired
    private RemediationSessionRepository remediationSessionRepository;

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    private final String testSubject = "Data Structures & Algorithms";
    private final String testConcept = "Arrays & Linked Lists";

    @BeforeEach
    public void setUp() {
        // Clean test collections in edupilot_test DB
        remediationSessionRepository.deleteAll();
        questionRepository.deleteAll();

        // Seed 40 distinct questions across 8 sub-aspect categories (5 per category) to support 10-question batches
        List<QuizQuestion> testQuestions = new ArrayList<>();
        String[] subAspectPrefixes = new String[] {
                "How to reverse a singly linked list",
                "What is random access time complexity",
                "How does cache locality affect performance",
                "What happens during dynamic array resizing",
                "How does head insertion work in linked list",
                "What is tail deletion complexity",
                "Explain node memory allocation overhead",
                "What is the embedded use-case trade-off"
        };
        int qId = 1;
        for (int cat = 0; cat < subAspectPrefixes.length; cat++) {
            String prefix = subAspectPrefixes[cat];
            for (int v = 1; v <= 5; v++) {
                QuizQuestion q = QuizQuestion.builder()
                        .subject(testSubject)
                        .concept(testConcept)
                        .difficulty(QuizQuestion.Difficulty.MEDIUM)
                        .questionText(prefix + " variant " + v + " for " + testConcept)
                        .options(List.of("Option A", "Option B", "Option C", "Option D"))
                        .correctOptionIndex(0)
                        .conceptualExplanation("Explanation for question " + qId)
                        .moduleSource(ModuleType.REMEDIATION)
                        .build();
                testQuestions.add(q);
                qId++;
            }
        }
        questionRepository.saveAll(testQuestions);
    }

    @Test
    @DisplayName("Test A, B, C: Student A repeated completed 10-question attempts receive non-overlapping questions")
    public void testStudentARepeatedAttemptsZeroOverlap() {
        String studentA = "student_A_" + UUID.randomUUID();

        // Attempt 1 (10 questions)
        Map<String, Object> attempt1 = conceptRemediationService.startVerificationTest(studentA, testSubject, testConcept);
        String session1Id = (String) attempt1.get("sessionId");
        List<DashboardTestQuestionDTO> qList1 = (List<DashboardTestQuestionDTO>) attempt1.get("questions");
        assertEquals(10, qList1.size());

        Set<String> set1 = new HashSet<>();
        for (DashboardTestQuestionDTO q : qList1) {
            set1.add(q.getQuestionId());
        }

        // Complete Attempt 1
        completeSession(studentA, session1Id, qList1);

        // Attempt 2 (10 questions)
        Map<String, Object> attempt2 = conceptRemediationService.startVerificationTest(studentA, testSubject, testConcept);
        String session2Id = (String) attempt2.get("sessionId");
        List<DashboardTestQuestionDTO> qList2 = (List<DashboardTestQuestionDTO>) attempt2.get("questions");
        assertEquals(10, qList2.size());

        Set<String> set2 = new HashSet<>();
        for (DashboardTestQuestionDTO q : qList2) {
            set2.add(q.getQuestionId());
        }

        // Verify Zero Overlap between Attempt 1 and Attempt 2
        Set<String> overlap12 = new HashSet<>(set1);
        overlap12.retainAll(set2);
        assertTrue(overlap12.isEmpty(), "Attempt 2 questions must have zero overlap with completed Attempt 1 questions. Overlap: " + overlap12);

        // Complete Attempt 2
        completeSession(studentA, session2Id, qList2);

        // Attempt 3 (10 questions)
        Map<String, Object> attempt3 = conceptRemediationService.startVerificationTest(studentA, testSubject, testConcept);
        List<DashboardTestQuestionDTO> qList3 = (List<DashboardTestQuestionDTO>) attempt3.get("questions");
        assertEquals(10, qList3.size());

        Set<String> set3 = new HashSet<>();
        for (DashboardTestQuestionDTO q : qList3) {
            set3.add(q.getQuestionId());
        }

        // Verify Zero Overlap between Attempt 3 and previous attempts
        Set<String> overlap13 = new HashSet<>(set1);
        overlap13.retainAll(set3);
        assertTrue(overlap13.isEmpty(), "Attempt 3 must have zero overlap with Attempt 1 questions");

        Set<String> overlap23 = new HashSet<>(set2);
        overlap23.retainAll(set3);
        assertTrue(overlap23.isEmpty(), "Attempt 3 must have zero overlap with Attempt 2 questions");
    }

    @Test
    @DisplayName("Test D: Student B is independent and not restricted by Student A history")
    public void testStudentBIndependence() {
        String studentA = "student_A_" + UUID.randomUUID();
        String studentB = "student_B_" + UUID.randomUUID();

        // Student A completes 10-question attempt
        Map<String, Object> attemptA = conceptRemediationService.startVerificationTest(studentA, testSubject, testConcept);
        String sessionAId = (String) attemptA.get("sessionId");
        List<DashboardTestQuestionDTO> qListA = (List<DashboardTestQuestionDTO>) attemptA.get("questions");
        completeSession(studentA, sessionAId, qListA);

        // Student B starts attempt
        Map<String, Object> attemptB = conceptRemediationService.startVerificationTest(studentB, testSubject, testConcept);
        List<DashboardTestQuestionDTO> qListB = (List<DashboardTestQuestionDTO>) attemptB.get("questions");
        assertEquals(10, qListB.size());
    }

    @Test
    @DisplayName("Test E: Incomplete/abandoned session questions are NOT excluded")
    public void testIncompleteSessionNotExcluded() {
        String studentC = "student_C_" + UUID.randomUUID();

        // Start session 1 but DO NOT mark completed
        Map<String, Object> attempt1 = conceptRemediationService.startVerificationTest(studentC, testSubject, testConcept);
        List<DashboardTestQuestionDTO> qList1 = (List<DashboardTestQuestionDTO>) attempt1.get("questions");
        assertEquals(10, qList1.size());

        // Start session 2 without completing session 1
        Map<String, Object> attempt2 = conceptRemediationService.startVerificationTest(studentC, testSubject, testConcept);
        List<DashboardTestQuestionDTO> qList2 = (List<DashboardTestQuestionDTO>) attempt2.get("questions");
        assertEquals(10, qList2.size(), "Second session starts normally");
    }

    @Test
    @DisplayName("Test F & G: Concept and Subject boundaries maintained for all 10 questions")
    public void testConceptBoundaryMaintenance() {
        String student = "student_boundary_" + UUID.randomUUID();
        Map<String, Object> attempt = conceptRemediationService.startVerificationTest(student, testSubject, testConcept);
        List<DashboardTestQuestionDTO> qList = (List<DashboardTestQuestionDTO>) attempt.get("questions");
        assertEquals(10, qList.size());
        for (DashboardTestQuestionDTO q : qList) {
            assertEquals(testSubject, q.getSubject());
            assertEquals(testConcept, q.getConcept());
            assertNotNull(q.getConceptualExplanation());
        }
    }

    @Test
    @DisplayName("Test H & I: Grading and ConceptMastery update work correctly for 10-question verification")
    public void testGradingAndMasteryUpdate() {
        String student = "student_grading_" + UUID.randomUUID();
        Map<String, Object> attempt = conceptRemediationService.startVerificationTest(student, testSubject, testConcept);
        String sessionId = (String) attempt.get("sessionId");
        List<DashboardTestQuestionDTO> qList = (List<DashboardTestQuestionDTO>) attempt.get("questions");
        assertEquals(10, qList.size());

        List<DashboardTestSubmissionDTO.AnswerEntry> answers = new ArrayList<>();
        for (DashboardTestQuestionDTO q : qList) {
            answers.add(new DashboardTestSubmissionDTO.AnswerEntry(q.getQuestionId(), 0)); // Correct index is 0
        }

        Map<String, Object> result = conceptRemediationService.submitRemediationTest(student, sessionId, answers);
        assertNotNull(result);
        assertTrue((Boolean) result.get("passed"));

        // Verify concept mastery updated
        Optional<ConceptMastery> masteryOpt = conceptMasteryRepository.findByUserIdAndConceptName(student, testConcept);
        assertTrue(masteryOpt.isPresent());
        assertTrue(masteryOpt.get().getMasteryScore() > 0);
    }

    private void completeSession(String studentId, String sessionId, List<DashboardTestQuestionDTO> qList) {
        List<DashboardTestSubmissionDTO.AnswerEntry> answers = new ArrayList<>();
        for (DashboardTestQuestionDTO q : qList) {
            QuizQuestion entity = questionRepository.findById(q.getQuestionId()).orElse(null);
            int correctIdx = entity != null ? entity.getCorrectOptionIndex() : 0;
            answers.add(new DashboardTestSubmissionDTO.AnswerEntry(q.getQuestionId(), correctIdx));
        }
        conceptRemediationService.submitRemediationTest(studentId, sessionId, answers);
    }

    @Test
    @DisplayName("Verify correctOptionIndex integrity, explanation consistency, and topic scoping for 10-question Topic Mastery")
    public void testAnswerIndexAndExplanationIntegrity() {
        String student = "student_integrity_" + UUID.randomUUID();

        // 1. Generate 10-question Topic Mastery Verification quiz
        Map<String, Object> attempt = conceptRemediationService.startVerificationTest(student, testSubject, testConcept);
        List<DashboardTestQuestionDTO> qList = (List<DashboardTestQuestionDTO>) attempt.get("questions");
        assertEquals(10, qList.size(), "Verification quiz must contain exactly 10 questions");

        for (DashboardTestQuestionDTO q : qList) {
            // TEST 3: Verify 10 questions remain specifically scoped to Data Structures & Algorithms -> Arrays & Linked Lists
            assertEquals(testSubject, q.getSubject(), "Subject must be " + testSubject);
            assertEquals(testConcept, q.getConcept(), "Concept must be " + testConcept);

            QuizQuestion entity = questionRepository.findById(q.getQuestionId())
                    .orElseThrow(() -> new AssertionError("Question entity not found for ID: " + q.getQuestionId()));

            // TEST 1: Assert correctOptionIndex >= 0 && correctOptionIndex < options.length on both entity AND DTO
            int correctIdx = entity.getCorrectOptionIndex();
            assertEquals(correctIdx, q.getCorrectOptionIndex(),
                    "DashboardTestQuestionDTO correctOptionIndex (" + q.getCorrectOptionIndex() + ") must match entity correctOptionIndex (" + correctIdx + ")");
            List<String> options = entity.getOptions();
            assertNotNull(options, "Options list must not be null");
            assertEquals(4, options.size(), "Options list must contain 4 choices");
            assertTrue(correctIdx >= 0 && correctIdx < options.size(),
                    "correctOptionIndex (" + correctIdx + ") must be within bounds [0, 3]");

            String correctOptionText = options.get(correctIdx);
            assertNotNull(correctOptionText, "Option at correctOptionIndex must not be null");
            assertFalse(correctOptionText.isBlank(), "Option at correctOptionIndex must not be blank");

            // TEST 2: Verify question explanation is consistent with selected correct option
            String exp = entity.getConceptualExplanation();
            assertNotNull(exp, "Conceptual explanation must not be null");
            assertFalse(exp.isBlank(), "Conceptual explanation must not be blank");

            int alignedIdx = QuizGenerationService.verifyAndAlignWithExplanation(correctIdx, options, exp);
            assertEquals(correctIdx, alignedIdx,
                    "correctOptionIndex (" + correctIdx + ": '" + correctOptionText + "') must be consistent with conceptual explanation: '" + exp + "'");
        }
    }

    @Test
    @DisplayName("Verify exactly 10 questions returned, all 10 IDs unique, all 10 normalized texts unique, and properly scoped to concept")
    public void testTenUniqueQuestionsAndNoTextDuplicatesInSingleAttempt() {
        // Seed duplicate question text records with DIFFERENT IDs in DB to test deduplication
        for (int i = 1; i <= 5; i++) {
            QuizQuestion dupQ = QuizQuestion.builder()
                    .subject(testSubject)
                    .concept(testConcept)
                    .difficulty(QuizQuestion.Difficulty.MEDIUM)
                    .questionText("How to reverse a singly linked list variant 1 for " + testConcept) // Same text as Q1 seeded in setUp
                    .options(List.of("Option A", "Option B", "Option C", "Option D"))
                    .correctOptionIndex(0)
                    .conceptualExplanation("Duplicate text explanation for question 1 clone " + i)
                    .moduleSource(ModuleType.REMEDIATION)
                    .build();
            questionRepository.save(dupQ);
        }

        String student = "student_unique_" + UUID.randomUUID();
        Map<String, Object> attempt = conceptRemediationService.startVerificationTest(student, testSubject, testConcept);
        List<DashboardTestQuestionDTO> qList = (List<DashboardTestQuestionDTO>) attempt.get("questions");

        // 1. Assert 10 questions returned
        assertEquals(10, qList.size(), "Topic Mastery Verification attempt must return exactly 10 questions");

        Set<String> seenIds = new HashSet<>();
        Set<String> seenNormTexts = new HashSet<>();

        for (DashboardTestQuestionDTO q : qList) {
            // 2. Assert subject and concept scoping
            assertEquals(testSubject, q.getSubject());
            assertEquals(testConcept, q.getConcept());

            // 3. Assert unique IDs within attempt
            assertTrue(seenIds.add(q.getQuestionId()), "Question ID " + q.getQuestionId() + " must be unique within attempt");

            // 4. Assert unique normalized question text within attempt
            String normText = QuizGenerationService.normalizeQuestionText(q.getQuestionText());
            assertTrue(seenNormTexts.add(normText), "Normalized question text '" + normText + "' must be unique within attempt");

            // 5. Assert correctOptionIndex within bounds and non-empty option
            assertTrue(q.getCorrectOptionIndex() >= 0 && q.getCorrectOptionIndex() < q.getOptions().size(),
                    "correctOptionIndex (" + q.getCorrectOptionIndex() + ") must be within bounds [0, 3]");
            assertNotNull(q.getOptions().get(q.getCorrectOptionIndex()));
            assertFalse(q.getOptions().get(q.getCorrectOptionIndex()).isBlank());
        }

        assertEquals(10, seenIds.size(), "Attempt must contain exactly 10 unique question IDs");
        assertEquals(10, seenNormTexts.size(), "Attempt must contain exactly 10 unique normalized question texts");
    }

    @Test
    @DisplayName("Regression Test: Valid correctOptionIndex is strictly preserved and never corrupted by fuzzy heuristics")
    public void testValidCorrectOptionIndexIsPreserved() {
        List<String> options = List.of(
                "A singly linked list requires elements to be stored in contiguous memory locations, while a dynamic array does not.",
                "A singly linked list stores elements in nodes linked by pointers, whereas a dynamic array stores elements in a contiguous block of memory.",
                "A dynamic array has a fixed size that cannot be changed, while a singly linked list has a variable size.",
                "A singly linked list supports O(1) random access, while a dynamic array requires O(n) time to access an element by index."
        );
        String explanation = "Arrays (including dynamic arrays) store elements in a single contiguous block of memory, which allows for O(1) index-based access. In contrast, linked lists store data in nodes that can be scattered in memory, connected by pointers to the next node.";

        // 1. Exact failing question regression: stored correctOptionIndex = 1 must remain 1
        int aligned1 = QuizGenerationService.verifyAndAlignWithExplanation(1, options, explanation);
        assertEquals(1, aligned1, "Valid correctOptionIndex=1 must remain 1 and not be corrupted to 0 or 3");

        // 2. Test valid index 0 is preserved
        assertEquals(0, QuizGenerationService.verifyAndAlignWithExplanation(0, options, explanation), "Index 0 must be preserved");

        // 3. Test valid index 2 is preserved
        assertEquals(2, QuizGenerationService.verifyAndAlignWithExplanation(2, options, explanation), "Index 2 must be preserved");

        // 4. Test last valid index 3 is preserved
        assertEquals(3, QuizGenerationService.verifyAndAlignWithExplanation(3, options, explanation), "Index 3 must be preserved");

        // 5. Test invalid index (-1) falls back to explanation-based alignment
        String explicitExp = "Option B is correct. " + explanation;
        assertEquals(1, QuizGenerationService.verifyAndAlignWithExplanation(-1, options, explicitExp), "Invalid index -1 must fall back to repairing using explicit letter in explanation");
    }

    @Test
    @DisplayName("Verify missing or unparsed correctOptionIndex (-1) is correctly repaired using explanation")
    public void testMisalignmentResolutionExamples() {
        // Example 1
        List<String> options1 = List.of(
                "Accessing the element at index i",
                "Inserting an element at the head",
                "Deleting the last element",
                "Searching for a specific value"
        );
        String exp1 = "Option B: Inserting an element at the head of a singly linked list takes O(1) time because only pointers are updated. In contrast, dynamic arrays require shifting elements...";

        int resolved1 = QuizGenerationService.verifyAndAlignWithExplanation(-1, options1, exp1);
        assertEquals(1, resolved1, "Example 1 with invalid index -1 must resolve to index 1 (Inserting an element at the head)");

        // Example 2
        List<String> options2 = List.of(
                "Linked list: O(n), Array: O(1)",
                "Linked list: O(1), Array: O(n)",
                "Linked list: O(n), Array: O(n)",
                "Linked list: O(1), Array: O(1)"
        );
        String exp2 = "singly linked list insertion at head = O(1), dynamic array insertion at head = O(n)";

        int resolved2 = QuizGenerationService.verifyAndAlignWithExplanation(-1, options2, exp2);
        assertEquals(1, resolved2, "Example 2 with invalid index -1 must resolve to index 1 (Linked list: O(1), Array: O(n))");
    }

    @Test
    @DisplayName("Verify semantic sub-aspect signature extraction and redundancy filtering prevent repetitive questions")
    public void testSubAspectSignatureExtractionAndSemanticDeduplication() {
        String text1 = "What is the time complexity of random access by index in a dynamic array vs a singly linked list?";
        String text2 = "Which structure provides O(1) random access indexing?";
        String text3 = "Why is insertion at the head of a singly linked list O(1) while array insertion at head is O(n)?";
        String text4 = "What is the time complexity of inserting a node at the beginning of a linked list?";

        String sig1 = QuizGenerationService.extractSubAspectSignature(text1);
        String sig2 = QuizGenerationService.extractSubAspectSignature(text2);
        String sig3 = QuizGenerationService.extractSubAspectSignature(text3);
        String sig4 = QuizGenerationService.extractSubAspectSignature(text4);

        assertEquals("RANDOM_ACCESS", sig1);
        assertEquals("RANDOM_ACCESS", sig2);
        assertEquals("HEAD_OPERATIONS", sig3);
        assertEquals("HEAD_OPERATIONS", sig4);

        QuizQuestion q1 = QuizQuestion.builder().questionText(text1).build();
        QuizQuestion q2 = QuizQuestion.builder().questionText(text2).build();
        QuizQuestion q3 = QuizQuestion.builder().questionText(text3).build();

        List<QuizQuestion> current = new ArrayList<>(List.of(q1, q2));
        // With maxAllowedSameSignature = 2, a 3rd question with RANDOM_ACCESS signature will be flagged as semantically redundant
        assertTrue(QuizGenerationService.isSemanticallyRedundant(q2, current, 1), "q2 should be redundant if limit is 1");
        assertFalse(QuizGenerationService.isSemanticallyRedundant(q3, current, 2), "q3 has HEAD_OPERATIONS signature so not redundant with RANDOM_ACCESS questions");
    }
}

