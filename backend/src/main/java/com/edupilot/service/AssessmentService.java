package com.edupilot.service;

import com.edupilot.dto.*;
import com.edupilot.model.*;
import com.edupilot.repository.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessmentService {

    @Autowired
    private AssessmentQuestionRepository questionRepository;

    @Autowired
    private AssessmentSessionRepository sessionRepository;

    @Autowired
    private AssessmentResultRepository resultRepository;

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private ConceptMasteryRepository conceptRepository;

    @Autowired
    private AdaptiveSessionRepository adaptiveSessionRepository;

    @Autowired
    private QuizQuestionRepository quizQuestionRepository;

    @Autowired
    private QuizGenerationService quizGenerationService;

    @Autowired
    private RecommendationService recommendationService;

    private final Map<String, Object> sessionLocks = new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<String> activePrefetchSessions = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<String, java.util.concurrent.CompletableFuture<QuizQuestion>> inFlightGenerations = new java.util.concurrent.ConcurrentHashMap<>();

    @PostConstruct
    public void initDefaultQuestionBank() {
        if (questionRepository.findBySubjectCodeAndIsActiveTrue("DS-LL-PRE01").size() < 25) {
            List<AssessmentQuestion> qList = new ArrayList<>();

            // 8 EASY Questions
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Arrays & Linked Lists", "What is the time complexity to insert an element at the beginning of a singly Linked List when the head pointer is given?", List.of("O(1)", "O(N)", "O(log N)", "O(N^2)"), 0, "Inserting at the head of a singly linked list requires updating the new node's next pointer and head pointer, taking constant time O(1).", AssessmentQuestion.Difficulty.EASY, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Stacks & Queues", "Which data structure is most suitable for implementing a Queue where insertions occur at one end and deletions at the other in O(1) time?", List.of("Singly Linked List with Head and Tail pointers", "Array without dynamic resizing", "Binary Search Tree", "Stack"), 0, "A Linked List with head and tail pointers allows O(1) enqueue at tail and O(1) dequeue at head.", AssessmentQuestion.Difficulty.EASY, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Stacks & Queues", "What is the access order principle governing a Stack data structure?", List.of("First-In-First-Out (FIFO)", "Last-In-First-Out (LIFO)", "Random Access", "Highest-Priority-First"), 1, "A Stack operates on Last-In-First-Out (LIFO) order.", AssessmentQuestion.Difficulty.EASY, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Arrays & Linked Lists", "What is the average time complexity of accessing an element at a given index in a standard static Array?", List.of("O(N)", "O(1)", "O(log N)", "O(N^2)"), 1, "Arrays provide direct indexing via memory offset in constant time O(1).", AssessmentQuestion.Difficulty.EASY, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Sorting Algorithms", "Which sorting algorithm guarantees stable O(N log N) time complexity in its worst case?", List.of("Quick Sort", "Merge Sort", "Heap Sort", "Selection Sort"), 1, "Merge Sort is guaranteed O(N log N) worst-case time and preserves relative order of equal keys (stable).", AssessmentQuestion.Difficulty.EASY, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Graph Theory & Dynamic Programming", "Which graph traversal algorithm utilizes a Queue (FIFO) data structure?", List.of("Depth First Search (DFS)", "Breadth First Search (BFS)", "Dijkstra Algorithm", "Kruskal Algorithm"), 1, "BFS visits vertices level by level using a Queue.", AssessmentQuestion.Difficulty.EASY, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Sorting Algorithms", "What is the time complexity of Binary Search on a sorted array of N elements?", List.of("O(N)", "O(N log N)", "O(log N)", "O(1)"), 2, "Binary Search halves the search space each step, yielding logarithmic time O(log N).", AssessmentQuestion.Difficulty.EASY, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Arrays & Linked Lists", "What is the worst-case number of key comparisons required in a Linear Search on an unsorted array of length N?", List.of("1", "log N", "N / 2", "N"), 3, "Linear Search compares up to N elements if the target is at the end or missing.", AssessmentQuestion.Difficulty.EASY, 1, "MCQ", true));

            // 9 MEDIUM Questions
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Arrays & Linked Lists", "Floyd's Cycle-Finding Algorithm (Slow and Fast Pointers) detects a loop in a Linked List in what time and space complexity?", List.of("Time: O(N), Space: O(1)", "Time: O(N^2), Space: O(1)", "Time: O(N), Space: O(N)", "Time: O(log N), Space: O(1)"), 0, "Floyd's algorithm traverses the list using O(1) auxiliary space and detects cycles in O(N) time.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Arrays & Linked Lists", "What is a key structural advantage of a Doubly Linked List over a Singly Linked List?", List.of("Bidirectional traversal and O(1) node deletion given node pointer", "Lower memory consumption per node", "Cache locality optimization", "Random access by index in O(1) time"), 0, "Doubly Linked List node contains next and prev pointers allowing O(1) deletion given pointer to target node.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Binary Search Trees", "In hash tables, what collision resolution technique stores multiple key-value pairs in a linked list at the same bucket index?", List.of("Open Addressing", "Separate Chaining", "Linear Probing", "Quadratic Probing"), 1, "Separate Chaining maintains a linked list at each array bucket to resolve collisions.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Binary Search Trees", "What is the worst-case search time complexity in an unbalanced (skewed) Binary Search Tree?", List.of("O(1)", "O(log N)", "O(N)", "O(N log N)"), 2, "An unbalanced BST degrades into a linear linked list with worst-case search time O(N).", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Sorting Algorithms", "Which in-place sorting algorithm constructs a max-heap to sort an array in O(N log N) worst-case time?", List.of("Quick Sort", "Insertion Sort", "Heap Sort", "Bubble Sort"), 2, "Heap Sort uses a max-heap structure to achieve guaranteed O(N log N) time in-place.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Graph Theory & Dynamic Programming", "Which graph traversal technique naturally uses a Stack (or function call stack) to explore as deep as possible before backtracking?", List.of("Breadth-First Search (BFS)", "Depth-First Search (DFS)", "Kruskal's Algorithm", "Prim's Algorithm"), 1, "DFS uses LIFO call stack or explicit stack to traverse deep into graph branches.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Stacks & Queues", "How can a Queue be implemented using two Stacks such that enqueue and dequeue operations achieve amortized O(1) time?", List.of("By pushing to Stack1 and popping from Stack2 (transferring when Stack2 is empty)", "By keeping Stack1 sorted at all times", "By alternating enqueues between Stack1 and Stack2", "It is impossible to implement a Queue with two Stacks"), 0, "Pushing to Stack1 and popping from Stack2 yields amortized O(1) time per operation.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Stacks & Queues", "In a Circular Array implementation of a Queue of size N, what formula updates the rear pointer upon enqueuing an item?", List.of("rear = rear + 1", "rear = (rear + 1) % N", "rear = (rear - 1) % N", "rear = rear * 2"), 1, "Modular arithmetic (rear + 1) % N wraps the index back to 0 when reaching array end.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Binary Search Trees", "What is the maximum tree height of a self-balancing AVL Tree containing N nodes?", List.of("O(N)", "O(log N)", "O(N^2)", "O(1)"), 1, "AVL tree balance factor guarantees tree height is bounded by O(log N).", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));

            // 8 HARD Questions
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Arrays & Linked Lists", "In a Circular Singly Linked List with N nodes, what is the time complexity to delete the last node if only the head pointer is maintained?", List.of("O(N) traversal to locate second-to-last node", "O(1) time with 0 pointer updates", "O(log N) traversal", "O(N^2) operations"), 0, "Deleting the last node requires traversing N-1 nodes to locate the second-to-last node.", AssessmentQuestion.Difficulty.HARD, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Graph Theory & Dynamic Programming", "What core properties must a computational problem exhibit for Dynamic Programming to be applicable?", List.of("Greedy Choice Property only", "Overlapping Subproblems & Optimal Substructure", "Divide & Conquer without subproblem reuse", "Randomized state transitions"), 1, "Dynamic Programming applies to problems exhibiting overlapping subproblems and optimal substructure.", AssessmentQuestion.Difficulty.HARD, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Graph Theory & Dynamic Programming", "Using a Min-Heap Priority Queue, what is the worst-case time complexity of Dijkstra's algorithm for a graph with V vertices and E edges?", List.of("O(V^2)", "O((V + E) log V)", "O(E log E)", "O(V * E)"), 1, "Min-heap priority queue implementation achieves O((V + E) log V) time complexity.", AssessmentQuestion.Difficulty.HARD, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Graph Theory & Dynamic Programming", "What algorithmic approach is used to perform Topological Sorting on a Directed Acyclic Graph (DAG) using vertex in-degrees?", List.of("Floyd-Warshall Algorithm", "Kahn's Algorithm (BFS-based)", "Bellman-Ford Algorithm", "Tarjan's Strongly Connected Components"), 1, "Kahn's algorithm repeatedly removes vertices with in-degree 0 using BFS queue.", AssessmentQuestion.Difficulty.HARD, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Binary Search Trees", "Which combination of data structures allows implementing an LRU (Least Recently Used) Cache with O(1) get and put operations?", List.of("Doubly Linked List + Hash Map", "Singly Linked List + Binary Search Tree", "Array + Stack", "Min-Heap + Queue"), 0, "Doubly Linked List maintains access order in O(1) while Hash Map provides O(1) key lookup.", AssessmentQuestion.Difficulty.HARD, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Binary Search Trees", "In an in-order traversal of a Binary Search Tree, what is the in-order successor of a node with a non-null right child?", List.of("The left-most node in its right subtree", "The right-most node in its left subtree", "Its immediate parent node", "The root node of the tree"), 0, "The minimum value key greater than the current node is the left-most node in its right subtree.", AssessmentQuestion.Difficulty.HARD, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Sorting Algorithms", "What choice of pivot element in Quick Sort leads to its worst-case O(N^2) performance on an already sorted array?", List.of("Random element", "Median-of-three element", "Always picking the first or last element", "Middle element"), 2, "Picking the first or last element on a sorted array produces extremely unbalanced partitions (N-1 and 0).", AssessmentQuestion.Difficulty.HARD, 1, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures & Algorithms", "Graph Theory & Dynamic Programming", "Which data structure optimizes Kruskal's Minimum Spanning Tree algorithm to detect cycles efficiently?", List.of("Disjoint Set Union (DSU) with Path Compression", "Adjacency Matrix", "Binary Max-Heap", "Segment Tree"), 0, "DSU with union-by-rank and path compression checks cycle formation in near-constant alpha(V) time.", AssessmentQuestion.Difficulty.HARD, 1, "MCQ", true));

            questionRepository.saveAll(qList);
        }
    }

    public List<Map<String, Object>> getAvailableAssessmentSubjects(String branch, int semester) {
        List<Subject> catalogSubjects;
        if (branch != null && !branch.trim().isEmpty() && semester > 0) {
            catalogSubjects = subjectRepository.findByBranchAndSemesterAndIsActiveTrue(branch, semester);
        } else if (branch != null && !branch.trim().isEmpty()) {
            catalogSubjects = subjectRepository.findByBranchAndIsActiveTrue(branch);
        } else {
            catalogSubjects = subjectRepository.findByIsActiveTrue();
        }

        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Subject s : catalogSubjects) {
            List<AssessmentQuestion> qList = questionRepository.findBySubjectCodeAndIsActiveTrue(s.getSubjectCode());
            Map<String, Object> map = new HashMap<>();
            map.put("subjectCode", s.getSubjectCode());
            map.put("subjectName", s.getSubjectName());
            map.put("branch", s.getBranch());
            map.put("semester", s.getSemester());
            map.put("credits", s.getCredits());
            map.put("questionCount", qList.size());
            resultList.add(map);
        }
        return resultList;
    }

    public List<QuizGenerationService.QuestionBlueprintSpec> buildAdaptiveBlueprint(String subjectName, String userId) {
        return buildAdaptiveBlueprint(subjectName, userId, 25);
    }

    public List<QuizGenerationService.QuestionBlueprintSpec> buildAdaptiveBlueprint(String subjectName, String userId, int targetCount) {
        List<QuizGenerationService.QuestionBlueprintSpec> blueprint = new ArrayList<>();
        List<String> blueprintConcepts = RecommendationService.getSubjectBlueprintConcepts(subjectName);

        if (blueprintConcepts == null || blueprintConcepts.isEmpty()) {
            blueprintConcepts = List.of("Arrays & Linked Lists", "Stacks & Queues", "Binary Search Trees", "Sorting Algorithms", "Graph Theory & Dynamic Programming");
        }

        int count = targetCount > 0 ? targetCount : 25;
        int easyCount = (int) Math.round(count * 0.3);
        int hardCount = (int) Math.round(count * 0.3);
        int mediumCount = count - easyCount - hardCount;

        QuizQuestion.Difficulty[] diffPattern = new QuizQuestion.Difficulty[count];
        int idx = 0;
        for (int i = 0; i < easyCount; i++) diffPattern[idx++] = QuizQuestion.Difficulty.EASY;
        for (int i = 0; i < mediumCount; i++) diffPattern[idx++] = QuizQuestion.Difficulty.MEDIUM;
        for (int i = 0; i < hardCount && idx < count; i++) diffPattern[idx++] = QuizQuestion.Difficulty.HARD;

        for (int i = 0; i < count; i++) {
            String c = blueprintConcepts.get(i % blueprintConcepts.size());
            blueprint.add(new QuizGenerationService.QuestionBlueprintSpec(i + 1, c, diffPattern[i]));
        }

        return blueprint;
    }

    public AssessmentSessionResponse startAssessmentSession(AssessmentStartRequest req) {
        String branch = req.getBranch() != null ? req.getBranch() : "Computer Science & Engineering";
        int semester = req.getSemester() > 0 ? req.getSemester() : 3;
        String subjectCode = req.getSubjectCode() != null ? req.getSubjectCode().trim().toUpperCase() : "CS301";
        String rawUserId = req.getUserId() != null ? req.getUserId() : "anonymous_student";
        String userId = studentService.resolveUserId(rawUserId);
        StudentProfile studentProfile = studentService.findOrCreateProfile(userId);

        int totalQuestions = req.getQuestionCount() > 0 ? req.getQuestionCount() : 25;

        String subjectName = req.getSubjectName() != null && !req.getSubjectName().isBlank()
                ? req.getSubjectName().trim()
                : null;
        if (subjectName == null) {
            subjectName = "Data Structures & Algorithms";
            Optional<Subject> sOpt = subjectRepository.findBySubjectCode(subjectCode);
            if (sOpt.isPresent()) {
                subjectName = sOpt.get().getSubjectName();
            }
        }

        List<QuizGenerationService.QuestionBlueprintSpec> blueprint = buildAdaptiveBlueprint(subjectName, userId, totalQuestions);

        List<AssessmentSessionResponse.QuestionItemDTO> dtoList = new ArrayList<>();
        List<String> questionIds = new ArrayList<>();
        List<String> fingerprints = new ArrayList<>();

        // Generate ONLY Question 1 upfront to avoid delaying user start
        QuizGenerationService.QuestionBlueprintSpec q1Spec = blueprint.get(0);
        Map<String, Object> genContext = new HashMap<>();
        genContext.put("adaptiveSummary", "Baseline " + totalQuestions + "-question initial assessment");
        genContext.put("purpose", "DIAGNOSTIC_QUESTION_1");

        QuizQuestion q1;
        try {
            q1 = quizGenerationService.generateSingleDiagnosticQuestion(subjectName, q1Spec, genContext, 1, totalQuestions);
        } catch (Exception ex) {
            System.err.println("[AssessmentService] Groq diagnostic Q1 generation failed: " + ex.getMessage());
            throw new IllegalStateException("Diagnostic question generation failed: " + ex.getMessage(), ex);
        }

        if (q1 == null) {
            throw new IllegalStateException("Diagnostic question generation failed: Q1 is null");
        }

        q1 = quizQuestionRepository.save(q1);

        questionIds.add(q1.getId());
        fingerprints.add(q1.getQuestionFingerprint() != null ? q1.getQuestionFingerprint() : q1.getQuestionText());

        AssessmentSessionResponse.QuestionItemDTO qDto = new AssessmentSessionResponse.QuestionItemDTO();
        qDto.setQuestionId(q1.getId());
        qDto.setTopic(q1.getConcept() != null ? q1.getConcept() : "Linked List");
        qDto.setQuestionText(q1.getQuestionText());
        qDto.setOptions(q1.getOptions());
        qDto.setMarks(1);
        qDto.setDifficulty(q1.getDifficulty() != null ? q1.getDifficulty().name() : "EASY");
        dtoList.add(qDto);

        AssessmentSession session = new AssessmentSession();
        session.setUserId(userId);
        session.setStudentProfileId(userId);
        session.setBranch(branch);
        session.setSemester(semester);
        session.setSubjectCode(subjectCode);
        session.setSubjectName(subjectName);
        session.setQuestionIds(questionIds);
        session.setUsedQuestionFingerprints(fingerprints);
        session.setTotalQuestions(totalQuestions);
        session.setTotalMarks(totalQuestions);
        session.setStatus(AssessmentSession.Status.IN_PROGRESS);
        session.setStartTime(LocalDateTime.now());
        session.setQuestionCount(0);
        session.setCurrentQuestionId(q1.getId());
        session.setActiveQuestionSubmitted(false);

        AssessmentSession savedSession = sessionRepository.save(session);

        // Single sequential prefetch for Question 2 in background
        prefetchNextQuestionAsync(savedSession.getId(), 1);

        AssessmentSessionResponse resp = new AssessmentSessionResponse();
        resp.setSessionId(savedSession.getId());
        resp.setBranch(branch);
        resp.setSemester(semester);
        resp.setSubjectCode(subjectCode);
        resp.setSubjectName(subjectName);
        resp.setTotalQuestions(totalQuestions);
        resp.setTotalMarks(totalQuestions);
        resp.setQuestions(dtoList);

        return resp;
    }

    public AssessmentResultResponse submitAssessment(AssessmentSubmissionRequest req) {
        AssessmentSession session = sessionRepository.findById(req.getSessionId())
                .orElseThrow(
                        () -> new IllegalArgumentException("Invalid assessment session ID: " + req.getSessionId()));

        List<AssessmentQuestion> questions = questionRepository.findAllById(session.getQuestionIds());
        Map<String, AssessmentQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(AssessmentQuestion::getId, q -> q));

        if (questionMap.isEmpty() && session.getQuestionIds() != null && !session.getQuestionIds().isEmpty()) {
            List<QuizQuestion> quizQuestions = quizQuestionRepository.findAllById(session.getQuestionIds());
            for (QuizQuestion qq : quizQuestions) {
                AssessmentQuestion aq = new AssessmentQuestion();
                aq.setId(qq.getId());
                aq.setTopic(qq.getConcept() != null ? qq.getConcept() : "Linked List");
                aq.setQuestionText(qq.getQuestionText());
                aq.setOptions(qq.getOptions());
                aq.setCorrectOptionIndex(qq.getCorrectOptionIndex());
                aq.setMarks(1);
                questionMap.put(qq.getId(), aq);
            }
        }

        int totalQuestions = session.getTotalQuestions();
        int totalMarks = totalQuestions;
        int correctAnswers = 0;
        int incorrectAnswers = 0;
        int skippedQuestions = 0;
        int score = 0;

        Map<String, Map<String, Object>> topicBreakdown = new HashMap<>();
        List<AssessmentResult.UserAnswer> userAnswersList = new ArrayList<>();
        Set<String> processedQuestionIds = new HashSet<>();

        if (req.getAnswers() != null) {
            for (AssessmentSubmissionRequest.AnswerItem ansItem : req.getAnswers()) {
                if (ansItem.getQuestionId() == null || processedQuestionIds.contains(ansItem.getQuestionId())) {
                    continue; // Skip duplicate question submission
                }
                processedQuestionIds.add(ansItem.getQuestionId());

                AssessmentQuestion q = questionMap.get(ansItem.getQuestionId());
                if (q == null)
                    continue;

                String topic = q.getTopic() != null ? q.getTopic() : "General";
                topicBreakdown.putIfAbsent(topic, new HashMap<>(Map.of("correct", 0, "total", 0, "percentage", 0.0)));
                Map<String, Object> topicStat = topicBreakdown.get(topic);
                topicStat.put("total", ((Number) topicStat.get("total")).intValue() + 1);

                boolean isCorrect = false;
                int marksObtained = 0;

                if (ansItem.getSelectedOption() < 0) {
                    skippedQuestions++;
                } else if (ansItem.getSelectedOption() == q.getCorrectOptionIndex()) {
                    isCorrect = true;
                    correctAnswers++;
                    marksObtained = 1;
                    score += 1;
                    topicStat.put("correct", ((Number) topicStat.get("correct")).intValue() + 1);
                } else {
                    incorrectAnswers++;
                }

                userAnswersList.add(new AssessmentResult.UserAnswer(q.getId(), topic, ansItem.getSelectedOption(),
                        isCorrect, marksObtained));
            }
        }

        if (processedQuestionIds.size() < totalQuestions) {
            skippedQuestions += (totalQuestions - processedQuestionIds.size());
        }

        // Compute topic percentages
        for (Map<String, Object> topicStat : topicBreakdown.values()) {
            int c = ((Number) topicStat.get("correct")).intValue();
            int t = ((Number) topicStat.get("total")).intValue();
            double pct = t > 0 ? (c * 100.0) / t : 0.0;
            topicStat.put("percentage", Math.round(pct * 10.0) / 10.0);
        }

        double percentage = totalMarks > 0 ? Math.round((score * 100.0 / totalMarks) * 10.0) / 10.0 : 0.0;
        int answeredCount = correctAnswers + incorrectAnswers;
        double accuracy = answeredCount > 0 ? Math.round((correctAnswers * 100.0 / answeredCount) * 10.0) / 10.0 : 0.0;

        String masteryLevel;
        if (percentage >= 85.0)
            masteryLevel = "MASTER";
        else if (percentage >= 70.0)
            masteryLevel = "PROFICIENT";
        else if (percentage >= 50.0)
            masteryLevel = "INTERMEDIATE";
        else
            masteryLevel = "NOVICE";

        session.setStatus(AssessmentSession.Status.COMPLETED);
        session.setEndTime(LocalDateTime.now());
        sessionRepository.save(session);

        AssessmentResult result = new AssessmentResult();
        result.setSessionId(session.getId());
        result.setUserId(session.getUserId());
        result.setStudentProfileId(session.getStudentProfileId());
        result.setBranch(session.getBranch());
        result.setSemester(session.getSemester());
        result.setSubjectCode(session.getSubjectCode());
        result.setSubjectName(session.getSubjectName());
        result.setTotalQuestions(totalQuestions);
        result.setCorrectAnswers(correctAnswers);
        result.setIncorrectAnswers(incorrectAnswers);
        result.setSkippedQuestions(skippedQuestions);
        result.setScore(score);
        result.setTotalMarks(totalMarks);
        result.setPercentage(percentage);
        result.setAccuracy(accuracy);
        result.setTimeTakenSeconds(req.getTimeTakenSeconds() > 0 ? req.getTimeTakenSeconds() : 60);
        result.setMasteryLevel(masteryLevel);
        result.setTopicBreakdown(topicBreakdown);
        result.setUserAnswers(userAnswersList);
        result.setCreatedAt(LocalDateTime.now());

        AssessmentResult savedResult = resultRepository.save(result);

        // Process knowledge engine mastery update
        try {
            knowledgeService.processAssessmentResult(savedResult);
        } catch (Exception ex) {
            System.err.println("Failed to process knowledge profile update: " + ex.getMessage());
        }

        // Sync Concept Mastery Map on StudentProfile
        if (session.getUserId() != null) {
            Optional<StudentProfile> profOpt = profileRepository.findByUserId(session.getUserId());
            if (profOpt.isPresent()) {
                StudentProfile prof = profOpt.get();
                Map<String, Double> masteryMap = prof.getConceptMastery() != null ? prof.getConceptMastery()
                        : new HashMap<>();
                masteryMap.put(session.getSubjectName(), percentage);
                prof.setConceptMastery(masteryMap);
                prof.setCompletedQuizzesCount(prof.getCompletedQuizzesCount() + 1);
                profileRepository.save(prof);
                try {
                    studentService.syncConceptMasteryWithProfile(session.getUserId(), session.getSubjectName());
                } catch (Exception ex) {
                    System.err.println("Failed profile mastery sync: " + ex.getMessage());
                }
            }
        }

        AssessmentResultResponse response = new AssessmentResultResponse(savedResult);

        // Build Adaptive Assessment Handoff Bridge for concepts evaluated in THIS
        // diagnostic session
        List<String> targetAdaptiveConcepts = new ArrayList<>();
        List<AssessmentResultResponse.ConceptEvaluationDTO> conceptEvaluations = new ArrayList<>();

        if (savedResult.getUserAnswers() != null && !savedResult.getUserAnswers().isEmpty()) {
            Set<String> evaluatedTopics = new LinkedHashSet<>();
            for (AssessmentResult.UserAnswer ans : savedResult.getUserAnswers()) {
                if (ans.getTopic() != null && !ans.getTopic().isBlank()) {
                    evaluatedTopics.add(ans.getTopic().trim());
                }
            }

            for (String topic : evaluatedTopics) {
                Optional<ConceptMastery> cmOpt = conceptRepository.findByUserIdAndSubjectCodeAndTopicAndConceptName(
                        savedResult.getUserId(), savedResult.getSubjectCode(), topic, topic);
                if (cmOpt.isPresent()) {
                    ConceptMastery cm = cmOpt.get();
                    boolean requiresAdaptive = (cm.getStatus() == ConceptMastery.ConceptStatus.UNCERTAIN
                            || cm.getStatus() == ConceptMastery.ConceptStatus.WEAK);
                    if (requiresAdaptive) {
                        targetAdaptiveConcepts.add(topic);
                    }
                    conceptEvaluations.add(new AssessmentResultResponse.ConceptEvaluationDTO(
                            topic,
                            cm.getAccuracy(),
                            cm.getAttemptCount(),
                            cm.getMasteryLevel().name(),
                            cm.getConfidenceScore(),
                            cm.getStatus() != null ? cm.getStatus().name() : "UNASSESSED",
                            requiresAdaptive));
                }
            }
        }

        response.setAdaptiveEligible(!targetAdaptiveConcepts.isEmpty());
        response.setTargetAdaptiveConcepts(targetAdaptiveConcepts);
        response.setConceptEvaluations(conceptEvaluations);

        return response;
    }

    public Optional<AssessmentResultResponse> getAssessmentResultById(String id) {
        return resultRepository.findById(id).map(AssessmentResultResponse::new);
    }

    public List<AssessmentResultResponse> getAssessmentHistoryByUserId(String userId) {
        return resultRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AssessmentResultResponse::new)
                .collect(Collectors.toList());
    }

    public Optional<AssessmentResultResponse> getLatestAssessmentResultByUserId(String userId) {
        return resultRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .map(AssessmentResultResponse::new);
    }

    // =========================================================================
    // PHASE 5: TRUE ONE-BY-ONE ADAPTIVE DIAGNOSTIC METHODS
    // =========================================================================

    public AdaptiveAssessmentDTOs.AdaptiveStartResponse startAdaptiveSession(
            AdaptiveAssessmentDTOs.AdaptiveStartRequest req, String authenticatedUserId) {
        String effectiveUserId = authenticatedUserId != null && !authenticatedUserId.isBlank()
                && !"anonymousUser".equals(authenticatedUserId)
                        ? authenticatedUserId
                        : req.getUserId();
        if (effectiveUserId == null || effectiveUserId.isBlank()) {
            effectiveUserId = "anonymous_student";
        }

        AssessmentSession diagSession = sessionRepository.findById(req.getDiagnosticSessionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Diagnostic session not found: " + req.getDiagnosticSessionId()));

        if (diagSession.getUserId() != null && !diagSession.getUserId().equalsIgnoreCase(effectiveUserId)
                && !"anonymous_student".equals(effectiveUserId)) {
            throw new SecurityException("Unauthorized access to diagnostic session: User does not own session "
                    + req.getDiagnosticSessionId());
        }

        // Identify concepts evaluated in diagnostic that are UNCERTAIN or WEAK for THIS
        // SUBJECT ONLY
        String targetSubj = req.getSubjectName() != null && !req.getSubjectName().isBlank()
                ? req.getSubjectName().trim()
                : diagSession.getSubjectName();

        List<ConceptMastery> userConcepts = conceptRepository.findByUserId(effectiveUserId);
        Map<String, ConceptMastery> cmMap = new HashMap<>();
        if (userConcepts != null) {
            for (ConceptMastery cm : userConcepts) {
                String cmSubject = cm.getSubjectName();
                if (cmSubject != null && cmSubject.equalsIgnoreCase(targetSubj)) {
                    if (cm.getTopic() != null)
                        cmMap.put(cm.getTopic().trim().toLowerCase(), cm);
                    if (cm.getConceptName() != null)
                        cmMap.put(cm.getConceptName().trim().toLowerCase(), cm);
                }
            }
        }

        List<String> targetConcepts = new ArrayList<>();
        Set<String> diagTopics = new LinkedHashSet<>();

        // Extract concepts evaluated in current diagnostic session from user answers
        if (diagSession.getUserAnswers() != null && !diagSession.getUserAnswers().isEmpty()) {
            for (AssessmentResult.UserAnswer ans : diagSession.getUserAnswers()) {
                if (ans.getTopic() != null && !ans.getTopic().isBlank()
                        && RecommendationService.isConceptValidForSubject(targetSubj, ans.getTopic().trim())) {
                    diagTopics.add(ans.getTopic().trim());
                }
            }
        }

        // Supplement/fallback with QuizQuestionRepository lookup for session
        // questionIds
        if (diagTopics.isEmpty() && diagSession.getQuestionIds() != null && !diagSession.getQuestionIds().isEmpty()) {
            List<QuizQuestion> diagQuestions = quizQuestionRepository.findAllById(diagSession.getQuestionIds());
            for (QuizQuestion q : diagQuestions) {
                if (q.getConcept() != null && !q.getConcept().isBlank()
                        && RecommendationService.isConceptValidForSubject(targetSubj, q.getConcept().trim())) {
                    diagTopics.add(q.getConcept().trim());
                }
            }
        }

        // Fallback to subject blueprint if session had no valid topic records
        if (diagTopics.isEmpty()) {
            diagTopics.addAll(RecommendationService.getSubjectBlueprintConcepts(targetSubj));
        }

        // Prioritize: UNCERTAIN first, then WEAK. Exclude STRONG concepts!
        List<String> uncertainConcepts = new ArrayList<>();
        List<String> weakConcepts = new ArrayList<>();

        for (String top : diagTopics) {
            if (!RecommendationService.isConceptValidForSubject(targetSubj, top))
                continue;
            ConceptMastery cm = cmMap.get(top.toLowerCase());
            if (cm == null || cm.getStatus() == ConceptMastery.ConceptStatus.UNCERTAIN
                    || cm.getStatus() == ConceptMastery.ConceptStatus.UNASSESSED) {
                uncertainConcepts.add(top);
            } else if (cm.getStatus() == ConceptMastery.ConceptStatus.WEAK) {
                weakConcepts.add(top);
            }
        }

        targetConcepts.addAll(uncertainConcepts);
        targetConcepts.addAll(weakConcepts);

        if (targetConcepts.isEmpty()) {
            AdaptiveSession completedSession = new AdaptiveSession(null, req.getDiagnosticSessionId(), effectiveUserId,
                    diagSession.getStudentProfileId(), req.getSubjectCode(), diagSession.getSubjectName(),
                    targetConcepts);
            completedSession.setStatus(AdaptiveSession.Status.COMPLETED);
            adaptiveSessionRepository.save(completedSession);
            return new AdaptiveAssessmentDTOs.AdaptiveStartResponse(completedSession.getId(), req.getSubjectCode(),
                    targetConcepts, 15, 0, true);
        }

        String subjectName = req.getSubjectName() != null && !req.getSubjectName().isBlank()
                ? req.getSubjectName().trim()
                : diagSession.getSubjectName();

        // Build Stage 2 10-question adaptive blueprint
        List<QuizGenerationService.QuestionBlueprintSpec> blueprint = buildAdaptiveBlueprint(subjectName,
                effectiveUserId);

        Map<String, Object> genContext = new HashMap<>();
        genContext.put("adaptiveSummary", "Stage 2 10-question adaptive assessment batch");
        List<QuizQuestion> generatedBatch = quizGenerationService.generateBatchDiagnosticQuestionsViaGroq(subjectName,
                blueprint, genContext);

        targetConcepts.clear();
        List<String> questionIds = new ArrayList<>();
        List<String> fingerprints = new ArrayList<>();
        for (QuizQuestion q : generatedBatch) {
            questionIds.add(q.getId());
            fingerprints.add(q.getQuestionFingerprint() != null ? q.getQuestionFingerprint() : q.getQuestionText());
            if (!targetConcepts.contains(q.getConcept())) {
                targetConcepts.add(q.getConcept());
            }
        }

        AdaptiveSession session = new AdaptiveSession(null, req.getDiagnosticSessionId(), effectiveUserId,
                diagSession.getStudentProfileId(), req.getSubjectCode(), subjectName, targetConcepts);
        session.setUsedQuestionIds(questionIds);
        session.setUsedQuestionFingerprints(fingerprints);
        session.setMaxQuestions(10);
        session.setQuestionCount(0);
        session.setCurrentQuestionId(questionIds.isEmpty() ? null : questionIds.get(0));
        session.setActiveQuestionSubmitted(false);
        session.setStatus(AdaptiveSession.Status.IN_PROGRESS);

        AdaptiveSession saved = adaptiveSessionRepository.save(session);

        return new AdaptiveAssessmentDTOs.AdaptiveStartResponse(saved.getId(), req.getSubjectCode(), targetConcepts, 10,
                targetConcepts.size(), false);
    }

    public AdaptiveAssessmentDTOs.AdaptiveNextResponse getAdaptiveNextQuestion(
            AdaptiveAssessmentDTOs.AdaptiveNextRequest req, String authenticatedUserId) {
        if (req == null || req.getAdaptiveSessionId() == null || req.getAdaptiveSessionId().isBlank()) {
            throw new IllegalArgumentException("adaptiveSessionId is required");
        }

        Object lock = sessionLocks.computeIfAbsent(req.getAdaptiveSessionId(), k -> new Object());
        synchronized (lock) {
            AdaptiveSession session = adaptiveSessionRepository.findById(req.getAdaptiveSessionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Adaptive session not found: " + req.getAdaptiveSessionId()));

            if (authenticatedUserId != null && !authenticatedUserId.isBlank()
                    && !"anonymousUser".equals(authenticatedUserId)
                    && session.getUserId() != null && !session.getUserId().equalsIgnoreCase(authenticatedUserId)
                    && !"anonymous_student".equals(authenticatedUserId)) {
                throw new SecurityException("Unauthorized session access: User does not own adaptive session "
                        + req.getAdaptiveSessionId());
            }

            int count = session.getQuestionCount();
            List<String> qIds = session.getUsedQuestionIds();

            System.out.println("[SESSION DEBUG BEFORE getAdaptiveNextQuestion] sessionId=" + session.getId()
                    + ", currentQuestionIndex=" + count + ", completedCount=" + count + ", isComplete="
                    + (session.getStatus() == AdaptiveSession.Status.COMPLETED));

            if (session.getStatus() != AdaptiveSession.Status.IN_PROGRESS || qIds == null || count >= qIds.size()
                    || count >= 10) {
                session.setStatus(AdaptiveSession.Status.COMPLETED);
                adaptiveSessionRepository.save(session);
                return new AdaptiveAssessmentDTOs.AdaptiveNextResponse(session.getId(), true, null, 10, 10,
                        session.getCurrentConcept(),
                        session.getCurrentDifficulty() != null ? session.getCurrentDifficulty().name() : "MEDIUM");
            }

            String activeQId = qIds.get(count);
            QuizQuestion newQuestion = quizQuestionRepository.findById(activeQId)
                    .orElseThrow(
                            () -> new IllegalStateException("Question ID " + activeQId + " not found in database"));

            session.setCurrentConcept(newQuestion.getConcept());
            session.setCurrentDifficulty(newQuestion.getDifficulty());
            session.setCurrentQuestionId(newQuestion.getId());
            session.setActiveQuestionSubmitted(false);
            adaptiveSessionRepository.save(session);

            int overallQNum = count + 1;
            System.out.println("[SESSION DEBUG AFTER getAdaptiveNextQuestion] sessionId=" + session.getId()
                    + ", responseQuestionNumber=" + overallQNum + ", responseQuestionId=" + newQuestion.getId()
                    + ", isComplete=false");

            AdaptiveAssessmentDTOs.QuestionItemDTO dto = new AdaptiveAssessmentDTOs.QuestionItemDTO(
                    newQuestion.getId(), newQuestion.getSubject(), newQuestion.getConcept(), newQuestion.getDifficulty().name(),
                    newQuestion.getQuestionText(), newQuestion.getOptions(), newQuestion.getCorrectOptionIndex(), null
            );
            System.out.println("[QUESTION DELIVERY DEBUG] [ADAPTIVE] questionId=" + newQuestion.getId() +
                    ", position=" + overallQNum +
                    ", concept=" + newQuestion.getConcept() +
                    ", correctOptionIndex=" + newQuestion.getCorrectOptionIndex() +
                    ", questionText=\"" + newQuestion.getQuestionText() + "\"" +
                    ", explanation=\"" + newQuestion.getConceptualExplanation() + "\"");
            return new AdaptiveAssessmentDTOs.AdaptiveNextResponse(session.getId(), false, dto, overallQNum, 10, newQuestion.getConcept(), newQuestion.getDifficulty().name());
        }
    }

    public AdaptiveAssessmentDTOs.AdaptiveSubmitResponse submitAdaptiveAnswer(AdaptiveAssessmentDTOs.AdaptiveSubmitRequest req, String authenticatedUserId) {
        if (req == null || req.getAdaptiveSessionId() == null || req.getAdaptiveSessionId().isBlank()) {
            throw new IllegalArgumentException("adaptiveSessionId is required");
        }

        Object lock = sessionLocks.computeIfAbsent(req.getAdaptiveSessionId(), k -> new Object());
        synchronized (lock) {
            AdaptiveSession session = adaptiveSessionRepository.findById(req.getAdaptiveSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Adaptive session not found: " + req.getAdaptiveSessionId()));

            if (authenticatedUserId != null && !authenticatedUserId.isBlank() && !"anonymousUser".equals(authenticatedUserId)
                    && session.getUserId() != null && !session.getUserId().equalsIgnoreCase(authenticatedUserId) && !"anonymous_student".equals(authenticatedUserId)) {
                throw new SecurityException("Unauthorized session access: User does not own adaptive session " + req.getAdaptiveSessionId());
            }

            if (session.getStatus() != AdaptiveSession.Status.IN_PROGRESS) {
                throw new IllegalStateException("Adaptive session is already completed: " + req.getAdaptiveSessionId());
            }

            if (session.getCurrentQuestionId() == null || !session.getCurrentQuestionId().equals(req.getQuestionId())) {
                throw new IllegalArgumentException("Submitted question ID " + req.getQuestionId() + " does not match active question " + session.getCurrentQuestionId());
            }

            if (session.isActiveQuestionSubmitted()) {
                throw new IllegalStateException("Question " + req.getQuestionId() + " answer has already been submitted.");
            }

            QuizQuestion question = quizQuestionRepository.findById(req.getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException("Question not found: " + req.getQuestionId()));

            boolean isCorrect = (req.getSelectedOption() == question.getCorrectOptionIndex());

            // Mark current active question as submitted and increment question count
            session.setActiveQuestionSubmitted(true);
            session.setQuestionCount(session.getQuestionCount() + 1);

            // Update single concept mastery using authoritative KnowledgeService
            knowledgeService.updateSingleConceptMastery(
                    session.getUserId(),
                    session.getStudentProfileId(),
                    session.getSubjectCode(),
                    session.getSubjectName(),
                    question.getConcept(),
                    isCorrect
            );

            try {
                studentService.syncConceptMasteryWithProfile(session.getUserId(), session.getSubjectName());
            } catch (Exception ex) {
                System.err.println("Failed profile mastery sync in adaptive answer: " + ex.getMessage());
            }

            // Fetch updated ConceptMastery state
            Optional<ConceptMastery> updatedCmOpt = conceptRepository.findByUserIdAndSubjectCodeAndTopicAndConceptName(
                    session.getUserId(), session.getSubjectCode(), question.getConcept(), question.getConcept()
            );

            String updatedStatus = "UNCERTAIN";
            double updatedConf = 25.0;
            if (updatedCmOpt.isPresent()) {
                ConceptMastery cm = updatedCmOpt.get();
                updatedStatus = cm.getStatus() != null ? cm.getStatus().name() : "UNCERTAIN";
                updatedConf = cm.getConfidenceScore();
            }

            // Difficulty selection adjustment rule:
            QuizQuestion.Difficulty nextDiff = session.getCurrentDifficulty() != null ? session.getCurrentDifficulty() : QuizQuestion.Difficulty.MEDIUM;
            if (isCorrect) {
                if (req.getResponseTimeSeconds() < 15.0 && nextDiff != QuizQuestion.Difficulty.HARD) {
                    nextDiff = nextDiff == QuizQuestion.Difficulty.EASY ? QuizQuestion.Difficulty.MEDIUM : QuizQuestion.Difficulty.HARD;
                }
            } else {
                if (nextDiff != QuizQuestion.Difficulty.EASY) {
                    nextDiff = nextDiff == QuizQuestion.Difficulty.HARD ? QuizQuestion.Difficulty.MEDIUM : QuizQuestion.Difficulty.EASY;
                }
            }
            session.setCurrentDifficulty(nextDiff);

            boolean completed = session.getQuestionCount() >= session.getMaxQuestions();
            if (completed) {
                session.setStatus(AdaptiveSession.Status.COMPLETED);
            }

            adaptiveSessionRepository.save(session);

            System.out.println("[SUBMIT BACKEND DEBUG] [ADAPTIVE] sessionId=" + session.getId() +
                    ", questionId=" + question.getId() +
                    ", position=" + session.getQuestionCount() +
                    ", concept=" + question.getConcept() +
                    ", correctOptionIndex=" + question.getCorrectOptionIndex() +
                    ", questionText=\"" + question.getQuestionText() + "\"" +
                    ", conceptualExplanation=\"" + question.getConceptualExplanation() + "\"");

            return new AdaptiveAssessmentDTOs.AdaptiveSubmitResponse(
                    session.getId(),
                    isCorrect,
                    question.getCorrectOptionIndex(),
                    question.getConceptualExplanation(),
                    completed,
                    updatedStatus,
                    updatedConf,
                    nextDiff.name()
            );
        }
    }

    // =========================================================================
    // PHASE 6: GROQ INITIAL DIAGNOSTIC INCREMENTAL / ON-DEMAND METHODS
    // =========================================================================

    private QuizQuestion ensureQuestionGenerated(String sessionId, int targetIndex) {
        return ensureQuestionGenerated(sessionId, targetIndex, false);
    }

    private QuizQuestion ensureQuestionGenerated(String sessionId, int targetIndex, boolean isPrefetchCall) {
        AssessmentSession sessionCheck = sessionRepository.findById(sessionId).orElse(null);
        int totalSessionQuestions = sessionCheck != null && sessionCheck.getTotalQuestions() > 0 ? sessionCheck.getTotalQuestions() : 25;

        if (targetIndex < 0 || targetIndex >= totalSessionQuestions) return null;

        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());

        // 1. Short lock section: Check if target question is already generated
        String existingQId = null;
        String subjectName = null;
        String userId = null;
        List<String> fingerprintsCopy = new ArrayList<>();
        QuizGenerationService.QuestionBlueprintSpec spec = null;

        synchronized (lock) {
            AssessmentSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

            List<String> qIds = session.getQuestionIds() != null ? session.getQuestionIds() : new ArrayList<>();
            if (targetIndex < qIds.size()) {
                existingQId = qIds.get(targetIndex);
            } else {
                subjectName = session.getSubjectName();
                userId = session.getUserId();
                List<QuizGenerationService.QuestionBlueprintSpec> blueprint = buildAdaptiveBlueprint(subjectName, userId, totalSessionQuestions);
                if (targetIndex < blueprint.size()) {
                    spec = blueprint.get(targetIndex);
                }
                if (session.getUsedQuestionFingerprints() != null) {
                    fingerprintsCopy = new ArrayList<>(session.getUsedQuestionFingerprints());
                }
            }
        }

        if (existingQId != null) {
            if (!isPrefetchCall && targetIndex + 1 < totalSessionQuestions) {
                triggerPrefetchIfNeeded(sessionId, targetIndex + 1);
            }
            return quizQuestionRepository.findById(existingQId).orElse(null);
        }

        if (spec == null) {
            return null;
        }

        String generationKey = sessionId + ":" + targetIndex;
        java.util.concurrent.CompletableFuture<QuizQuestion> myFuture = new java.util.concurrent.CompletableFuture<>();
        java.util.concurrent.CompletableFuture<QuizQuestion> existingFuture = inFlightGenerations.putIfAbsent(generationKey, myFuture);

        if (existingFuture != null) {
            System.out.println("[IN-FLIGHT COALESCE] Reusing active generation for session " + sessionId + " question index " + targetIndex);
            try {
                QuizQuestion sharedQ = existingFuture.join();
                if (!isPrefetchCall && targetIndex + 1 < totalSessionQuestions) {
                    triggerPrefetchIfNeeded(sessionId, targetIndex + 1);
                }
                return sharedQ;
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException(cause);
            }
        }

        try {
            // 2. Unlocked Groq HTTP generation (No sessionLocks monitor held during AI network call)
            Map<String, Object> genContext = new HashMap<>();
            genContext.put("adaptiveSummary", "Baseline " + totalSessionQuestions + "-question initial assessment");
            genContext.put("excludeQuestions", fingerprintsCopy);
            genContext.put("purpose", "DIAGNOSTIC_QUESTION_" + (targetIndex + 1));

            QuizQuestion generatedQ = quizGenerationService.generateSingleDiagnosticQuestion(subjectName, spec, genContext, targetIndex + 1, totalSessionQuestions);
            if (generatedQ == null) {
                myFuture.complete(null);
                return null;
            }
            generatedQ = quizQuestionRepository.save(generatedQ);

            // 3. Short lock section: Atomic persistence & state update
            QuizQuestion finalQuestion = generatedQ;
            boolean shouldPrefetch = false;
            synchronized (lock) {
                AssessmentSession session = sessionRepository.findById(sessionId)
                        .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

                List<String> qIds = session.getQuestionIds() != null ? new ArrayList<>(session.getQuestionIds()) : new ArrayList<>();

                if (targetIndex < qIds.size()) {
                    String raceQId = qIds.get(targetIndex);
                    finalQuestion = quizQuestionRepository.findById(raceQId).orElse(generatedQ);
                } else {
                    qIds.add(generatedQ.getId());
                    List<String> fingerprints = session.getUsedQuestionFingerprints() != null ? new ArrayList<>(session.getUsedQuestionFingerprints()) : new ArrayList<>();
                    String fp = generatedQ.getQuestionFingerprint() != null ? generatedQ.getQuestionFingerprint() : generatedQ.getQuestionText();
                    if (fp != null && !fingerprints.contains(fp)) {
                        fingerprints.add(fp);
                    }
                    session.setQuestionIds(qIds);
                    session.setUsedQuestionFingerprints(fingerprints);
                    sessionRepository.save(session);

                    if (!isPrefetchCall && targetIndex + 1 < totalSessionQuestions && qIds.size() <= targetIndex + 1) {
                        shouldPrefetch = true;
                    }
                }
            }

            myFuture.complete(finalQuestion);

            if (shouldPrefetch) {
                prefetchNextQuestionAsync(sessionId, targetIndex + 1);
            }

            return finalQuestion;
        } catch (Throwable t) {
            myFuture.completeExceptionally(t);
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        } finally {
            inFlightGenerations.remove(generationKey);
        }
    }

    private void triggerPrefetchIfNeeded(String sessionId, int nextIndex) {
        AssessmentSession sessionCheck = sessionRepository.findById(sessionId).orElse(null);
        int totalSessionQuestions = sessionCheck != null && sessionCheck.getTotalQuestions() > 0 ? sessionCheck.getTotalQuestions() : 25;
        if (nextIndex < 0 || nextIndex >= totalSessionQuestions) return;
        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());
        boolean needsPrefetch = false;
        synchronized (lock) {
            AssessmentSession session = sessionRepository.findById(sessionId).orElse(null);
            if (session != null) {
                List<String> qIds = session.getQuestionIds();
                if (qIds == null || nextIndex >= qIds.size()) {
                    needsPrefetch = true;
                }
            }
        }
        if (needsPrefetch) {
            prefetchNextQuestionAsync(sessionId, nextIndex);
        }
    }

    private void prefetchNextQuestionAsync(String sessionId, int targetIndex) {
        AssessmentSession sessionCheck = sessionRepository.findById(sessionId).orElse(null);
        int totalSessionQuestions = sessionCheck != null && sessionCheck.getTotalQuestions() > 0 ? sessionCheck.getTotalQuestions() : 25;
        if (targetIndex < 0 || targetIndex >= totalSessionQuestions) return;
        if (!activePrefetchSessions.add(sessionId)) {
            System.out.println("[PREFETCH SKIPPED] Session " + sessionId + " already has a prefetch request in flight.");
            return;
        }
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                ensureQuestionGenerated(sessionId, targetIndex, true);
            } catch (Exception ex) {
                System.err.println("[AssessmentService] Prefetch for question index " + targetIndex + " failed silently: " + ex.getMessage());
            } finally {
                activePrefetchSessions.remove(sessionId);
            }
        });
    }

    public AdaptiveAssessmentDTOs.AdaptiveNextResponse getInitialNextQuestion(AdaptiveAssessmentDTOs.AdaptiveNextRequest req, String authenticatedUserId) {
        if (req == null || req.getAdaptiveSessionId() == null || req.getAdaptiveSessionId().isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        String sessionId = req.getAdaptiveSessionId();
        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());
        
        int count;
        int totalQuestions;
        synchronized (lock) {
            AssessmentSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Initial assessment session not found: " + sessionId));

            String effectiveUserId = authenticatedUserId != null && !authenticatedUserId.isBlank() && !"anonymousUser".equals(authenticatedUserId)
                    ? authenticatedUserId : session.getUserId();

            if (session.getUserId() != null && !session.getUserId().equalsIgnoreCase(effectiveUserId) && !"anonymous_student".equals(effectiveUserId)) {
                throw new SecurityException("Unauthorized session access: User does not own assessment session " + sessionId);
            }

            count = session.getQuestionCount();
            totalQuestions = session.getTotalQuestions() > 0 ? session.getTotalQuestions() : 25;

            if (session.getStatus() != AssessmentSession.Status.IN_PROGRESS || count >= totalQuestions) {
                AdaptiveAssessmentDTOs.AdaptiveNextResponse nextResp = new AdaptiveAssessmentDTOs.AdaptiveNextResponse(session.getId(), true, null, totalQuestions, totalQuestions, "Complete", "MEDIUM");
                Optional<AssessmentResult> latestOpt = resultRepository.findTopByUserIdOrderByCreatedAtDesc(effectiveUserId);
                latestOpt.ifPresent(ar -> nextResp.setResult(new AssessmentResultResponse(ar)));
                return nextResp;
            }
        }

        // Unlocked AI generation / In-flight coalesce join (NO session lock held)
        QuizQuestion newQuestion = ensureQuestionGenerated(sessionId, count);
        if (newQuestion == null) {
            throw new IllegalStateException("Question at index " + count + " not found or could not be generated");
        }

        synchronized (lock) {
            AssessmentSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException("Initial assessment session not found: " + sessionId));

            session.setCurrentQuestionId(newQuestion.getId());
            session.setActiveQuestionSubmitted(false);
            sessionRepository.save(session);

            int currentQNum = count + 1;
            AdaptiveAssessmentDTOs.QuestionItemDTO dto = new AdaptiveAssessmentDTOs.QuestionItemDTO(
                    newQuestion.getId(), newQuestion.getSubject(), newQuestion.getConcept(), newQuestion.getDifficulty().name(),
                    newQuestion.getQuestionText(), newQuestion.getOptions(), newQuestion.getCorrectOptionIndex(), null
            );
            System.out.println("[QUESTION DELIVERY DEBUG] [INITIAL] questionId=" + newQuestion.getId() +
                    ", position=" + currentQNum +
                    ", concept=" + newQuestion.getConcept() +
                    ", correctOptionIndex=" + newQuestion.getCorrectOptionIndex() +
                    ", questionText=\"" + newQuestion.getQuestionText() + "\"" +
                    ", explanation=\"" + newQuestion.getConceptualExplanation() + "\"");
            return new AdaptiveAssessmentDTOs.AdaptiveNextResponse(session.getId(), false, dto, currentQNum, totalQuestions, newQuestion.getConcept(), newQuestion.getDifficulty().name());
        }
    }

    public AdaptiveAssessmentDTOs.AdaptiveSubmitResponse submitInitialAnswer(AdaptiveAssessmentDTOs.AdaptiveSubmitRequest req, String authenticatedUserId) {
        if (req == null || req.getAdaptiveSessionId() == null || req.getAdaptiveSessionId().isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        Object lock = sessionLocks.computeIfAbsent(req.getAdaptiveSessionId(), k -> new Object());
        synchronized (lock) {
            AssessmentSession session = sessionRepository.findById(req.getAdaptiveSessionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Initial assessment session not found: " + req.getAdaptiveSessionId()));

            String effectiveUserId = authenticatedUserId != null && !authenticatedUserId.isBlank()
                    && !"anonymousUser".equals(authenticatedUserId)
                            ? authenticatedUserId
                            : session.getUserId();

            if (session.getUserId() != null && !session.getUserId().equalsIgnoreCase(effectiveUserId)
                    && !"anonymous_student".equals(effectiveUserId)) {
                throw new SecurityException(
                        "Unauthorized session access: User does not own assessment session "
                                + req.getAdaptiveSessionId());
            }

            if (session.getStatus() != AssessmentSession.Status.IN_PROGRESS) {
                throw new IllegalStateException("Initial assessment session is already completed: " + req.getAdaptiveSessionId());
            }

            if (session.getCurrentQuestionId() == null || !session.getCurrentQuestionId().equals(req.getQuestionId())) {
                throw new IllegalArgumentException("Submitted question ID " + req.getQuestionId() + " does not match active question " + session.getCurrentQuestionId());
            }

            if (session.isActiveQuestionSubmitted()) {
                throw new IllegalStateException("Question " + req.getQuestionId() + " answer has already been submitted.");
            }

            System.out.println("[SESSION DEBUG BEFORE submitInitialAnswer] sessionId=" + session.getId() + ", currentQuestionIndex=" + session.getQuestionCount() + ", completedCount=" + session.getQuestionCount() + ", isComplete=" + (session.getStatus() == AssessmentSession.Status.COMPLETED));

            QuizQuestion question = quizQuestionRepository.findById(req.getQuestionId())
                    .orElseThrow(() -> new IllegalArgumentException("Question not found: " + req.getQuestionId()));

            boolean isCorrect = (req.getSelectedOption() == question.getCorrectOptionIndex());

            // Mark active question as submitted and increment question count
            session.setActiveQuestionSubmitted(true);
            session.setQuestionCount(session.getQuestionCount() + 1);

            // Update concept mastery via KnowledgeService (Source of Truth)
            knowledgeService.updateSingleConceptMastery(
                    effectiveUserId,
                    session.getStudentProfileId(),
                    session.getSubjectCode(),
                    session.getSubjectName(),
                    question.getConcept(),
                    isCorrect
            );

            // Record UserAnswer in session history
            if (session.getUserAnswers() == null) session.setUserAnswers(new ArrayList<>());
            session.getUserAnswers().add(new AssessmentResult.UserAnswer(question.getId(), question.getConcept(), req.getSelectedOption(), isCorrect, isCorrect ? 2 : 0));

            int totalQuestions = session.getTotalQuestions() > 0 ? session.getTotalQuestions() : 25;
            boolean completed = session.getQuestionCount() >= totalQuestions;

            AssessmentResultResponse resultResponse = null;
            if (completed) {
                session.setStatus(AssessmentSession.Status.COMPLETED);
                session.setEndTime(LocalDateTime.now());

                // Build final AssessmentResult
                int correctCount = (int) session.getUserAnswers().stream().filter(AssessmentResult.UserAnswer::isCorrect).count();
                int totalMarks = totalQuestions * 2;
                int score = correctCount * 2;
                double percentage = Math.round((correctCount * 100.0 / totalQuestions) * 10.0) / 10.0;

                AssessmentResult result = new AssessmentResult();
                result.setSessionId(session.getId());
                result.setUserId(effectiveUserId);
                result.setStudentProfileId(session.getStudentProfileId());
                result.setBranch(session.getBranch());
                result.setSemester(session.getSemester());
                result.setSubjectCode(session.getSubjectCode());
                result.setSubjectName(session.getSubjectName());
                result.setTotalQuestions(totalQuestions);
                result.setCorrectAnswers(correctCount);
                result.setIncorrectAnswers(totalQuestions - correctCount);
                result.setSkippedQuestions(0);
                result.setScore(score);
                result.setTotalMarks(totalMarks);
                result.setPercentage(percentage);
                result.setAccuracy(percentage);
                result.setTimeTakenSeconds(60);
                result.setMasteryLevel(percentage >= 85 ? "MASTER" : percentage >= 70 ? "PROFICIENT" : percentage >= 50 ? "INTERMEDIATE" : "BEGINNER");
                result.setUserAnswers(session.getUserAnswers());
                result.setCreatedAt(LocalDateTime.now());

                AssessmentResult savedResult = resultRepository.save(result);
                resultResponse = new AssessmentResultResponse(savedResult);

                try {
                    StudentProfile prof = studentService.findOrCreateProfile(effectiveUserId);
                    if (prof != null) {
                        Map<String, Double> cmMap = prof.getConceptMastery();
                        if (cmMap == null) cmMap = new HashMap<>();
                        if (session.getSubjectName() != null) {
                            cmMap.put(session.getSubjectName(), percentage);
                        }
                        prof.setConceptMastery(cmMap);
                        profileRepository.save(prof);
                    }
                } catch (Exception ex) {
                    System.err.println("Failed updating profile concept mastery: " + ex.getMessage());
                }

                try {
                    knowledgeService.syncKnowledgeProfileSummary(effectiveUserId, session.getSubjectName());
                    studentService.syncConceptMasteryWithProfile(effectiveUserId, session.getSubjectName());
                } catch (Exception ex) {
                    System.err.println("Failed knowledge profile processing: " + ex.getMessage());
                }
            }

            sessionRepository.save(session);

            Optional<ConceptMastery> updatedCmOpt = conceptRepository.findByUserIdAndSubjectCodeAndTopicAndConceptName(
                    effectiveUserId, session.getSubjectCode(), question.getConcept(), question.getConcept()
            );

            String updatedStatus = updatedCmOpt.isPresent() && updatedCmOpt.get().getStatus() != null ? updatedCmOpt.get().getStatus().name() : "UNCERTAIN";
            double updatedConf = updatedCmOpt.isPresent() ? updatedCmOpt.get().getConfidenceScore() : 25.0;

            System.out.println("[SESSION DEBUG AFTER submitInitialAnswer] sessionId=" + session.getId() + ", currentQuestionIndex=" + session.getQuestionCount() + ", completedCount=" + session.getQuestionCount() + ", isComplete=" + completed);

            System.out.println("[SUBMIT BACKEND DEBUG] [INITIAL] sessionId=" + session.getId() +
                    ", questionId=" + question.getId() +
                    ", position=" + session.getQuestionCount() +
                    ", concept=" + question.getConcept() +
                    ", correctOptionIndex=" + question.getCorrectOptionIndex() +
                    ", questionText=\"" + question.getQuestionText() + "\"" +
                    ", conceptualExplanation=\"" + question.getConceptualExplanation() + "\"");

            AdaptiveAssessmentDTOs.AdaptiveSubmitResponse submitResponse = new AdaptiveAssessmentDTOs.AdaptiveSubmitResponse(
                    session.getId(),
                    isCorrect,
                    question.getCorrectOptionIndex(),
                    question.getConceptualExplanation(),
                    completed,
                    updatedStatus,
                    updatedConf,
                    "MEDIUM"
            );
            if (completed && resultResponse != null) {
                submitResponse.setResult(resultResponse);
            }
            return submitResponse;
        }
    }

    public Map<String, Object> abandonSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId cannot be null or empty");
        }
        Optional<AssessmentSession> sessionOpt = sessionRepository.findById(sessionId.trim());
        if (sessionOpt.isPresent()) {
            AssessmentSession session = sessionOpt.get();
            session.setStatus(AssessmentSession.Status.ABANDONED);
            sessionRepository.save(session);
            return Map.of("status", "SESSION_ABANDONED", "sessionId", sessionId);
        }
        return Map.of("status", "SESSION_NOT_FOUND", "sessionId", sessionId);
    }

    public Map<String, Object> abandonAdaptiveSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("sessionId cannot be null or empty");
        }
        Optional<AdaptiveSession> sessionOpt = adaptiveSessionRepository.findById(sessionId.trim());
        if (sessionOpt.isPresent()) {
            AdaptiveSession session = sessionOpt.get();
            session.setStatus(AdaptiveSession.Status.COMPLETED);
            adaptiveSessionRepository.save(session);
            return Map.of("status", "SESSION_ABANDONED", "sessionId", sessionId);
        }
        return Map.of("status", "SESSION_NOT_FOUND", "sessionId", sessionId);
    }
}
