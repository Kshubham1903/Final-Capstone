package com.edupilot.service;

import com.edupilot.model.ConceptMastery;
import com.edupilot.model.StudentProfile;
import com.edupilot.model.User;
import com.edupilot.repository.AssessmentResultRepository;
import com.edupilot.repository.ConceptMasteryRepository;
import com.edupilot.repository.QuizSessionRepository;
import com.edupilot.repository.RemediationSessionRepository;
import com.edupilot.repository.StudentProfileRepository;
import com.edupilot.repository.StudentStateSnapshotRepository;
import com.edupilot.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Idempotent, environment-independent classification service for legacy STUDENT accounts.
 * Classifies legacy users (where accountType is null/unclassified) based on actual persisted
 * academic participation and test automation provenance.
 *
 * Rules:
 * 1. Explicit accountType (GENUINE_STUDENT, TEST_AUTOMATION, SYNTHETIC_RESEARCH_SEED) is NEVER overwritten.
 * 2. Real academic participation + no automation provenance -> GENUINE_STUDENT.
 * 3. Clearly automated/test-generated activity -> TEST_AUTOMATION.
 * 4. No meaningful academic activity -> remain null (unclassified).
 * 5. Operating on 0 users (fresh database) completes cleanly with 0 updates.
 */
@Service
public class LegacyAccountClassificationService {

    private static final Logger log = LoggerFactory.getLogger(LegacyAccountClassificationService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private RemediationSessionRepository remediationSessionRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    @Autowired
    private StudentStateSnapshotRepository snapshotRepository;

    public int classifyLegacyStudentAccounts() {
        List<User> studentUsers = userRepository.findByRole(User.Role.STUDENT);
        if (studentUsers == null || studentUsers.isEmpty()) {
            log.info(">>> Legacy Classification: 0 student users found in repository.");
            return 0;
        }

        // Find users requiring classification (accountType is null)
        List<User> legacyCandidates = studentUsers.stream()
                .filter(u -> u.getAccountType() == null)
                .toList();

        if (legacyCandidates.isEmpty()) {
            log.info(">>> Legacy Classification: All student users have explicit accountType. No migration required.");
            return 0;
        }

        log.info(">>> Legacy Classification: Found {} unclassified student accounts to evaluate.", legacyCandidates.size());

        // Map profiles to resolve userId / email / profileId lookup keys
        List<StudentProfile> allProfiles = studentProfileRepository.findAll();
        Map<String, StudentProfile> profilesByUserId = new HashMap<>();
        Map<String, StudentProfile> profilesByEmail = new HashMap<>();
        for (StudentProfile p : allProfiles) {
            if (p.getUserId() != null && !p.getUserId().isBlank()) {
                profilesByUserId.put(p.getUserId(), p);
            }
            if (p.getEmail() != null && !p.getEmail().isBlank()) {
                profilesByEmail.put(p.getEmail(), p);
            }
        }

        int updatedCount = 0;

        for (User u : legacyCandidates) {
            String userId = u.getId();
            String email = u.getEmail();
            String fullName = u.getFullName();

            // Match keys for querying activity
            Set<String> matchKeys = new HashSet<>();
            if (userId != null && !userId.isBlank()) matchKeys.add(userId);
            if (email != null && !email.isBlank()) matchKeys.add(email);

            StudentProfile profile = (userId != null) ? profilesByUserId.get(userId) : null;
            if (profile == null && email != null) {
                profile = profilesByEmail.get(email);
            }
            if (profile != null) {
                if (profile.getId() != null) matchKeys.add(profile.getId());
                if (profile.getUserId() != null) matchKeys.add(profile.getUserId());
            }

            // Academic activity counts
            long assessmentCount = countAssessments(matchKeys);
            long quizCount = countQuizzes(matchKeys);
            long remediationCount = countRemediations(matchKeys);
            long conceptMasteryCount = countConceptMasteries(matchKeys);
            long snapshotCount = countSnapshots(matchKeys);

            long totalAcademicActivity = assessmentCount + quizCount + remediationCount + conceptMasteryCount + snapshotCount;

            boolean isAutomation = isAutomatedTestRunner(email, fullName);

            if (isAutomation) {
                u.setAccountType(User.AccountType.TEST_AUTOMATION);
                userRepository.save(u);
                updatedCount++;
                log.info(">>> Legacy Classification: User [{}] classified as TEST_AUTOMATION (test runner provenance).", email != null ? email : userId);
            } else if (totalAcademicActivity > 0) {
                u.setAccountType(User.AccountType.GENUINE_STUDENT);
                userRepository.save(u);
                updatedCount++;
                log.info(">>> Legacy Classification: User [{}] classified as GENUINE_STUDENT ({} academic records found).", email != null ? email : userId, totalAcademicActivity);
            } else {
                // No meaningful activity -> remain null (unclassified)
                log.info(">>> Legacy Classification: User [{}] remains unclassified (null accountType, 0 academic records).", email != null ? email : userId);
            }
        }

        log.info(">>> Legacy Classification: Completed. Updated {} legacy student accounts.", updatedCount);
        return updatedCount;
    }

    private boolean isAutomatedTestRunner(String email, String fullName) {
        if (email != null) {
            String lowerEmail = email.toLowerCase();
            if (lowerEmail.startsWith("growth_test_") || lowerEmail.startsWith("ml_test_") ||
                lowerEmail.startsWith("test_") || lowerEmail.startsWith("smoke_") ||
                lowerEmail.startsWith("smoketest_") || lowerEmail.startsWith("reg_audit_") ||
                lowerEmail.startsWith("wizard_student_") || lowerEmail.startsWith("studentb_") ||
                lowerEmail.startsWith("audit_") || lowerEmail.startsWith("jwt_") ||
                lowerEmail.startsWith("new_flow_student_") || lowerEmail.startsWith("phase") ||
                lowerEmail.contains("test_identity") || lowerEmail.contains("@test.") || lowerEmail.contains("dummy")) {
                return true;
            }
        }
        if (fullName != null) {
            String lowerName = fullName.toLowerCase();
            if (lowerName.contains("test identity") || lowerName.contains("automation bot") || lowerName.contains("smoke test")) {
                return true;
            }
        }
        return false;
    }

    private long countAssessments(Set<String> matchKeys) {
        return assessmentResultRepository.findAll().stream()
                .filter(ar -> (ar.getUserId() != null && matchKeys.contains(ar.getUserId())) ||
                              (ar.getStudentProfileId() != null && matchKeys.contains(ar.getStudentProfileId())))
                .count();
    }

    private long countQuizzes(Set<String> matchKeys) {
        return quizSessionRepository.findAll().stream()
                .filter(qs -> (qs.getUserId() != null && matchKeys.contains(qs.getUserId())) ||
                              (qs.getStudentProfileId() != null && matchKeys.contains(qs.getStudentProfileId())))
                .count();
    }

    private long countRemediations(Set<String> matchKeys) {
        return remediationSessionRepository.findAll().stream()
                .filter(rs -> (rs.getStudentId() != null && matchKeys.contains(rs.getStudentId())))
                .count();
    }

    private long countConceptMasteries(Set<String> matchKeys) {
        return conceptMasteryRepository.findAll().stream()
                .filter(cm -> (cm.getUserId() != null && matchKeys.contains(cm.getUserId())) ||
                              (cm.getStudentProfileId() != null && matchKeys.contains(cm.getStudentProfileId())))
                .count();
    }

    private long countSnapshots(Set<String> matchKeys) {
        return snapshotRepository.findAll().stream()
                .filter(s -> (s.getStudentId() != null && matchKeys.contains(s.getStudentId())))
                .count();
    }
}
