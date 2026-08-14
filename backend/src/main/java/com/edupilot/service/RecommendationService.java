package com.edupilot.service;

import com.edupilot.dto.*;
import com.edupilot.model.*;
import com.edupilot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private ConceptMasteryRepository conceptRepository;

    @Autowired
    private KnowledgeProfileRepository knowledgeProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private com.edupilot.repository.QuizSessionRepository quizSessionRepository;

    @Autowired
    private LearningPlannerService plannerService;

    /**
     * Normalizes dynamically generated concept names so that the same
     * concept does not create multiple ConceptMastery records.
     */
    public static String normalizeConceptName(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "General Concept";
        }

        String c = raw.trim();

        // Remove standard question preamble prefixes.
        c = c.replaceAll(
                "(?i)^(Regarding fundamental principles of|In an operational engineering context for|"
                        + "Under high-scale production constraints evaluating|In foundational study of|"
                        + "When implementing practical workflows for|Advanced application of|"
                        + "Foundational principles of)\\s+",
                ""
        );

        // Remove difficulty/tier/index suffixes.
        c = c.replaceAll(
                "(?i)\\s+(Implementation|Architecture|Foundations|Concepts|Mechanics|Principles)?"
                        + "\\s*[-|\\[\\(]?\\s*(EASY|MEDIUM|HARD|Tier\\s*\\d+)?\\s*#?\\d+[\\]\\)]?",
                ""
        );

        // Remove standalone difficulty/tier/index tags.
        c = c.replaceAll(
                "(?i)\\s*[-|\\[\\(]?\\s*(EASY|MEDIUM|HARD|Tier\\s*\\d+)\\s*[\\]\\)]?",
                ""
        );

        c = c.replaceAll("(?i)\\s*#\\d+.*$", "");

        // Remove trailing template qualifier words.
        if (c.matches(
                "(?i).+\\s+(Implementation|Architecture|Foundations|Mechanics|Concepts|Principles)$"
        )
                && !c.equalsIgnoreCase("Software Architecture")
                && !c.equalsIgnoreCase("System Architecture")) {

            c = c.replaceAll(
                    "(?i)\\s+(Implementation|Architecture|Foundations|Mechanics|Concepts|Principles)$",
                    ""
            );
        }

        // Clean punctuation.
        c = c.replaceAll("^[\\s:-]+|[\\s:-]+$", "");

        return c.trim();
    }

    /**
     * Subject-aware concept normalization.
     */
    public static String normalizeConceptName(String text, String subject) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String normSubject = subject != null
                ? subject.trim().toLowerCase()
                : "";

        if (normSubject.contains("artificial intelligence")
                || normSubject.equals("ai")) {

            return mapAiCanonicalConcept(text);
        }

        if (normSubject.contains("discrete")
                || normSubject.equals("dms")) {

            return mapDmsCanonicalConcept(text);
        }

        return normalizeConceptName(text);
    }

    /**
     * Maps AI questions to canonical concepts.
     */
    public static String mapAiCanonicalConcept(String text) {
        if (text == null) {
            return "Uninformed & Heuristic Search";
        }

        String low = text.toLowerCase();

        if (low.contains("search")
                || low.contains("dfs")
                || low.contains("bfs")
                || low.contains("heuristic")
                || low.contains("a*")
                || low.contains("uniform-cost")
                || low.contains("hill-climbing")
                || low.contains("simulated annealing")
                || low.contains("state space")
                || low.contains("greedy")
                || low.contains("pathfinding")
                || low.contains("local search")
                || low.contains("iterative deepening")
                || low.contains("bidirectional")) {

            if (!low.contains("game playing")
                    && !low.contains("minimax")
                    && !low.contains("alpha-beta")
                    && !low.contains("csp")
                    && !low.contains("constraint")) {

                return "Uninformed & Heuristic Search";
            }
        }

        if (low.contains("logic")
                || low.contains("propositional")
                || low.contains("predicate")
                || low.contains("first-order")
                || low.contains("resolution")
                || low.contains("modus ponens")
                || low.contains("horn clause")
                || low.contains("bayes")
                || low.contains("probability")
                || low.contains("wumpus")
                || low.contains("ontology")
                || low.contains("knowledge representation")
                || low.contains("backward chaining")
                || low.contains("forward chaining")
                || low.contains("markov")
                || low.contains("inference")
                || low.contains("clause")
                || low.contains("unification")
                || low.contains("variable elimination")
                || low.contains("fol")) {

            return "Logic & Automated Reasoning";
        }

        return "Game Theory & Constraint Satisfaction";
    }

    /**
     * Maps Discrete Mathematics questions to canonical concepts.
     */
    public static String mapDmsCanonicalConcept(String text) {
        if (text == null) {
            return "Set Theory & Mathematical Logic";
        }

        String low = text.toLowerCase();

        if (low.contains("graph")
                || low.contains("path")
                || low.contains("cycle")
                || low.contains("tree")
                || low.contains("bipartite")
                || low.contains("planar")
                || low.contains("degree")
                || low.contains("handshaking")
                || low.contains("hamiltonian")
                || low.contains("eulerian")
                || low.contains("chromatic")
                || low.contains("adjacency")
                || low.contains("poset")
                || low.contains("lattice")
                || low.contains("vertex")
                || low.contains("edge")
                || low.contains("walk")
                || low.contains("isomorphism")
                || low.contains("topological")) {

            if (!low.contains("inclusion-exclusion")
                    && !low.contains("recurrence")) {

                return "Graph Theory & Structural Properties";
            }
        }

        if (low.contains("combinatorics")
                || low.contains("permutation")
                || low.contains("combination")
                || low.contains("pigeonhole")
                || low.contains("recurrence")
                || low.contains("induction")
                || low.contains("inclusion-exclusion")
                || low.contains("generating function")
                || low.contains("binomial")
                || low.contains("counting")
                || low.contains("divisibility")
                || low.contains("modular")
                || low.contains("gcd")
                || low.contains("euclidean")
                || low.contains("catalan")
                || low.contains("stirling")
                || low.contains("derangement")) {

            return "Combinatorics & Recurrence Relations";
        }

        return "Set Theory & Mathematical Logic";
    }

    /**
     * Generate dynamic, explainable recommendations.
     *
     * Main logic:
     *
     * 1. Identify the student's subjects.
     * 2. Check whether the student has real topic-level performance data.
     * 3. If there is no real data, recommend Initial Diagnostic.
     * 4. If real data exists, completely exclude Initial Diagnostic.
     * 5. Calculate priority for every topic.
     * 6. Select the highest-priority topic.
     * 7. Save exactly one active recommendation per subject.
     * 8. Trigger learning-plan generation.
     */
    public List<RecommendationResponse> generateRecommendations(String userId) {

        if (userId == null || userId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Optional<StudentProfile> profOpt =
                studentProfileRepository.findByUserId(userId);

        if (profOpt.isEmpty()) {
            profOpt = studentProfileRepository.findById(userId);
        }

        if (profOpt.isEmpty()) {
            return Collections.emptyList();
        }

        StudentProfile profile = profOpt.get();

        List<String> subjects = profile.getSubjects();

        if (subjects == null || subjects.isEmpty()) {
            return Collections.emptyList();
        }

        List<Recommendation> generatedList = new ArrayList<>();

        /*
         * Latest quiz session is intentionally used as contextual information.
         * The final recommendation is still selected using historical
         * ConceptMastery + recent wrong-answer statistics.
         */
        Optional<QuizSession> latestSessionOpt =
                quizSessionRepository.findFirstByUserIdOrderByLastAnswerTimeDesc(userId);

        String latestSubjectName = null;
        String latestSubjectCode = null;

        if (latestSessionOpt.isPresent()) {

            QuizSession latestSession = latestSessionOpt.get();

            latestSubjectCode = latestSession.getSubjectCode();

            latestSubjectName = latestSession.getSubjectName();

            /*
             * Normalize the latest session's concepts so the recommendation
             * system can work consistently with dynamic quiz questions.
             */
            if (latestSession.getAnswers() != null) {

                for (QuizSession.QuizAnswerRecord answer
                        : latestSession.getAnswers()) {

                    if (answer == null || answer.getConcept() == null
                            || answer.getConcept().isBlank()) {
                        continue;
                    }

                    normalizeConceptName(
                            answer.getConcept(),
                            latestSubjectName
                    );
                }
            }
        }

        /*
         * Generate exactly one primary recommendation per subject.
         */
        for (String subjectName : subjects) {

            if (subjectName == null || subjectName.isBlank()) {
                continue;
            }

            Optional<Subject> catOpt =
                    subjectRepository.findBySubjectName(subjectName);

            final String finalSubjectCode =
                    catOpt.map(Subject::getSubjectCode).orElse("CS301");

            final String finalSubjectName = subjectName;

            /*
             * Fetch all ConceptMastery records for this subject.
             */
            List<ConceptMastery> userConceptMasteries =
                    conceptRepository.findByUserIdAndSubjectCode(
                            userId,
                            finalSubjectCode
                    );

            /*
             * Determine whether the student has actually attempted
             * topic-level questions for this subject.
             *
             * Initial Diagnostic records do NOT count as real performance.
             */
            boolean hasAnyRealPerformanceData = false;

            if (userConceptMasteries != null) {

                for (ConceptMastery cm : userConceptMasteries) {

                    if (cm == null) {
                        continue;
                    }

                    if (cm.getAttemptCount() > 0
                            && cm.getTopic() != null
                            && !cm.getTopic().isBlank()
                            && !cm.getTopic().equalsIgnoreCase("Initial Diagnostic")
                            && !cm.getTopic().equalsIgnoreCase("Initial Diagnostic Foundations")) {

                        hasAnyRealPerformanceData = true;
                        break;
                    }
                }
            }

            /*
             * Find all currently active recommendations for this subject.
             */
            List<Recommendation> activeRecs =
                    recommendationRepository.findByUserIdAndSubjectCodeAndStatus(
                            userId,
                            finalSubjectCode,
                            Recommendation.Status.ACTIVE
                    );

            Recommendation rec;

            if (activeRecs == null || activeRecs.isEmpty()) {

                rec = new Recommendation();

                rec.setUserId(userId);
                rec.setStudentProfileId(profile.getId());
                rec.setSubjectCode(finalSubjectCode);
                rec.setSubjectName(finalSubjectName);
                rec.setStatus(Recommendation.Status.ACTIVE);

            } else {

                /*
                 * Reuse the existing active recommendation so that the
                 * recommendation ID remains stable where possible.
                 */
                rec = activeRecs.get(0);

                /*
                 * Deactivate duplicate recommendations.
                 */
                for (int i = 1; i < activeRecs.size(); i++) {

                    Recommendation duplicate = activeRecs.get(i);

                    duplicate.setStatus(
                            Recommendation.Status.COMPLETED
                    );

                    recommendationRepository.save(duplicate);
                }
            }

            String previousTopic = rec.getTopic();

            /*
             * ============================================================
             * NEW STUDENT / NO REAL PERFORMANCE
             * ============================================================
             */
            if (!hasAnyRealPerformanceData) {

                /*
                 * Initial Diagnostic is ONLY allowed here.
                 */
                rec.setTopic("Initial Diagnostic");

                rec.setConceptName(
                        finalSubjectName + " Foundations"
                );

                rec.setReason(
                        "Baseline diagnostic evaluation required to map "
                                + "conceptual mastery for "
                                + finalSubjectName + "."
                );

                rec.setRecommendedAction(
                        "Take your first 5-minute diagnostic assessment for "
                                + finalSubjectName + "."
                );

                rec.setRecommendationType(
                        Recommendation.RecommendationType.DIAGNOSTIC_RETEST
                );

                rec.setPriority(
                        Recommendation.Priority.HIGH
                );

                rec.setDifficulty("MEDIUM");

                rec.setConfidenceScore(50.0);
                rec.setMasteryScore(50.0);
                rec.setAccuracy(50.0);

                rec.setPrevTopic(previousTopic);

                rec.setCreatedAt(LocalDateTime.now());

                rec.setExpiresAt(
                        LocalDateTime.now().plusDays(7)
                );

            } else {

                /*
                 * ========================================================
                 * EXISTING STUDENT
                 * ========================================================
                 *
                 * The student has real performance data.
                 *
                 * Initial Diagnostic is NEVER considered again.
                 */
                List<String> topics =
                        getTopicsForSubject(
                                finalSubjectName,
                                finalSubjectCode,
                                userId
                        );

                /*
                 * Explicitly remove all diagnostic placeholders.
                 */
                topics.removeIf(topic ->
                        topic != null
                                && (
                                topic.equalsIgnoreCase("Initial Diagnostic")
                                        || topic.equalsIgnoreCase("Initial Diagnostic Foundations")
                        )
                );

                String highestPriorityTopic = null;

                double maxScore = -1.0;

                ConceptMastery bestCm = null;

                /*
                 * Calculate priority for EVERY topic.
                 */
                for (String topic : topics) {

                    if (topic == null || topic.isBlank()) {
                        continue;
                    }

                    Optional<ConceptMastery> cmOpt =
                            conceptRepository
                                    .findByUserIdAndSubjectCodeAndTopicAndConceptName(
                                            userId,
                                            finalSubjectCode,
                                            topic,
                                            topic
                                    );

                    double accuracy = 50.0;

                    int attempts = 0;

                    int recentWrongs = 0;

                    boolean hasData = false;

                    ConceptMastery cm = null;

                    if (cmOpt.isPresent()) {

                        cm = cmOpt.get();

                        accuracy = cm.getAccuracy();

                        attempts = cm.getAttemptCount();

                        recentWrongs =
                                cm.getRecentWrongAnswerCount();

                        hasData = attempts > 0;
                    }

                    /*
                     * Priority formula:
                     *
                     * Unattempted topic = 45
                     *
                     * Attempted topic =
                     *     (100 - accuracy)
                     *     +
                     *     (recent wrong answers * 15)
                     *
                     * More wrong answers + lower accuracy
                     * = higher recommendation priority.
                     */
                    double priorityScore;

                    if (!hasData) {

                        priorityScore = 45.0;

                    } else {

                        priorityScore =
                                (100.0 - accuracy)
                                        + (recentWrongs * 15.0);
                    }

                    /*
                     * Select the highest-priority topic.
                     */
                    if (priorityScore > maxScore) {

                        maxScore = priorityScore;

                        highestPriorityTopic = topic;

                        bestCm = cm;
                    }
                }

                /*
                 * Safety fallback.
                 */
                if (highestPriorityTopic == null) {

                    highestPriorityTopic =
                            topics.isEmpty()
                                    ? "General Concepts"
                                    : topics.get(0);
                }

                rec.setTopic(highestPriorityTopic);

                rec.setConceptName(highestPriorityTopic);

                double accuracy =
                        bestCm != null
                                ? bestCm.getAccuracy()
                                : 50.0;

                int recentWrongs =
                        bestCm != null
                                ? bestCm.getRecentWrongAnswerCount()
                                : 0;

                int totalAttempts =
                        bestCm != null
                                ? bestCm.getAttemptCount()
                                : 0;

                /*
                 * ========================================================
                 * EXPLAINABLE REASON
                 * ========================================================
                 */
                String reason = "";

                /*
                 * If the recommendation changed from the previous topic,
                 * explain why.
                 */
                if (previousTopic != null
                        && !previousTopic.equalsIgnoreCase(
                        highestPriorityTopic)
                        && !previousTopic.equalsIgnoreCase(
                        "Initial Diagnostic")) {

                    Optional<ConceptMastery> previousCmOpt =
                            conceptRepository
                                    .findByUserIdAndSubjectCodeAndTopicAndConceptName(
                                            userId,
                                            finalSubjectCode,
                                            previousTopic,
                                            previousTopic
                                    );

                    if (previousCmOpt.isPresent()) {

                        double previousAccuracy =
                                previousCmOpt.get().getAccuracy();

                        reason =
                                "Your "
                                        + previousTopic
                                        + " accuracy improved to "
                                        + Math.round(previousAccuracy)
                                        + "%; "
                                        + highestPriorityTopic
                                        + " is now your highest-priority topic.";

                    } else {

                        reason =
                                "You completed "
                                        + previousTopic
                                        + "; "
                                        + highestPriorityTopic
                                        + " is now your highest-priority topic.";
                    }
                }

                /*
                 * If there is no topic-change explanation,
                 * explain the current weakness.
                 */
                if (reason.isEmpty()) {

                    if (recentWrongs > 0) {

                        reason =
                                "You answered "
                                        + recentWrongs
                                        + " of your recent questions on "
                                        + highestPriorityTopic
                                        + " incorrectly.";

                    } else if (totalAttempts == 0) {

                        reason =
                                "Establish foundational understanding of "
                                        + highestPriorityTopic
                                        + " to boost your mastery.";

                    } else {

                        reason =
                                "Your "
                                        + highestPriorityTopic
                                        + " accuracy is "
                                        + Math.round(accuracy)
                                        + "%. Practice to build consistency.";
                    }
                }

                rec.setReason(reason);

                /*
                 * ========================================================
                 * PRIORITY LEVEL
                 * ========================================================
                 */
                Recommendation.Priority priorityLevel;

                if (maxScore >= 75.0) {

                    priorityLevel =
                            Recommendation.Priority.CRITICAL;

                } else if (maxScore >= 50.0) {

                    priorityLevel =
                            Recommendation.Priority.HIGH;

                } else if (maxScore >= 30.0) {

                    priorityLevel =
                            Recommendation.Priority.MEDIUM;

                } else {

                    priorityLevel =
                            Recommendation.Priority.LOW;
                }

                rec.setPriority(priorityLevel);

                /*
                 * ========================================================
                 * DIFFICULTY
                 * ========================================================
                 */
                String difficulty = "MEDIUM";

                if (accuracy < 50.0) {

                    difficulty = "EASY";

                } else if (accuracy >= 75.0) {

                    difficulty = "HARD";
                }

                rec.setDifficulty(difficulty);

                /*
                 * ========================================================
                 * RECOMMENDATION METADATA
                 * ========================================================
                 */
                rec.setRecommendationType(
                        Recommendation.RecommendationType.CONCEPT_REVISION
                );

                rec.setRecommendedAction(
                        "Solve practice questions on "
                                + highestPriorityTopic
                                + "."
                );

                rec.setEstimatedStudyTimeMinutes(20);

                rec.setConfidenceScore(accuracy);

                rec.setMasteryScore(
                        bestCm != null
                                ? bestCm.getMasteryScore()
                                : 50.0
                );

                rec.setAccuracy(accuracy);

                rec.setPrevTopic(previousTopic);

                rec.setStatus(
                        Recommendation.Status.ACTIVE
                );

                rec.setCreatedAt(LocalDateTime.now());

                rec.setExpiresAt(
                        LocalDateTime.now().plusDays(7)
                );
            }

            /*
             * Save the one primary recommendation for this subject.
             */
            generatedList.add(
                    recommendationRepository.save(rec)
            );
        }

        /*
         * Regenerate the student's learning plan based on
         * the newly calculated recommendation.
         */
        try {

            plannerService.generateLearningPlan(userId);

        } catch (Exception ex) {

            System.err.println(
                    "Failed to trigger planner generation: "
                            + ex.getMessage()
            );
        }

        return generatedList.stream()
                .map(RecommendationResponse::new)
                .collect(Collectors.toList());
    }

    public static List<String> getSubjectBlueprintConcepts(String subjectName) {
        if (subjectName == null || subjectName.isBlank()) {
            return List.of("Core Principles", "Foundational Concepts", "Advanced System Architecture", "Operational Frameworks", "Optimization & Best Practices");
        }
        String lower = subjectName.toLowerCase().trim();
        if (lower.contains("physics")) {
            return List.of("Mechanics", "Kinematics & Laws of Motion", "Work, Energy & Power", "Thermodynamics", "Optics & Electromagnetism");
        } else if (lower.contains("database") || lower.contains("dbms") || lower.contains("sql")) {
            return List.of("Relational Data Modeling", "SQL Query Optimization", "Database Normalization", "ACID Transactions", "Indexing & B-Trees");
        } else if (lower.contains("java") || lower.contains("object oriented") || lower.contains("oop")) {
            return List.of("Classes & Encapsulation", "Inheritance & Polymorphism", "Exception Handling", "Java Collections Framework", "Multithreading & Concurrency");
        } else if (lower.contains("operating") || lower.contains("os")) {
            return List.of("Process Synchronization & Deadlocks", "CPU Scheduling Algorithms", "Virtual Memory & Paging", "File Systems Architecture", "System Calls & Kernel Architecture");
        } else if (lower.contains("network") || lower.contains("cn")) {
            return List.of("OSI & TCP/IP Model", "IP Addressing & Subnetting", "Routing Algorithms", "Transport Layer Protocols (TCP/UDP)", "Network Security & Cryptography");
        } else if (lower.contains("discrete") || lower.contains("math")) {
            return List.of("Set Theory & Relations", "Graph Theory & Trees", "Combinatorics & Permutations", "Propositional Logic", "Boolean Algebra");
        } else if (lower.contains("programming in c") || lower.equals("c")) {
            return List.of("Pointers & Memory Allocation", "Structures & Unions", "File I/O Operations", "Control Flow & Functions", "Arrays & Strings");
        } else if (lower.contains("software engineering") || lower.contains("testing")) {
            return List.of("Agile & Scrum Methodologies", "Requirements Engineering", "Software Design Patterns", "CI/CD & DevOps Pipelines", "Software Testing & QA");
        } else if (lower.contains("artificial intelligence") || lower.contains("machine learning") || lower.equals("ai")) {
            return List.of("Supervised & Unsupervised Learning", "Neural Networks & Deep Learning", "Model Evaluation & Metrics", "Feature Engineering", "Reinforcement Learning");
        } else if (lower.contains("compiler")) {
            return List.of("Lexical Analysis & Parsing", "Syntax Directed Translation", "Intermediate Code Generation", "Code Optimization", "Symbol Tables");
        } else if (lower.contains("cloud")) {
            return List.of("IaaS, PaaS & SaaS Models", "Virtualization & Containers", "Cloud Storage & Databases", "Identity & Access Management (IAM)", "Serverless Computing");
        } else if (lower.contains("cyber") || lower.contains("security") || lower.contains("cryptography")) {
            return List.of("Symmetric & Asymmetric Encryption", "Public Key Infrastructure (PKI)", "Network Security & Firewalls", "Threat Analysis & Vulnerabilities", "Authentication Protocols");
        } else if (lower.contains("data structure") || lower.contains("algorithm") || lower.contains("dsa")) {
            return List.of("Arrays & Linked Lists", "Stacks & Queues", "Binary Search Trees", "Sorting Algorithms", "Graph Theory & Dynamic Programming");
        } else {
            return List.of(
                "Core Principles of " + subjectName,
                "Foundational Concepts of " + subjectName,
                "Advanced System Architecture of " + subjectName,
                "Operational Frameworks of " + subjectName,
                "Optimization & Best Practices in " + subjectName
            );
        }
    }

    public static boolean isConceptValidForSubject(String subjectName, String conceptName) {
        if (subjectName == null || conceptName == null || conceptName.isBlank()) return true;
        String targetLower = conceptName.trim().toLowerCase();
        List<String> validConcepts = getSubjectBlueprintConcepts(subjectName);
        for (String valid : validConcepts) {
            if (valid.toLowerCase().contains(targetLower) || targetLower.contains(valid.toLowerCase())) {
                return true;
            }
        }
        if (targetLower.contains("core") || targetLower.contains("principle") || targetLower.contains("general") || targetLower.contains("foundations") || targetLower.contains("advanced")) {
            return true;
        }
        String subjLower = subjectName.toLowerCase();
        if (!subjLower.contains("data structure") && !subjLower.contains("algorithm") && !subjLower.contains("dsa")) {
            if (targetLower.contains("binary search tree") || targetLower.contains("sorting algorithm") || targetLower.contains("hash table") || targetLower.contains("dynamic programming")) {
                return false;
            }
        }
        if (!subjLower.contains("database") && !subjLower.contains("dbms") && !subjLower.contains("sql")) {
            if (targetLower.contains("sql query") || targetLower.contains("database normalization") || targetLower.contains("acid transactions")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the standard topic catalogue for a subject and also adds
     * dynamically discovered topics from ConceptMastery.
     */
    private List<String> getTopicsForSubject(
            String subjectName,
            String subjectCode,
            String userId) {

        Set<String> topics = new LinkedHashSet<>();
        topics.addAll(getSubjectBlueprintConcepts(subjectName));

        /*
         * Add dynamically generated concepts discovered in the student's
         * ConceptMastery records.
         */
        List<ConceptMastery> cmList =
                conceptRepository.findByUserId(userId);

        if (cmList != null) {

            for (ConceptMastery cm : cmList) {

                if (cm == null) {
                    continue;
                }

                if (cm.getSubjectName() != null
                        && cm.getSubjectCode() != null
                        && (
                        cm.getSubjectName().equalsIgnoreCase(subjectName)
                                || cm.getSubjectCode().equalsIgnoreCase(subjectCode)
                )) {

                    if (cm.getTopic() != null
                            && !cm.getTopic().isBlank()) {

                        topics.add(cm.getTopic());
                    }
                }
            }
        }

        return new ArrayList<>(topics);
    }

    /**
     * Returns active and verification-pending recommendations.
     */
    public List<RecommendationResponse> getActiveRecommendations(
            String userId) {

        List<Recommendation> active =
                recommendationRepository
                        .findByUserIdAndStatusOrderByCreatedAtDesc(
                                userId,
                                Recommendation.Status.ACTIVE
                        );

        List<Recommendation> pending =
                recommendationRepository
                        .findByUserIdAndStatusOrderByCreatedAtDesc(
                                userId,
                                Recommendation.Status.VERIFICATION_PENDING
                        );

        List<Recommendation> all =
                new ArrayList<>(active);

        all.addAll(pending);

        /*
         * If there are no active recommendations, generate them.
         */
        if (all.isEmpty()) {

            return generateRecommendations(userId);
        }

        return all.stream()
                .map(RecommendationResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Returns high-priority recommendations.
     */
    public List<RecommendationResponse> getHighPriorityRecommendations(
            String userId) {

        List<Recommendation.Priority> priorities =
                List.of(
                        Recommendation.Priority.CRITICAL,
                        Recommendation.Priority.HIGH
                );

        List<Recommendation> highList =
                recommendationRepository
                        .findByUserIdAndPriorityInAndStatus(
                                userId,
                                priorities,
                                Recommendation.Status.ACTIVE
                        );

        if (highList.isEmpty()) {

            return getActiveRecommendations(userId);
        }

        return highList.stream()
                .map(RecommendationResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Marks a recommendation as completed.
     */
    public RecommendationResponse completeRecommendation(String id) {

        Recommendation rec =
                recommendationRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Recommendation ID not found: "
                                                + id
                                )
                        );

        rec.setStatus(
                Recommendation.Status.COMPLETED
        );

        Recommendation saved =
                recommendationRepository.save(rec);

        return new RecommendationResponse(saved);
    }

    /**
     * Processes verification/diagnostic results for a concept.
     */
    public void processVerificationResult(
            String userId,
            String subjectName,
            String targetConcept,
            boolean isPassed,
            double accuracy) {

        if (userId == null
                || targetConcept == null
                || targetConcept.isBlank()) {

            return;
        }

        String normConcept =
                normalizeConceptName(targetConcept);

        /*
         * ================================================================
         * 1. UPDATE CONCEPT MASTERY
         * ================================================================
         */
        List<ConceptMastery> cmList =
                conceptRepository.findByUserId(userId);

        ConceptMastery targetCm = null;

        for (ConceptMastery cm : cmList) {

            if (normConcept.equalsIgnoreCase(
                    cm.getConceptName())) {

                targetCm = cm;
                break;
            }
        }

        if (targetCm == null) {

            targetCm = new ConceptMastery();

            targetCm.setUserId(userId);

            targetCm.setSubjectName(
                    subjectName != null
                            ? subjectName
                            : "Data Structures & Algorithms"
            );

            targetCm.setConceptName(normConcept);

            targetCm.setTopic(normConcept);
        }

        targetCm.setAttemptCount(
                targetCm.getAttemptCount() + 1
        );

        if (isPassed) {

            targetCm.setCorrectCount(
                    targetCm.getCorrectCount() + 1
            );

            targetCm.setAccuracy(
                    Math.max(85.0, accuracy)
            );

            targetCm.setMasteryLevel(
                    ConceptMastery.MasteryLevel.MASTER
            );

            targetCm.setConfidenceScore(100.0);

            targetCm.setRecommendedAction(
                    "Mastery achieved! Concept verified successfully."
            );

        } else {

            targetCm.setAccuracy(
                    Math.min(
                            targetCm.getAccuracy(),
                            accuracy
                    )
            );

            targetCm.setMasteryLevel(
                    ConceptMastery.MasteryLevel.BEGINNER
            );

            targetCm.setConfidenceScore(
                    Math.max(25.0, accuracy)
            );

            targetCm.setRecommendedAction(
                    "Practice needed: Review "
                            + normConcept
                            + " and attempt verification quiz again."
            );
        }

        targetCm.setLastAssessedAt(
                LocalDateTime.now()
        );

        conceptRepository.save(targetCm);

        /*
         * ================================================================
         * 2. UPDATE ASSOCIATED RECOMMENDATIONS
         * ================================================================
         */
        List<Recommendation> recs =
                recommendationRepository.findByUserIdAndStatus(
                        userId,
                        Recommendation.Status.VERIFICATION_PENDING
                );

        List<Recommendation> activeRecs =
                recommendationRepository.findByUserIdAndStatus(
                        userId,
                        Recommendation.Status.ACTIVE
                );

        recs.addAll(activeRecs);

        for (Recommendation r : recs) {

            if (!normConcept.equalsIgnoreCase(
                    r.getConceptName())) {

                continue;
            }

            if (isPassed) {

                r.setStatus(
                        Recommendation.Status.COMPLETED
                );

                recommendationRepository.save(r);

                if (r.getId() != null) {

                    plannerService.forceCompleteTask(
                            userId,
                            r.getId()
                    );
                }

            } else {

                r.setStatus(
                        Recommendation.Status.ACTIVE
                );

                r.setReason(
                        "In your verification quiz for "
                                + r.getSubjectName()
                                + ", mastery for "
                                + normConcept
                                + " was not demonstrated yet."
                );

                r.setRecommendedAction(
                        "Practice needed: Review "
                                + normConcept
                                + " fundamental concepts and attempt practice questions again."
                );

                recommendationRepository.save(r);
            }
        }

        /*
         * Recalculate the overall recommendation after verification.
         * This allows the recommendation to move to another weak topic
         * immediately after the student improves a concept.
         */
        try {

            generateRecommendations(userId);

        } catch (Exception ex) {

            System.err.println(
                    "Failed to regenerate recommendations after verification: "
                            + ex.getMessage()
            );
        }
    }
}