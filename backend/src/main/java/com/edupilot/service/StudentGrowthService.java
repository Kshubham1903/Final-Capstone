package com.edupilot.service;

import com.edupilot.dto.StudentGrowthResponseDTO;
import com.edupilot.dto.StudentGrowthResponseDTO.*;
import com.edupilot.model.AssessmentResult;
import com.edupilot.model.ConceptMastery;
import com.edupilot.model.QuizSession;
import com.edupilot.model.StudentProfile;
import com.edupilot.model.StudentStateSnapshot;
import com.edupilot.repository.AssessmentResultRepository;
import com.edupilot.repository.ConceptMasteryRepository;
import com.edupilot.repository.QuizSessionRepository;
import com.edupilot.repository.StudentStateSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentGrowthService {

    @Autowired
    private StudentService studentService;

    @Autowired
    private AssessmentResultRepository assessmentResultRepository;

    @Autowired
    private QuizSessionRepository quizSessionRepository;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    @Autowired
    private StudentStateSnapshotRepository snapshotRepository;

    /**
     * Research-Grade Student Growth Calculation Pipeline.
     * Computes baseline K0, current Kt, cumulative growth (pp), recent gain (pp),
     * concepts improved (concept-consistent baseline comparison), weak concepts remaining,
     * and authentic chronological learning trajectory.
     */
    public StudentGrowthResponseDTO calculateStudentGrowth(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new StudentGrowthResponseDTO(
                    "", 0.0, 0.0, 0.0, 0.0, 0, 0,
                    Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(),
                    "Invalid user ID."
            );
        }

        String resolvedUserId = studentService.resolveUserId(userId);
        StudentProfile profile = studentService.findOrCreateProfile(resolvedUserId);

        // 1. Extract Baseline (K0) from earliest initial diagnostic (T0)
        Map<String, Double> baselineConceptMap = extractBaselineConceptAccuracies(resolvedUserId, profile.getId());
        double baselineKnowledge = computeAverageScore(baselineConceptMap);

        // 2. Extract Current Knowledge (Kt) from authoritative ConceptMastery records
        List<ConceptMastery> currentCmList = conceptMasteryRepository.findByUserId(resolvedUserId);
        if (currentCmList.isEmpty() && profile.getId() != null) {
            currentCmList = conceptMasteryRepository.findByUserId(profile.getId());
        }

        Map<String, Double> currentConceptMap = new LinkedHashMap<>();
        for (ConceptMastery cm : currentCmList) {
            String concept = cm.getTopic() != null && !cm.getTopic().isBlank() ? cm.getTopic() : cm.getConceptName();
            if (concept != null) {
                double acc = cm.getAccuracy() > 0 ? cm.getAccuracy() : cm.getMasteryScore();
                if (acc > 1.0) acc = acc; // In 0-100 percentage
                currentConceptMap.put(concept, round2(acc));
            }
        }

        // 3. Concept-Consistent Comparable Baseline & Current Knowledge Calculation
        double currentKnowledge;
        if (!baselineConceptMap.isEmpty()) {
            // Aggregate Kt strictly over the comparable baseline concept set
            double sumComparableCurrent = 0.0;
            int countComparable = 0;
            for (String bConcept : baselineConceptMap.keySet()) {
                if (currentConceptMap.containsKey(bConcept)) {
                    sumComparableCurrent += currentConceptMap.get(bConcept);
                    countComparable++;
                } else {
                    sumComparableCurrent += baselineConceptMap.get(bConcept);
                    countComparable++;
                }
            }
            currentKnowledge = countComparable > 0 ? round2(sumComparableCurrent / countComparable) : baselineKnowledge;
        } else if (!currentConceptMap.isEmpty()) {
            currentKnowledge = computeAverageScore(currentConceptMap);
            baselineKnowledge = currentKnowledge; // Fallback if no initial diagnostic exists
        } else {
            currentKnowledge = 50.0;
            baselineKnowledge = 50.0;
        }

        // 4. Calculate Growth Metrics (Percentage Points pp)
        double cumulativeGrowth = round2(currentKnowledge - baselineKnowledge);

        // 5. Concept Improvement Calculation (Requires T0 baseline evidence)
        List<ConceptImprovementDTO> improvementList = new ArrayList<>();
        int conceptsImprovedCount = 0;

        for (Map.Entry<String, Double> entry : baselineConceptMap.entrySet()) {
            String concept = entry.getKey();
            double bAcc = round2(entry.getValue());
            double cAcc = currentConceptMap.getOrDefault(concept, bAcc);
            double growthPp = round2(cAcc - bAcc);
            boolean isImproved = growthPp > 0.0; // Concept Improved = Current Concept Knowledge > Valid Baseline Concept Knowledge

            if (isImproved) {
                conceptsImprovedCount++;
            }

            improvementList.add(new ConceptImprovementDTO(
                    concept,
                    getSubjectForConcept(concept, currentCmList),
                    bAcc,
                    cAcc,
                    growthPp,
                    isImproved
            ));
        }

        // 6. Weak Concepts Remaining (Current state < 70% or WEAK)
        int weakConceptsCount = 0;
        for (ConceptMastery cm : currentCmList) {
            if (cm.getStatus() == ConceptMastery.ConceptStatus.WEAK || cm.getAccuracy() < 70.0) {
                weakConceptsCount++;
            }
        }

        // 7. Chronological Trajectory Construction (Authentic Persisted Events Only)
        List<GrowthTrajectoryPointDTO> trajectory = buildAuthenticTrajectory(resolvedUserId, profile, baselineKnowledge);

        // Calculate Recent Gain (Kt - Kt-1)
        double recentGain = 0.0;
        if (trajectory.size() >= 2) {
            GrowthTrajectoryPointDTO latest = trajectory.get(trajectory.size() - 1);
            GrowthTrajectoryPointDTO previous = trajectory.get(trajectory.size() - 2);
            recentGain = round2(latest.getKnowledgeScore() - previous.getKnowledgeScore());
        }

        // 8. Subject-Level Growth Breakdown (DSA, DBMS, AI, etc.)
        Map<String, SubjectGrowthDTO> subjectGrowthMap = buildSubjectGrowthBreakdown(resolvedUserId, profile, baselineConceptMap, currentCmList);

        // 9. Data Sufficiency Assessment
        String dataSufficiencyNote = trajectory.size() >= 5
                ? "Current historical assessment data is sufficient for growth measurement."
                : "Current historical data is sufficient for growth measurement (" + trajectory.size() + " events), but insufficient for reliable ML model training.";

        return new StudentGrowthResponseDTO(
                resolvedUserId,
                round2(baselineKnowledge),
                round2(currentKnowledge),
                cumulativeGrowth,
                recentGain,
                conceptsImprovedCount,
                weakConceptsCount,
                subjectGrowthMap,
                trajectory,
                improvementList,
                dataSufficiencyNote
        );
    }

    /**
     * Extracts baseline concept accuracies strictly from T0 initial diagnostic assessment.
     */

    public Map<String, Double> extractBaselineConceptAccuracies(String canonicalUserId, String profileId) {
        Map<String, Double> baselineMap = new LinkedHashMap<>();

        List<AssessmentResult> results = assessmentResultRepository.findByUserId(canonicalUserId);
        if (results.isEmpty() && profileId != null) {
            results = assessmentResultRepository.findByUserId(profileId);
        }

        if (!results.isEmpty()) {
            results.sort(Comparator.comparing(AssessmentResult::getCreatedAt));
            AssessmentResult diagnostic = results.get(0);

            if (diagnostic.getTopicBreakdown() != null && !diagnostic.getTopicBreakdown().isEmpty()) {
                for (Map.Entry<String, Map<String, Object>> entry : diagnostic.getTopicBreakdown().entrySet()) {
                    String topic = entry.getKey();
                    Map<String, Object> data = entry.getValue();
                    double score = parsePercentageScore(data);
                    baselineMap.put(topic, round2(score));
                }
            } else if (diagnostic.getUserAnswers() != null && !diagnostic.getUserAnswers().isEmpty()) {
                Map<String, int[]> counts = new HashMap<>();
                for (AssessmentResult.UserAnswer ans : diagnostic.getUserAnswers()) {
                    String topic = ans.getTopic() != null ? ans.getTopic() : "General";
                    int[] c = counts.computeIfAbsent(topic, k -> new int[2]);
                    c[1]++;
                    if (ans.isCorrect()) c[0]++;
                }
                for (Map.Entry<String, int[]> e : counts.entrySet()) {
                    double score = e.getValue()[1] > 0 ? (e.getValue()[0] * 100.0 / e.getValue()[1]) : 50.0;
                    baselineMap.put(e.getKey(), round2(score));
                }
            } else if (diagnostic.getPercentage() > 0) {
                baselineMap.put(diagnostic.getSubjectName() != null ? diagnostic.getSubjectName() + " Foundations" : "General Foundations", round2(diagnostic.getPercentage()));
            }
        }

        return baselineMap;
    }

    /**
     * Builds authentic chronological trajectory using ONLY persisted valid assessment events.
     * Subject baselines are immutable and calculated per subject.
     */
    public List<GrowthTrajectoryPointDTO> buildAuthenticTrajectory(String resolvedUserId, StudentProfile profile, double defaultBaselineKnowledge) {
        List<GrowthTrajectoryPointDTO> rawPoints = new ArrayList<>();

        // 1. Diagnostics (T0) - filter valid ones (totalQuestions > 0 or percentage > 0 or score > 0)
        List<AssessmentResult> diagnostics = assessmentResultRepository.findByUserId(resolvedUserId);
        if (diagnostics.isEmpty() && profile.getId() != null) {
            diagnostics = assessmentResultRepository.findByUserId(profile.getId());
        }

        for (AssessmentResult ar : diagnostics) {
            if (ar.getTotalQuestions() == 0 && ar.getPercentage() == 0.0 && ar.getScore() == 0) {
                continue; // Skip invalid / 0-question record
            }
            double score = ar.getPercentage() > 0 ? ar.getPercentage() : ar.getScore();
            if (score <= 1.0) score *= 100.0;
            score = round2(score);

            rawPoints.add(new GrowthTrajectoryPointDTO(
                    ar.getId() != null ? ar.getId() : UUID.randomUUID().toString(),
                    ar.getCreatedAt() != null ? ar.getCreatedAt() : LocalDateTime.now().minusDays(3),
                    "DIAGNOSTIC",
                    ar.getSubjectName() != null && !ar.getSubjectName().isBlank() ? ar.getSubjectName() : "General",
                    score,
                    score,
                    0.0,
                    0.0,
                    "Initial Diagnostic Baseline Assessment"
            ));
        }

        // 2. Quiz Sessions (Adaptive, Practice, Verification, Remediation) - filter valid ones
        List<QuizSession> quizSessions = quizSessionRepository.findByUserIdOrderByLastAnswerTimeAsc(resolvedUserId);
        if (quizSessions.isEmpty() && profile.getId() != null) {
            quizSessions = quizSessionRepository.findByUserIdOrderByLastAnswerTimeAsc(profile.getId());
        }

        for (QuizSession qs : quizSessions) {
            boolean hasAnswers = qs.getAnswers() != null && !qs.getAnswers().isEmpty();
            if (qs.getTotalQuestions() == 0 && !hasAnswers) {
                continue; // Skip empty/abandoned quiz session
            }
            if (qs.getStatus() == QuizSession.Status.ABANDONED && !hasAnswers) {
                continue;
            }

            int totalQ = qs.getTotalQuestions() > 0 ? qs.getTotalQuestions() : (hasAnswers ? qs.getAnswers().size() : 0);
            if (totalQ == 0) continue;

            double score = (qs.getCorrectCount() * 100.0 / totalQ);
            score = round2(score);

            String type = qs.isVerificationQuiz() ? "VERIFICATION_QUIZ" : qs.getModuleType().name() + "_QUIZ";

            rawPoints.add(new GrowthTrajectoryPointDTO(
                    qs.getId() != null ? qs.getId() : UUID.randomUUID().toString(),
                    qs.getLastAnswerTime() != null ? qs.getLastAnswerTime() : LocalDateTime.now(),
                    type,
                    qs.getSubjectName() != null && !qs.getSubjectName().isBlank() ? qs.getSubjectName() : "General",
                    score,
                    score,
                    0.0,
                    0.0,
                    type.replace("_", " ") + " Submission"
            ));
        }

        // 3. Fallback to Snapshots if no sessions present
        if (rawPoints.isEmpty()) {
            List<StudentStateSnapshot> snapshots = snapshotRepository.findByStudentIdOrderByTimestampAsc(resolvedUserId);
            for (StudentStateSnapshot s : snapshots) {
                double score = round2(s.getOverallKnowledgeScore() * 100.0);
                rawPoints.add(new GrowthTrajectoryPointDTO(
                        s.getId() != null ? s.getId() : UUID.randomUUID().toString(),
                        s.getTimestamp() != null ? s.getTimestamp() : LocalDateTime.now(),
                        "SNAPSHOT",
                        "General",
                        score,
                        score,
                        0.0,
                        0.0,
                        "State Snapshot Event"
                ));
            }
        }

        // Group points by Subject to compute subject-isolated immutable baselines, cumulative growth, and recent gains
        Map<String, List<GrowthTrajectoryPointDTO>> pointsBySubject = rawPoints.stream()
                .collect(Collectors.groupingBy(GrowthTrajectoryPointDTO::getSubjectName, LinkedHashMap::new, Collectors.toList()));

        List<GrowthTrajectoryPointDTO> processedPoints = new ArrayList<>();

        for (Map.Entry<String, List<GrowthTrajectoryPointDTO>> entry : pointsBySubject.entrySet()) {
            List<GrowthTrajectoryPointDTO> subjectPoints = entry.getValue();
            subjectPoints.sort(Comparator.comparing(GrowthTrajectoryPointDTO::getTimestamp));

            if (subjectPoints.isEmpty()) continue;

            // Earliest valid observation score in subject is the subject baseline K0
            double subjectBaselineK0 = subjectPoints.get(0).getKnowledgeScore();

            double prevScore = subjectBaselineK0;
            for (int i = 0; i < subjectPoints.size(); i++) {
                GrowthTrajectoryPointDTO p = subjectPoints.get(i);
                p.setBaselineKnowledge(round2(subjectBaselineK0)); // Immutable baseline for this subject!
                p.setCumulativeGrowth(round2(p.getKnowledgeScore() - subjectBaselineK0));

                if (i == 0) {
                    p.setRecentGain(0.0);
                    p.setAssessmentType("DIAGNOSTIC");
                    p.setDescription("Initial Diagnostic Baseline Assessment (T0)");
                } else {
                    p.setRecentGain(round2(p.getKnowledgeScore() - prevScore));
                    if ("DIAGNOSTIC".equals(p.getAssessmentType()) || "ASSESSMENT".equals(p.getAssessmentType())) {
                        p.setAssessmentType("PROGRESS_ASSESSMENT");
                        p.setDescription("Progress Assessment Checkpoint (T" + i + ")");
                    } else if (p.getDescription() != null && !p.getDescription().isBlank() && !p.getDescription().contains("(T")) {
                        p.setDescription(p.getDescription() + " (T" + i + ")");
                    } else if (p.getDescription() == null || p.getDescription().isBlank()) {
                        p.setDescription("Progress Checkpoint (T" + i + ")");
                    }
                }
                prevScore = p.getKnowledgeScore();
                processedPoints.add(p);
            }
        }

        // Re-sort overall trajectory chronologically
        processedPoints.sort(Comparator.comparing(GrowthTrajectoryPointDTO::getTimestamp));
        return processedPoints;
    }

    /**
     * Subject-level growth calculation (Subject Agnostic: DSA, DBMS, AI, OS, CN, etc.)
     */
    public Map<String, SubjectGrowthDTO> buildSubjectGrowthBreakdown(
            String resolvedUserId,
            StudentProfile profile,
            Map<String, Double> baselineConceptMap,
            List<ConceptMastery> currentCmList) {

        Map<String, List<ConceptMastery>> subjectGroup = currentCmList.stream()
                .filter(cm -> cm.getSubjectName() != null && !cm.getSubjectName().isBlank())
                .collect(Collectors.groupingBy(ConceptMastery::getSubjectName));

        if (subjectGroup.isEmpty() && profile.getSubjects() != null) {
            for (String subj : profile.getSubjects()) {
                subjectGroup.put(subj, Collections.emptyList());
            }
        }

        Map<String, SubjectGrowthDTO> result = new LinkedHashMap<>();

        for (Map.Entry<String, List<ConceptMastery>> entry : subjectGroup.entrySet()) {
            String subject = entry.getKey();
            List<ConceptMastery> cms = entry.getValue();

            double currentSubjAcc = 0.0;
            int total = cms.size();
            int weak = 0;

            if (total > 0) {
                double sum = 0.0;
                for (ConceptMastery cm : cms) {
                    double acc = cm.getAccuracy() > 0 ? cm.getAccuracy() : cm.getMasteryScore();
                    sum += acc;
                    if (cm.getStatus() == ConceptMastery.ConceptStatus.WEAK || acc < 70.0) {
                        weak++;
                    }
                }
                currentSubjAcc = round2(sum / total);
            } else if (profile.getConceptMastery() != null && profile.getConceptMastery().containsKey(subject)) {
                currentSubjAcc = round2(profile.getConceptMastery().get(subject));
            } else {
                currentSubjAcc = 50.0;
            }

            // Estimate subject baseline
            double baselineSubjAcc = currentSubjAcc;
            double sumBase = 0.0;
            int baseCount = 0;
            for (Map.Entry<String, Double> bEntry : baselineConceptMap.entrySet()) {
                sumBase += bEntry.getValue();
                baseCount++;
            }
            if (baseCount > 0) {
                baselineSubjAcc = round2(sumBase / baseCount);
            }

            double growthPp = round2(currentSubjAcc - baselineSubjAcc);

            result.put(subject, new SubjectGrowthDTO(
                    subject,
                    baselineSubjAcc,
                    currentSubjAcc,
                    growthPp,
                    total,
                    weak
            ));
        }

        return result;
    }

    /**
     * Extracts leak-free temporal feature dataset for future Student Growth ML model training.
     * Information at T_k uses strictly data at or before T_k; target is performance at T_{k+1} within the same subject trajectory.
     */
    public List<MLFeaturePointDTO> extractMLFeatureDataset(String userId) {
        String resolvedUserId = studentService.resolveUserId(userId);
        StudentProfile profile = studentService.findOrCreateProfile(resolvedUserId);

        List<ConceptMastery> currentCmList = conceptMasteryRepository.findByUserId(resolvedUserId);
        if (currentCmList.isEmpty() && profile.getId() != null) {
            currentCmList = conceptMasteryRepository.findByUserId(profile.getId());
        }

        // 1. Gather all valid raw observations
        List<RawObs> rawObservations = new ArrayList<>();

        // Diagnostics / Assessments
        List<AssessmentResult> diagnostics = assessmentResultRepository.findByUserId(resolvedUserId);
        if (diagnostics.isEmpty() && profile.getId() != null) {
            diagnostics = assessmentResultRepository.findByUserId(profile.getId());
        }
        for (AssessmentResult ar : diagnostics) {
            if (ar.getTotalQuestions() == 0 && ar.getPercentage() == 0.0 && ar.getScore() == 0) {
                continue;
            }
            double score = ar.getPercentage() > 0 ? ar.getPercentage() : ar.getScore();
            if (score <= 1.0) score *= 100.0;
            score = round2(score);

            double accuracy = ar.getAccuracy() > 0 ? ar.getAccuracy() : score;
            accuracy = round2(accuracy);

            String subject = ar.getSubjectName() != null && !ar.getSubjectName().isBlank() ? ar.getSubjectName() : "General";
            String concept = extractConceptFromAssessment(ar);

            Double confidence = extractEmpiricalConfidence(concept, currentCmList);

            rawObservations.add(new RawObs(
                    ar.getCreatedAt() != null ? ar.getCreatedAt() : LocalDateTime.now().minusDays(3),
                    subject,
                    concept,
                    score,
                    accuracy,
                    confidence,
                    "DIAGNOSTIC"
            ));
        }

        // Quiz Sessions
        List<QuizSession> quizSessions = quizSessionRepository.findByUserIdOrderByLastAnswerTimeAsc(resolvedUserId);
        if (quizSessions.isEmpty() && profile.getId() != null) {
            quizSessions = quizSessionRepository.findByUserIdOrderByLastAnswerTimeAsc(profile.getId());
        }
        for (QuizSession qs : quizSessions) {
            boolean hasAnswers = qs.getAnswers() != null && !qs.getAnswers().isEmpty();
            if (qs.getTotalQuestions() == 0 && !hasAnswers) {
                continue;
            }
            if (qs.getStatus() == QuizSession.Status.ABANDONED && !hasAnswers) {
                continue;
            }

            int totalQ = qs.getTotalQuestions() > 0 ? qs.getTotalQuestions() : (hasAnswers ? qs.getAnswers().size() : 0);
            if (totalQ == 0) continue;

            double score = round2(qs.getCorrectCount() * 100.0 / totalQ);
            double accuracy = score;

            String subject = qs.getSubjectName() != null && !qs.getSubjectName().isBlank() ? qs.getSubjectName() : "General";
            String concept = extractConceptFromQuizSession(qs);

            Double confidence = extractEmpiricalConfidence(concept, currentCmList);
            String activityType = qs.isVerificationQuiz() ? "VERIFICATION_QUIZ" : qs.getModuleType().name() + "_QUIZ";

            rawObservations.add(new RawObs(
                    qs.getLastAnswerTime() != null ? qs.getLastAnswerTime() : LocalDateTime.now(),
                    subject,
                    concept,
                    score,
                    accuracy,
                    confidence,
                    activityType
            ));
        }

        // Snapshots fallback if empty
        if (rawObservations.isEmpty()) {
            List<StudentStateSnapshot> snapshots = snapshotRepository.findByStudentIdOrderByTimestampAsc(resolvedUserId);
            for (StudentStateSnapshot s : snapshots) {
                double score = round2(s.getOverallKnowledgeScore() * 100.0);
                rawObservations.add(new RawObs(
                        s.getTimestamp() != null ? s.getTimestamp() : LocalDateTime.now(),
                        "General",
                        "General",
                        score,
                        score,
                        null,
                        "SNAPSHOT"
                ));
            }
        }

        // Group observations by Subject to guarantee subject-isolated trajectories, immutable baselines, and clean target calculation
        Map<String, List<RawObs>> obsBySubject = rawObservations.stream()
                .collect(Collectors.groupingBy(RawObs::getSubject, LinkedHashMap::new, Collectors.toList()));

        List<MLFeaturePointDTO> featureList = new ArrayList<>();

        for (Map.Entry<String, List<RawObs>> entry : obsBySubject.entrySet()) {
            List<RawObs> subjectObs = entry.getValue();
            subjectObs.sort(Comparator.comparing(RawObs::getTimestamp));

            if (subjectObs.isEmpty()) continue;

            // Earliest valid observation score in subject is the subject baseline K0
            double subjectBaselineK0 = subjectObs.get(0).getScore();

            for (int i = 0; i < subjectObs.size(); i++) {
                RawObs obs = subjectObs.get(i);
                double currentK = obs.getScore();
                double baselineK = round2(subjectBaselineK0); // IMMUTABLE baseline for this subject stream!
                double cumulativeGrowth = round2(currentK - baselineK);
                double recentGain = (i == 0) ? 0.0 : round2(currentK - subjectObs.get(i - 1).getScore());
                int attemptCountAtT = i + 1; // Cumulative attempts up to time T in this subject

                Double targetAtNext = (i + 1 < subjectObs.size()) ? subjectObs.get(i + 1).getScore() : null;

                featureList.add(new MLFeaturePointDTO(
                        resolvedUserId,
                        obs.getTimestamp(),
                        obs.getSubject(),
                        obs.getConcept(),
                        baselineK,
                        currentK,
                        cumulativeGrowth,
                        recentGain,
                        attemptCountAtT,
                        obs.getAccuracy(),
                        obs.getConfidence(),
                        obs.getActivityType(),
                        targetAtNext
                ));
            }
        }

        // Re-sort overall feature list chronologically
        featureList.sort(Comparator.comparing(MLFeaturePointDTO::getTimestamp));
        return featureList;
    }

    private static class RawObs {
        private final LocalDateTime timestamp;
        private final String subject;
        private final String concept;
        private final double score;
        private final double accuracy;
        private final Double confidence;
        private final String activityType;

        public RawObs(LocalDateTime timestamp, String subject, String concept, double score, double accuracy, Double confidence, String activityType) {
            this.timestamp = timestamp;
            this.subject = subject;
            this.concept = concept;
            this.score = score;
            this.accuracy = accuracy;
            this.confidence = confidence;
            this.activityType = activityType;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public String getSubject() { return subject; }
        public String getConcept() { return concept; }
        public double getScore() { return score; }
        public double getAccuracy() { return accuracy; }
        public Double getConfidence() { return confidence; }
        public String getActivityType() { return activityType; }
    }

    private String extractConceptFromAssessment(AssessmentResult ar) {
        if (ar.getTopicBreakdown() != null && !ar.getTopicBreakdown().isEmpty()) {
            if (ar.getTopicBreakdown().size() == 1) {
                return ar.getTopicBreakdown().keySet().iterator().next();
            }
        }
        if (ar.getUserAnswers() != null && !ar.getUserAnswers().isEmpty()) {
            Map<String, Long> topicCounts = ar.getUserAnswers().stream()
                    .filter(ans -> ans.getTopic() != null && !ans.getTopic().isBlank())
                    .collect(Collectors.groupingBy(AssessmentResult.UserAnswer::getTopic, Collectors.counting()));
            if (!topicCounts.isEmpty()) {
                return topicCounts.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
            }
        }
        return "General";
    }

    private String extractConceptFromQuizSession(QuizSession qs) {
        if (qs.getTargetConcept() != null && !qs.getTargetConcept().isBlank()) {
            return qs.getTargetConcept();
        }
        if (qs.getAnswers() != null && !qs.getAnswers().isEmpty()) {
            Map<String, Long> conceptCounts = qs.getAnswers().stream()
                    .filter(ans -> ans.getConcept() != null && !ans.getConcept().isBlank())
                    .collect(Collectors.groupingBy(QuizSession.QuizAnswerRecord::getConcept, Collectors.counting()));
            if (!conceptCounts.isEmpty()) {
                return conceptCounts.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
            }
        }
        return "General";
    }

    private Double extractEmpiricalConfidence(String concept, List<ConceptMastery> cmList) {
        if (concept == null || "General".equalsIgnoreCase(concept)) {
            return null;
        }
        for (ConceptMastery cm : cmList) {
            String cmConcept = cm.getTopic() != null && !cm.getTopic().isBlank() ? cm.getTopic() : cm.getConceptName();
            if (concept.equalsIgnoreCase(cmConcept) && cm.getConfidenceScore() > 0) {
                return round2(cm.getConfidenceScore());
            }
        }
        return null;
    }

    private double computeAverageScore(Map<String, Double> scoreMap) {
        if (scoreMap.isEmpty()) return 50.0;
        double sum = scoreMap.values().stream().mapToDouble(Double::doubleValue).sum();
        return round2(sum / scoreMap.size());
    }

    private String getSubjectForConcept(String concept, List<ConceptMastery> cms) {
        for (ConceptMastery cm : cms) {
            if (concept.equalsIgnoreCase(cm.getConceptName()) || concept.equalsIgnoreCase(cm.getTopic())) {
                return cm.getSubjectName() != null ? cm.getSubjectName() : "General";
            }
        }
        return "General";
    }

    private double parsePercentageScore(Map<String, Object> data) {
        if (data == null) return 50.0;
        if (data.containsKey("percentage")) {
            double p = ((Number) data.get("percentage")).doubleValue();
            return p <= 1.0 ? p * 100.0 : p;
        }
        if (data.containsKey("accuracy")) {
            double a = ((Number) data.get("accuracy")).doubleValue();
            return a <= 1.0 ? a * 100.0 : a;
        }
        if (data.containsKey("correct") && data.containsKey("total")) {
            double c = ((Number) data.get("correct")).doubleValue();
            double t = ((Number) data.get("total")).doubleValue();
            return t > 0 ? (c * 100.0 / t) : 50.0;
        }
        return 50.0;
    }

    private static double round2(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
