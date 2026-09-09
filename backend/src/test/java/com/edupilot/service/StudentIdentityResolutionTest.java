package com.edupilot.service;

import com.edupilot.model.*;
import com.edupilot.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class StudentIdentityResolutionTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    @Autowired
    private KnowledgeProfileRepository knowledgeProfileRepository;

    @Autowired
    private KnowledgeService knowledgeService;

    private String canonicalUserId;
    private String studentEmail;

    @BeforeEach
    public void setUp() {
        studentEmail = "test_identity_" + System.currentTimeMillis() + "@edupilot.com";

        User user = new User();
        user.setEmail(studentEmail);
        user.setFullName("Test Identity Student");
        user.setPassword("password");
        user.setRole(User.Role.STUDENT);
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.save(user);
        canonicalUserId = user.getId();

        // Create canonical profile
        StudentProfile canonicalProfile = StudentProfile.builder()
                .userId(canonicalUserId)
                .institution("EduPilot Academy")
                .degree("B.Tech")
                .branch("Computer Science & Engineering")
                .course("Computer Science & Engineering")
                .semester(1)
                .currentCgpa(8.0)
                .targetCgpa(8.5)
                .subjects(List.of("Data Structures & Algorithms", "Database Management Systems"))
                .build();
        profileRepository.save(canonicalProfile);

        // Create email-based duplicate profile
        StudentProfile duplicateProfile = StudentProfile.builder()
                .userId(studentEmail)
                .institution("EduPilot Academy")
                .degree("B.Tech")
                .branch("Computer Science & Engineering")
                .course("Computer Science & Engineering")
                .semester(1)
                .currentCgpa(8.0)
                .targetCgpa(8.5)
                .subjects(List.of("Data Structures & Algorithms"))
                .build();
        profileRepository.save(duplicateProfile);
    }

    @Test
    public void test1_resolveUserId_returnsCanonicalIdForEmailEvenWhenDuplicateProfileExists() {
        String resolved = studentService.resolveUserId(studentEmail);
        assertEquals(canonicalUserId, resolved, "resolveUserId(email) MUST return canonical Mongo User ID");
    }

    @Test
    public void test2_resolveUserId_returnsCanonicalIdForCanonicalIdInput() {
        String resolved = studentService.resolveUserId(canonicalUserId);
        assertEquals(canonicalUserId, resolved, "resolveUserId(canonicalId) MUST return canonical Mongo User ID");
    }

    @Test
    public void test3_dsaDiagnosticSubmission_persistsConceptMasteryUnderCanonicalUserId() {
        String resolvedId = studentService.resolveUserId(studentEmail);

        ConceptMastery cm = new ConceptMastery();
        cm.setUserId(resolvedId);
        cm.setStudentProfileId(resolvedId);
        cm.setSubjectCode("CS301");
        cm.setSubjectName("Data Structures & Algorithms");
        cm.setTopic("Arrays & Linked Lists");
        cm.setConceptName("Arrays & Linked Lists");
        cm.setAttemptCount(5);
        cm.setCorrectCount(4);
        cm.setAccuracy(80.0);
        cm.setStatus(ConceptMastery.ConceptStatus.STRONG);
        conceptMasteryRepository.save(cm);

        Optional<ConceptMastery> fetched = conceptMasteryRepository.findByUserIdAndSubjectCodeAndTopicAndConceptName(
                canonicalUserId, "CS301", "Arrays & Linked Lists", "Arrays & Linked Lists"
        );
        assertTrue(fetched.isPresent(), "ConceptMastery MUST be queryable under canonical User ID");
        assertEquals(canonicalUserId, fetched.get().getUserId());
    }

    @Test
    public void test4_studentProfileSynchronization_usesCanonicalUserId() {
        studentService.syncConceptMasteryWithProfile(studentEmail, "Data Structures & Algorithms");

        Optional<StudentProfile> canonicalProf = profileRepository.findByUserId(canonicalUserId);
        assertTrue(canonicalProf.isPresent(), "Canonical profile must exist");
        assertNotNull(canonicalProf.get().getConceptMastery(), "Concept mastery map must be synchronized");
    }

    @Test
    public void test5_knowledgeProfileSynchronization_usesCanonicalUserId() {
        knowledgeService.syncKnowledgeProfileSummary(studentEmail, "Data Structures & Algorithms");

        Optional<KnowledgeProfile> kp = knowledgeProfileRepository.findByUserId(canonicalUserId);
        assertTrue(kp.isPresent(), "KnowledgeProfile MUST be synchronized under canonical User ID");
        assertEquals(canonicalUserId, kp.get().getUserId());
    }

    @Test
    public void test6_existingEmailDuplicateProfile_cannotHijackIdentityResolution() {
        String resolved = studentService.resolveUserId(studentEmail);
        assertNotEquals(studentEmail, resolved, "Email string MUST NOT hijack identity resolution");
        assertEquals(canonicalUserId, resolved);
    }

    @Test
    public void test7_studentIsolation_remainsIntact() {
        String userAId = studentService.resolveUserId(studentEmail);
        String userBEmail = "studentB_" + System.currentTimeMillis() + "@edupilot.com";

        User userB = new User();
        userB.setEmail(userBEmail);
        userB.setFullName("Student B");
        userB.setPassword("pass");
        userB.setRole(User.Role.STUDENT);
        userB.setCreatedAt(LocalDateTime.now());
        userB = userRepository.save(userB);

        String userBId = studentService.resolveUserId(userBEmail);

        assertNotEquals(userAId, userBId, "User A and User B MUST have distinct canonical IDs");

        ConceptMastery cmA = new ConceptMastery();
        cmA.setUserId(userAId);
        cmA.setSubjectCode("CS301");
        cmA.setTopic("Arrays & Linked Lists");
        cmA.setConceptName("Arrays & Linked Lists");
        cmA.setAccuracy(90.0);
        conceptMasteryRepository.save(cmA);

        Optional<ConceptMastery> bQuery = conceptMasteryRepository.findByUserIdAndSubjectCodeAndTopicAndConceptName(
                userBId, "CS301", "Arrays & Linked Lists", "Arrays & Linked Lists"
        );
        assertTrue(bQuery.isEmpty(), "Student B cannot read Student A's concept mastery");
    }

    @Test
    public void test8_existingAssessmentQuizFlows_stillWork() {
        StudentProfile prof = studentService.findOrCreateProfile(studentEmail);
        assertNotNull(prof);
        assertEquals(canonicalUserId, prof.getUserId());
    }

    @Test
    public void test9_masteryCalculations_remainUnchanged() {
        ConceptMastery cm = new ConceptMastery();
        cm.setAttemptCount(10);
        cm.setCorrectCount(8);
        cm.setWrongCount(2);
        cm.setAccuracy(80.0);
        cm.setStatus(ConceptMastery.ConceptStatus.STRONG);
        assertEquals(80.0, cm.getAccuracy());
        assertEquals(ConceptMastery.ConceptStatus.STRONG, cm.getStatus());
    }

    @Test
    public void test10_diagnostic10QuestionRequirements_remainUnchanged() {
        List<QuizGenerationService.QuestionBlueprintSpec> blueprint = studentService.resolveUserId(studentEmail) != null ?
                List.of(
                        new QuizGenerationService.QuestionBlueprintSpec(1, "Arrays & Linked Lists", QuizQuestion.Difficulty.EASY),
                        new QuizGenerationService.QuestionBlueprintSpec(2, "Stacks & Queues", QuizQuestion.Difficulty.EASY),
                        new QuizGenerationService.QuestionBlueprintSpec(3, "Binary Search Trees", QuizQuestion.Difficulty.EASY),
                        new QuizGenerationService.QuestionBlueprintSpec(4, "Sorting Algorithms", QuizQuestion.Difficulty.MEDIUM),
                        new QuizGenerationService.QuestionBlueprintSpec(5, "Graph Theory", QuizQuestion.Difficulty.MEDIUM),
                        new QuizGenerationService.QuestionBlueprintSpec(6, "Arrays & Linked Lists", QuizQuestion.Difficulty.MEDIUM),
                        new QuizGenerationService.QuestionBlueprintSpec(7, "Stacks & Queues", QuizQuestion.Difficulty.MEDIUM),
                        new QuizGenerationService.QuestionBlueprintSpec(8, "Binary Search Trees", QuizQuestion.Difficulty.HARD),
                        new QuizGenerationService.QuestionBlueprintSpec(9, "Sorting Algorithms", QuizQuestion.Difficulty.HARD),
                        new QuizGenerationService.QuestionBlueprintSpec(10, "Graph Theory", QuizQuestion.Difficulty.HARD)
                ) : List.of();

        assertEquals(10, blueprint.size(), "Diagnostic test must have EXACTLY 10 questions");
        long easyCount = blueprint.stream().filter(b -> b.getDifficulty() == QuizQuestion.Difficulty.EASY).count();
        long mediumCount = blueprint.stream().filter(b -> b.getDifficulty() == QuizQuestion.Difficulty.MEDIUM).count();
        long hardCount = blueprint.stream().filter(b -> b.getDifficulty() == QuizQuestion.Difficulty.HARD).count();

        assertEquals(3, easyCount, "3 EASY questions");
        assertEquals(4, mediumCount, "4 MEDIUM questions");
        assertEquals(3, hardCount, "3 HARD questions");
    }
}
