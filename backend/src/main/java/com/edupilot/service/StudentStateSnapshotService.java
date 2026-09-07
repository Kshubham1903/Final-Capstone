package com.edupilot.service;

import com.edupilot.dto.StudentStateHistoryDTO;
import com.edupilot.model.ConceptMastery;
import com.edupilot.model.StudentProfile;
import com.edupilot.model.StudentStateSnapshot;
import com.edupilot.repository.ConceptMasteryRepository;
import com.edupilot.repository.StudentStateSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentStateSnapshotService {

    @Autowired
    private StudentStateSnapshotRepository snapshotRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentStateService studentStateService;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    /**
     * Captures and persists a historical StudentStateSnapshot for a student.
     * Prevents duplicate snapshots if scores haven't changed within the duplicate window (5 seconds).
     */
    public StudentStateSnapshot captureSnapshot(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        String resolvedUserId = studentService.resolveUserId(userId);
        StudentProfile profile = studentService.findOrCreateProfile(resolvedUserId);

        double engagementScore = round2(studentStateService.computeEngagementScore(profile));

        List<ConceptMastery> cmList = conceptMasteryRepository.findByUserId(resolvedUserId);
        if (cmList.isEmpty() && profile.getId() != null) {
            cmList = conceptMasteryRepository.findByUserId(profile.getId());
        }

        Map<String, Double> topicMastery = new LinkedHashMap<>();
        double sumK = 0.0;
        int countK = 0;

        for (ConceptMastery cm : cmList) {
            String topic = cm.getTopic() != null && !cm.getTopic().isEmpty() ? cm.getTopic() : cm.getConceptName();
            if (topic != null) {
                double score = cm.getAccuracy() > 0 ? cm.getAccuracy() : (cm.getMasteryScore() / 100.0);
                if (score > 1.0) score /= 100.0;
                score = round2(score);
                topicMastery.put(topic, score);
                sumK += score;
                countK++;
            }
        }

        if (topicMastery.isEmpty() && profile.getConceptMastery() != null) {
            for (Map.Entry<String, Double> entry : profile.getConceptMastery().entrySet()) {
                double score = entry.getValue() > 1.0 ? entry.getValue() / 100.0 : entry.getValue();
                score = round2(score);
                topicMastery.put(entry.getKey(), score);
                sumK += score;
                countK++;
            }
        }

        double overallK = countK > 0 ? round2(sumK / countK) : 0.50;

        // Duplicate protection: return latest if recorded within 5s with identical scores
        Optional<StudentStateSnapshot> latestOpt = snapshotRepository.findTopByStudentIdOrderByTimestampDesc(resolvedUserId);
        if (latestOpt.isPresent()) {
            StudentStateSnapshot latest = latestOpt.get();
            boolean isRecent = latest.getTimestamp() != null && 
                    latest.getTimestamp().isAfter(LocalDateTime.now().minusSeconds(5));
            boolean isSameScore = Math.abs(latest.getOverallKnowledgeScore() - overallK) < 1e-4 &&
                    Math.abs(latest.getEngagementScore() - engagementScore) < 1e-4;

            if (isRecent && isSameScore) {
                return latest;
            }
        }

        StudentStateSnapshot snapshot = new StudentStateSnapshot(
                resolvedUserId,
                overallK,
                engagementScore,
                topicMastery
        );

        return snapshotRepository.save(snapshot);
    }

    /**
     * Get historical snapshots in chronological DTO format.
     */
    public List<StudentStateHistoryDTO> getStudentStateHistoryDTO(String userId) {
        String resolvedUserId = studentService.resolveUserId(userId);
        List<StudentStateSnapshot> history = snapshotRepository.findByStudentIdOrderByTimestampAsc(resolvedUserId);

        if (history.isEmpty()) {
            StudentStateSnapshot first = captureSnapshot(resolvedUserId);
            if (first != null) {
                history = List.of(first);
            }
        }

        return history.stream().map(s -> new StudentStateHistoryDTO(
                s.getTimestamp(),
                s.getOverallKnowledgeScore(),
                s.getEngagementScore(),
                s.getTopicMastery()
        )).collect(Collectors.toList());
    }

    private static double round2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
