package com.edupilot.service;

import com.edupilot.model.AssessmentResult;
import com.edupilot.model.StudentProfile;
import com.edupilot.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class StudentProfileMasteryTest {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentProfileRepository profileRepository;

    @BeforeEach
    public void setUp() {
        // Clean up test users
        profileRepository.findByUserId("test_user_step8_new_junit").ifPresent(p -> profileRepository.delete(p));
        profileRepository.findByUserId("test_user_step8_existing_junit").ifPresent(p -> profileRepository.delete(p));
    }

    @Test
    public void testAutoCreatesProfileForNewUserAndPersistsMastery() {
        String userId = "test_user_step8_new_junit";
        String subject = "Data Structures & Algorithms";

        // Verify profile does NOT exist yet
        assertTrue(profileRepository.findByUserId(userId).isEmpty(), "Profile should not exist initially");

        // Construct mock assessment result
        AssessmentResult result = new AssessmentResult();
        result.setUserId(userId);
        result.setSubjectCode("CS301");
        result.setSubjectName(subject);

        AssessmentResult.UserAnswer ans1 = new AssessmentResult.UserAnswer();
        ans1.setTopic("Arrays & Linked Lists");
        ans1.setCorrect(true);
        result.setUserAnswers(List.of(ans1));

        // Trigger processAssessmentResult
        knowledgeService.processAssessmentResult(result);

        // Verify StudentProfile auto-created and mastery persisted
        StudentProfile profile = studentService.findOrCreateProfile(userId);
        assertNotNull(profile, "Profile should be auto-created");
        assertNotNull(profile.getConceptMastery(), "conceptMastery map should not be null");
        assertTrue(profile.getConceptMastery().containsKey(subject), "conceptMastery should contain subject key");

        Double masteryScore = profile.getConceptMastery().get(subject);
        assertNotNull(masteryScore, "Mastery score should not be null");
        assertTrue(masteryScore > 0, "Mastery score should be > 0");

        String originalProfileId = profile.getId();

        // Process a second assessment result for the SAME user
        AssessmentResult result2 = new AssessmentResult();
        result2.setUserId(userId);
        result2.setSubjectCode("CS301");
        result2.setSubjectName(subject);

        AssessmentResult.UserAnswer ans2 = new AssessmentResult.UserAnswer();
        ans2.setTopic("Stacks & Queues");
        ans2.setCorrect(true);
        result2.setUserAnswers(List.of(ans2));

        knowledgeService.processAssessmentResult(result2);

        // Verify profile updated without creating duplicate profile
        StudentProfile updatedProfile = studentService.findOrCreateProfile(userId);
        assertEquals(originalProfileId, updatedProfile.getId(), "Profile ID should remain identical (no duplicate profile created)");
    }

    @Test
    public void testExistingUserUpdatesProfileCorrectly() {
        String userId = "test_user_step8_existing_junit";
        String subject = "Operating Systems";

        // Pre-create profile
        StudentProfile preProfile = studentService.findOrCreateProfile(userId);
        preProfile.setBranch("Computer Science");
        profileRepository.save(preProfile);

        String expectedId = preProfile.getId();

        // Process assessment result
        AssessmentResult result = new AssessmentResult();
        result.setUserId(userId);
        result.setSubjectCode("CS302");
        result.setSubjectName(subject);

        AssessmentResult.UserAnswer ans = new AssessmentResult.UserAnswer();
        ans.setTopic("Processes & Threads");
        ans.setCorrect(true);
        result.setUserAnswers(List.of(ans));

        knowledgeService.processAssessmentResult(result);

        // Verify existing profile updated
        StudentProfile postProfile = studentService.findOrCreateProfile(userId);
        assertEquals(expectedId, postProfile.getId(), "Existing profile ID must be preserved");
        assertTrue(postProfile.getConceptMastery().containsKey(subject), "Existing profile must be updated with concept mastery");
    }
}
