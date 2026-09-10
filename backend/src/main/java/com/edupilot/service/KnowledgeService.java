package com.edupilot.service;

import com.edupilot.dto.ConceptMasteryResponse;
import com.edupilot.dto.KnowledgeProfileResponse;
import com.edupilot.model.AssessmentResult;
import com.edupilot.model.ConceptMastery;
import com.edupilot.model.KnowledgeProfile;
import com.edupilot.model.StudentProfile;
import com.edupilot.repository.ConceptMasteryRepository;
import com.edupilot.repository.KnowledgeProfileRepository;
import com.edupilot.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    @Autowired
    private ConceptMasteryRepository conceptRepository;

    @Autowired
    private KnowledgeProfileRepository profileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private StudentStateSnapshotService studentStateSnapshotService;

    /**
     * Process diagnostic assessment results & update persistent Knowledge Profile.
     */
    public KnowledgeProfile processAssessmentResult(AssessmentResult result) {
        if (result == null || result.getUserId() == null) {
            return null;
        }

        String userId = studentService.resolveUserId(result.getUserId());
        String subjectCode = result.getSubjectCode();
        String subjectName = result.getSubjectName();

        if (result.getUserAnswers() != null) {
            for (AssessmentResult.UserAnswer ans : result.getUserAnswers()) {
                String topic = ans.getTopic() != null ? ans.getTopic() : "General";
                updateSingleConceptMastery(userId, result.getStudentProfileId(), subjectCode, subjectName, topic, ans.isCorrect());
            }
        } else {
            syncKnowledgeProfileSummary(userId, subjectName);
        }

        try {
            studentStateSnapshotService.captureSnapshot(userId);
        } catch (Exception ex) {
            System.err.println("Snapshot error: " + ex.getMessage());
        }

        return profileRepository.findByUserId(userId).orElse(null);
    }

    /**
     * Standardized single concept mastery update (Authoritative Source of Truth).
     */
    public ConceptMastery updateSingleConceptMastery(String userId, String studentProfileId, String subjectCode, String subjectName, String topic, boolean isCorrect) {
        if (userId == null) return null;
        String conceptName = (topic != null && !topic.isBlank()) ? topic : "General";
        String sCode = (subjectCode != null && !subjectCode.isBlank()) ? subjectCode : "CS301";
        String sName = (subjectName != null && !subjectName.isBlank()) ? subjectName : "General";

        Optional<ConceptMastery> opt = conceptRepository.findByUserIdAndSubjectCodeAndTopicAndConceptName(
                userId, sCode, conceptName, conceptName
        );

        ConceptMastery cm = opt.orElseGet(() -> {
            ConceptMastery c = new ConceptMastery();
            c.setUserId(userId);
            c.setStudentProfileId(studentProfileId);
            c.setSubjectCode(sCode);
            c.setSubjectName(sName);
            c.setTopic(conceptName);
            c.setConceptName(conceptName);
            return c;
        });

        int attempts = cm.getAttemptCount() + 1;
        int corrects = cm.getCorrectCount() + (isCorrect ? 1 : 0);
        int wrongs = attempts - corrects;

        int recentWrongs = cm.getRecentWrongAnswerCount();
        if (isCorrect) {
            recentWrongs = Math.max(0, recentWrongs - 1);
        } else {
            recentWrongs = recentWrongs + 1;
        }

        double accuracy = Math.round((corrects * 100.0 / attempts) * 10.0) / 10.0;
        
        // V1 Sample-size evidence-confidence heuristic: min(attempts / 4.0 * 100.0, 100.0)
        double confidence = Math.min(Math.round((attempts / 4.0 * 100.0) * 10.0) / 10.0, 100.0);

        // 1. MasteryLevel based STRICTLY on accuracy (observed performance):
        ConceptMastery.MasteryLevel level;
        if (accuracy >= 85.0) {
            level = ConceptMastery.MasteryLevel.MASTER;
        } else if (accuracy >= 70.0) {
            level = ConceptMastery.MasteryLevel.PROFICIENT;
        } else if (accuracy >= 50.0) {
            level = ConceptMastery.MasteryLevel.INTERMEDIATE;
        } else {
            level = ConceptMastery.MasteryLevel.BEGINNER;
        }

        // 2. ConceptStatus based on evidence/reliability state:
        ConceptMastery.ConceptStatus status;
        if (attempts == 0) {
            status = ConceptMastery.ConceptStatus.UNASSESSED;
        } else if (confidence < 50.0) { // attempts < 2 (insufficient evidence)
            status = ConceptMastery.ConceptStatus.UNCERTAIN;
        } else if (accuracy >= 70.0) {
            status = ConceptMastery.ConceptStatus.STRONG; // confidence >= 50%, accuracy >= 70%
        } else {
            status = ConceptMastery.ConceptStatus.WEAK; // confidence >= 50%, accuracy < 70%
        }

        String action;
        if (status == ConceptMastery.ConceptStatus.UNCERTAIN) {
            action = "Targeted adaptive testing required for " + conceptName + " to gather sufficient evidence.";
        } else if (status == ConceptMastery.ConceptStatus.WEAK) {
            action = "Review core foundational concepts & practice EASY problems for " + conceptName;
        } else {
            action = "Mastery achieved! Maintain velocity with periodic spaced retention.";
        }

        cm.setAttemptCount(attempts);
        cm.setCorrectCount(corrects);
        cm.setAccuracy(accuracy);
        cm.setWrongCount(wrongs);
        cm.setRecentWrongAnswerCount(recentWrongs);
        cm.setMasteryScore(accuracy);
        cm.setConfidenceScore(confidence);
        cm.setMasteryLevel(level);
        cm.setStatus(status);
        cm.setRecommendedAction(action);
        cm.setLastAssessedAt(LocalDateTime.now());

        System.out.println("[MASTERY DEBUG CALCULATION] concept=" + conceptName + ", attempts=" + attempts + ", corrects=" + corrects + ", accuracy=" + accuracy + "%, status=" + status);

        ConceptMastery savedCm = conceptRepository.save(cm);

        syncKnowledgeProfileSummary(userId, sName);

        return savedCm;
    }

    public void syncKnowledgeProfileSummary(String userId, String subjectName) {
        if (userId == null) return;
        String canonicalUserId = studentService.resolveUserId(userId);

        List<ConceptMastery> allConcepts = conceptRepository.findByUserId(canonicalUserId);

        int mastered = 0;
        int proficient = 0;
        int intermediate = 0;
        int beginner = 0;
        List<String> strongList = new ArrayList<>();
        List<String> weakList = new ArrayList<>();

        for (ConceptMastery cm : allConcepts) {
            boolean isStrong = cm.getStatus() == ConceptMastery.ConceptStatus.STRONG || (cm.getAttemptCount() >= 1 && cm.getAccuracy() >= 70.0);
            boolean isWeak = cm.getStatus() == ConceptMastery.ConceptStatus.WEAK || (cm.getAttemptCount() >= 1 && cm.getAccuracy() < 70.0);

            if (isStrong) {
                strongList.add(cm.getConceptName());
            } else if (isWeak) {
                weakList.add(cm.getConceptName());
            }

            if (cm.getMasteryLevel() == ConceptMastery.MasteryLevel.MASTER) {
                mastered++;
            } else if (cm.getMasteryLevel() == ConceptMastery.MasteryLevel.PROFICIENT) {
                proficient++;
            } else if (cm.getMasteryLevel() == ConceptMastery.MasteryLevel.INTERMEDIATE) {
                intermediate++;
            } else {
                beginner++;
            }
        }

        double healthScore = 0.0;
        if (!allConcepts.isEmpty()) {
            double totalAcc = allConcepts.stream().mapToDouble(ConceptMastery::getAccuracy).sum();
            healthScore = Math.round((totalAcc / allConcepts.size()) * 10.0) / 10.0;
        }

        KnowledgeProfile kp = profileRepository.findByUserId(canonicalUserId).orElseGet(() -> {
            KnowledgeProfile k = new KnowledgeProfile();
            k.setUserId(canonicalUserId);
            return k;
        });

        kp.setTotalConceptsTracked(allConcepts.size());
        kp.setMasteredCount(mastered);
        kp.setProficientCount(proficient);
        kp.setIntermediateCount(intermediate);
        kp.setBeginnerCount(beginner);
        kp.setLearningHealthScore(healthScore);
        kp.setStrongConcepts(strongList);
        kp.setWeakConcepts(weakList);
        kp.setUpdatedAt(LocalDateTime.now());

        profileRepository.save(kp);

        // Sync with StudentProfile (uses StudentService.findOrCreateProfile to auto-create profile if missing)
        StudentProfile prof = studentService.findOrCreateProfile(canonicalUserId);

        String sName = (subjectName != null && !subjectName.isBlank()) ? subjectName : "General";
        String canonicalSubjectName = normalizeSubjectName(sName, prof.getSubjects());

        // Calculate subject-specific health score
        double subjectHealthScore;
        List<ConceptMastery> subjectConcepts = allConcepts.stream()
                .filter(c -> c.getSubjectName() != null && isSameSubject(c.getSubjectName(), canonicalSubjectName))
                .collect(Collectors.toList());
        if (!subjectConcepts.isEmpty()) {
            double subjectAccSum = subjectConcepts.stream().mapToDouble(ConceptMastery::getAccuracy).sum();
            subjectHealthScore = Math.round((subjectAccSum / subjectConcepts.size()) * 10.0) / 10.0;
        } else if (prof.getConceptMastery() != null && prof.getConceptMastery().containsKey(canonicalSubjectName)) {
            subjectHealthScore = prof.getConceptMastery().get(canonicalSubjectName);
        } else {
            subjectHealthScore = healthScore;
        }

        Map<String, List<String>> weakMap = prof.getWeakConcepts() != null ? prof.getWeakConcepts() : new HashMap<>();
        weakMap.put(canonicalSubjectName, weakList);
        prof.setWeakConcepts(weakMap);

        Map<String, List<String>> strongMap = prof.getStrongConcepts() != null ? prof.getStrongConcepts() : new HashMap<>();
        strongMap.put(canonicalSubjectName, strongList);
        prof.setStrongConcepts(strongMap);

        Map<String, Double> masteryMap = prof.getConceptMastery() != null ? prof.getConceptMastery() : new HashMap<>();
        masteryMap.put(canonicalSubjectName, subjectHealthScore);
        prof.setConceptMastery(masteryMap);

        studentProfileRepository.save(prof);

        System.out.println("[PROFILE DEBUG AFTER] userId=" + canonicalUserId + ", subject=" + canonicalSubjectName + ", updatedMastery=" + subjectHealthScore + "%, strongCount=" + strongList.size() + ", weakCount=" + weakList.size());

        // Trigger real-time recommendation engine generation
        try {
            recommendationService.generateRecommendations(canonicalUserId);
        } catch (Exception ex) {
            System.err.println("Failed to trigger recommendation generation: " + ex.getMessage());
        }
    }

    public KnowledgeProfileResponse getKnowledgeProfile(String userId) {
        if (userId == null) return new KnowledgeProfileResponse(new KnowledgeProfile(), Collections.emptyList());
        String canonicalUserId = studentService.resolveUserId(userId);

        KnowledgeProfile kp = profileRepository.findByUserId(canonicalUserId).orElseGet(() -> {
            KnowledgeProfile k = new KnowledgeProfile();
            k.setUserId(canonicalUserId);
            return k;
        });

        List<ConceptMasteryResponse> entries = conceptRepository.findByUserId(canonicalUserId)
                .stream()
                .map(ConceptMasteryResponse::new)
                .collect(Collectors.toList());

        return new KnowledgeProfileResponse(kp, entries);
    }

    public List<ConceptMasteryResponse> getWeakConcepts(String userId) {
        if (userId == null) return Collections.emptyList();
        String canonicalUserId = studentService.resolveUserId(userId);
        return conceptRepository.findByUserId(canonicalUserId)
                .stream()
                .filter(cm -> cm.getStatus() == ConceptMastery.ConceptStatus.WEAK || (cm.getAttemptCount() >= 1 && cm.getAccuracy() < 70.0))
                .map(ConceptMasteryResponse::new)
                .collect(Collectors.toList());
    }

    public List<ConceptMasteryResponse> getStrongConcepts(String userId) {
        if (userId == null) return Collections.emptyList();
        String canonicalUserId = studentService.resolveUserId(userId);
        return conceptRepository.findByUserId(canonicalUserId)
                .stream()
                .filter(cm -> cm.getStatus() == ConceptMastery.ConceptStatus.STRONG || (cm.getAttemptCount() >= 1 && cm.getAccuracy() >= 70.0))
                .map(ConceptMasteryResponse::new)
                .collect(Collectors.toList());
    }

    public List<ConceptMasteryResponse> getConceptMastery(String userId) {
        if (userId == null) return Collections.emptyList();
        String canonicalUserId = studentService.resolveUserId(userId);
        return conceptRepository.findByUserId(canonicalUserId)
                .stream()
                .map(ConceptMasteryResponse::new)
                .collect(Collectors.toList());
    }

    public static String normalizeSubjectName(String rawSubject, List<String> canonicalSubjects) {
        if (rawSubject == null || rawSubject.isBlank()) return "General";
        String cleaned = cleanSubjectString(rawSubject);
        if (canonicalSubjects != null) {
            for (String canonical : canonicalSubjects) {
                if (canonical != null && cleanSubjectString(canonical).equalsIgnoreCase(cleaned)) {
                    return canonical;
                }
            }
        }
        return rawSubject.trim();
    }

    public static boolean isSameSubject(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        return cleanSubjectString(s1).equalsIgnoreCase(cleanSubjectString(s2));
    }

    private static String cleanSubjectString(String str) {
        return str.trim().replaceAll("(?i)\\band\\b", "&").replaceAll("\\s+", " ");
    }
}
