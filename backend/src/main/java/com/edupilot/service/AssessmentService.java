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

    @PostConstruct
    public void initDefaultQuestionBank() {
        if (questionRepository.count() == 0) {
            List<AssessmentQuestion> qList = new ArrayList<>();

            // CS301: Data Structures & Algorithms
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "CS301", "Data Structures & Algorithms", "Binary Search Trees", "What is the worst-case time complexity of searching in an unbalanced Binary Search Tree?", List.of("O(1)", "O(log N)", "O(N)", "O(N log N)"), 2, "In an unbalanced BST (skewed tree), search degrades to linear scan O(N).", AssessmentQuestion.Difficulty.MEDIUM, 2, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "CS301", "Data Structures & Algorithms", "Sorting Algorithms", "Which of the following sorting algorithms offers stable O(N log N) time complexity in worst case?", List.of("Quick Sort", "Merge Sort", "Heap Sort", "Selection Sort"), 1, "Merge Sort is guaranteed O(N log N) worst-case time and is stable.", AssessmentQuestion.Difficulty.EASY, 2, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "CS301", "Data Structures & Algorithms", "Graph Theory", "Which graph traversal algorithm uses a Queue data structure?", List.of("Depth First Search (DFS)", "Breadth First Search (BFS)", "Dijkstra Algorithm", "Kruskal Algorithm"), 1, "BFS uses a Queue (FIFO) to visit node neighbors level by level.", AssessmentQuestion.Difficulty.EASY, 2, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "CS301", "Data Structures & Algorithms", "Dynamic Programming", "What key property makes a problem suitable for Dynamic Programming?", List.of("Greedy Choice Property", "Overlapping Subproblems & Optimal Substructure", "Divide & Conquer only", "Randomized state space"), 1, "Dynamic Programming optimizes problems with overlapping subproblems and optimal substructure.", AssessmentQuestion.Difficulty.HARD, 2, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "CS301", "Data Structures & Algorithms", "Hash Tables", "In hash tables, what technique resolves collisions by storing multiple entries in linked nodes at the same bucket index?", List.of("Open Addressing", "Separate Chaining", "Linear Probing", "Quadratic Probing"), 1, "Separate Chaining maintains a linked list of entries at each bucket index.", AssessmentQuestion.Difficulty.MEDIUM, 2, "MCQ", true));

            // CS302: Database Management Systems
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "CS302", "Database Management Systems", "Relational Algebra", "Which SQL command is used to ensure ACID compliance transaction atomicity commit?", List.of("COMMIT", "ROLLBACK", "SAVEPOINT", "GRANT"), 0, "COMMIT saves all transaction changes permanently to the database log.", AssessmentQuestion.Difficulty.EASY, 2, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "CS302", "Database Management Systems", "Normalization", "A relation is in 3NF if it is in 2NF and has no:", List.of("Partial dependencies", "Transitive dependencies", "Multi-valued dependencies", "Join dependencies"), 1, "3NF requires eliminating transitive functional dependencies.", AssessmentQuestion.Difficulty.MEDIUM, 2, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "CS302", "Database Management Systems", "Indexing", "Which indexing structure is commonly used by RDBMS for efficient B-tree disk block retrieval?", List.of("B+ Tree", "Red-Black Tree", "AVL Tree", "Trie"), 0, "B+ Trees store all records in leaf nodes, making range queries and disk block retrieval optimal.", AssessmentQuestion.Difficulty.HARD, 2, "MCQ", true));

            // CS601: Artificial Intelligence & Machine Learning
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 6, "CS601", "Artificial Intelligence & Machine Learning", "Neural Networks", "Which activation function suffers from the vanishing gradient problem in deep networks?", List.of("ReLU", "Leaky ReLU", "Sigmoid", "ELU"), 2, "Sigmoid function squashes inputs into (0,1), causing derivatives to vanish near zero during backpropagation.", AssessmentQuestion.Difficulty.MEDIUM, 2, "MCQ", true));
            qList.add(new AssessmentQuestion(null, "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 6, "CS601", "Artificial Intelligence & Machine Learning", "Supervised Learning", "Which evaluation metric is ideal for highly imbalanced classification datasets?", List.of("Accuracy", "F1-Score / PR-AUC", "Mean Squared Error", "R-squared"), 1, "F1-Score (harmonic mean of Precision & Recall) evaluates performance under class imbalance better than Accuracy.", AssessmentQuestion.Difficulty.MEDIUM, 2, "MCQ", true));

            questionRepository.saveAll(qList);
        }

        if (questionRepository.findBySubjectCodeAndIsActiveTrue("DS-LL-PRE01").isEmpty()) {
            List<AssessmentQuestion> list = new ArrayList<>();
            // Topic 1
            list.add(new AssessmentQuestion("DS-LL-PRE01-D1.1", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Introduction / Node Structure", "What is a linked list?", List.of("A collection of elements stored only in consecutive memory locations", "A collection of nodes connected using links/references", "A fixed-size collection of elements", "A collection containing only integer values"), 1, "A linked list is a dynamic collection of nodes connected by links/references.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D1.2", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Introduction / Node Structure", "Which two components are commonly present in a node of a singly linked list?", List.of("Data and next/reference", "Index and size", "Key and array", "Head and tail"), 0, "A singly linked list node contains data and a reference to the next node.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D1.3", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Introduction / Node Structure", "What does the head reference normally point to in a singly linked list?", List.of("The last node", "The first node", "The middle node", "The number of nodes"), 1, "The head reference points to the first node of a linked list.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D1.4", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Introduction / Node Structure", "Consider: 10 → 20 → 30 → NULL. What does NULL indicate?", List.of("The list contains an error", "The next node is 0", "There is no next node", "The list contains three nodes"), 2, "NULL indicates the end of the linked list (no next node).", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D1.5", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Introduction / Node Structure", "What is one major advantage of a linked list over a fixed-size array?", List.of("Constant-time random access", "Dynamic size and flexible insertion/deletion", "Contiguous memory allocation", "No additional memory for links"), 1, "Linked lists offer dynamic sizing and efficient insertions/deletions.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));

            // Topic 2
            list.add(new AssessmentQuestion("DS-LL-PRE01-D2.1", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Node Creation", "If a linked list is initially empty, what should the head reference normally contain?", List.of("1", "0", "NULL", "The address of the last node"), 2, "An empty linked list is represented by a head reference containing NULL.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D2.2", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Node Creation", "Suppose a new node is created: Node newNode = new Node(10). What does 10 represent?", List.of("Node address", "Data stored in the node", "Position of the node", "Number of links"), 1, "The parameter 10 represents the data stored inside the node.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D2.3", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Node Creation", "If a newly created node is currently the only node in a singly linked list, its next reference should normally be:", List.of("The address of itself", "The address of head", "NULL", "The address of the previous node"), 2, "A single node's next pointer should be NULL as there are no subsequent nodes.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D2.4", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Node Creation", "To insert a newly created node at the beginning of a singly linked list, which statement is conceptually correct?", List.of("newNode.next = head", "head.next = head", "newNode = NULL", "head = NULL"), 0, "Point the new node's next to the current head.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D2.5", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Node Creation", "After inserting a new node at the beginning, what should happen to head?", List.of("It remains unchanged", "It points to the new node", "It points to the last node", "It becomes NULL"), 1, "The head must be updated to point to the new node.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));

            // Topic 3
            list.add(new AssessmentQuestion("DS-LL-PRE01-D3.1", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Traversal", "What is traversal of a linked list?", List.of("Removing every node", "Visiting/processing nodes sequentially", "Sorting the nodes", "Creating a new list"), 1, "Traversal means visiting each node in the list sequentially.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D3.2", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Traversal", "For a singly linked list, traversal normally begins at:", List.of("NULL", "Tail", "Head", "Middle node"), 2, "Singly linked list traversal starts from the head.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D3.3", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Traversal", "Given: 10 → 20 → 30 → NULL. What is the normal forward traversal output?", List.of("30, 20, 10", "10, 20, 30", "20, 10, 30", "10, 30, 20"), 1, "Normal forward traversal prints elements in order: 10, 20, 30.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D3.4", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Traversal", "Which condition is commonly used to continue traversal until the end of a singly linked list?", List.of("current == head", "current != NULL", "current == NULL", "current > head"), 1, "The loop continues while current is not NULL.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D3.5", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Traversal", "What is the time complexity of traversing all n nodes of a singly linked list?", List.of("O(1)", "O(log n)", "O(n)", "O(n²)"), 2, "Traversal of n nodes takes O(n) linear time.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));

            // Topic 4
            list.add(new AssessmentQuestion("DS-LL-PRE01-D4.1", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Insertion", "What does insertion mean in a linked list?", List.of("Searching for a node", "Adding a new node", "Removing a node", "Traversing the list"), 1, "Insertion adds a new node to the linked list.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D4.2", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Insertion", "Given: 10 → 20 → 30. A node containing 5 is inserted at the beginning. What is the result?", List.of("10 → 20 → 30 → 5", "5 → 10 → 20 → 30", "10 → 5 → 20 → 30", "10 → 20 → 5 → 30"), 1, "Inserting 5 at the front results in 5 pointing to 10.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D4.3", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Insertion", "Given: 10 → 20 → 30. A node containing 25 is inserted between 20 and 30. What is the result?", List.of("25 → 10 → 20 → 30", "10 → 20 → 25 → 30", "10 → 25 → 20 → 30", "10 → 20 → 30 → 25"), 1, "Inserting 25 between 20 and 30 results in 10 → 20 → 25 → 30.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D4.4", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Insertion", "When inserting a node between two nodes in a singly linked list, which references need to be updated?", List.of("Only the new node's data", "The relevant next/reference links", "Only the head", "Only the tail"), 1, "The next pointers of the preceding node and the new node must be updated.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D4.5", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Insertion", "If a node is inserted at the beginning of a non-empty singly linked list, how many existing nodes need their next reference changed?", List.of("Every node", "No existing node", "Only the old head node", "Only the last node"), 1, "No existing node's next reference is changed; only the new node's next pointer points to the old head.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));

            // Topic 5
            list.add(new AssessmentQuestion("DS-LL-PRE01-D5.1", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Deletion", "What does deletion mean in a linked list?", List.of("Adding a node", "Removing a node", "Traversing a node", "Creating a node"), 1, "Deletion removes a node from the list.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D5.2", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Deletion", "Given: 10 → 20 → 30. If node 20 is deleted, what is the resulting list?", List.of("20 → 10 → 30", "10 → 30", "10 → 20", "30 → 20 → 10"), 1, "Deleting 20 yields 10 pointing directly to 30.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D5.3", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Deletion", "When deleting a middle node from a singly linked list, the previous node should generally:", List.of("Point to NULL", "Point to the node after the deleted node", "Become the head", "Also be deleted"), 1, "The previous node's next must be updated to skip the deleted node.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D5.4", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Deletion", "If the only node in a linked list is deleted, what should head become?", List.of("The deleted node", "1", "NULL", "The previous node"), 2, "If the single node is deleted, the head becomes NULL.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));
            list.add(new AssessmentQuestion("DS-LL-PRE01-D5.5", "EduPilot Academy", "B.Tech", "Computer Science & Engineering", 3, "DS-LL-PRE01", "Data Structures", "Deletion", "Consider: 10 → 20 → 30 → 40. If node 30 is deleted, which sequence should remain?", List.of("10 → 20 → 40", "10 → 30 → 40", "20 → 30 → 40", "10 → 20 → 30"), 0, "Deleting 30 yields 10 → 20 → 40.", AssessmentQuestion.Difficulty.MEDIUM, 1, "MCQ", true));

            questionRepository.saveAll(list);
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
        List<QuizGenerationService.QuestionBlueprintSpec> blueprint = new ArrayList<>();
        List<String> blueprintConcepts = RecommendationService.getSubjectBlueprintConcepts(subjectName);

        if (blueprintConcepts == null || blueprintConcepts.isEmpty()) {
            blueprintConcepts = List.of("Core Principles", "Foundational Concepts", "Advanced Applications");
        }

        List<ConceptMastery> userConcepts = null;
        if (userId != null && !userId.isBlank() && !"anonymous_student".equals(userId)) {
            userConcepts = conceptRepository.findByUserId(userId);
        }

        Map<String, ConceptMastery> cmMap = new HashMap<>();
        if (userConcepts != null) {
            for (ConceptMastery cm : userConcepts) {
                String cmSubject = cm.getSubjectName();
                if (cmSubject == null || cmSubject.equalsIgnoreCase(subjectName)) {
                    if (cm.getTopic() != null) cmMap.put(cm.getTopic().trim().toLowerCase(), cm);
                    if (cm.getConceptName() != null) cmMap.put(cm.getConceptName().trim().toLowerCase(), cm);
                }
            }
        }

        List<String> weakConcepts = new ArrayList<>();
        List<String> strongConcepts = new ArrayList<>();
        List<String> neutralConcepts = new ArrayList<>();

        for (String concept : blueprintConcepts) {
            ConceptMastery cm = cmMap.get(concept.toLowerCase());
            if (cm != null) {
                if (cm.getAccuracy() < 50.0 || cm.getStatus() == ConceptMastery.ConceptStatus.WEAK) {
                    weakConcepts.add(concept);
                } else if (cm.getAccuracy() >= 75.0 || cm.getStatus() == ConceptMastery.ConceptStatus.STRONG) {
                    strongConcepts.add(concept);
                } else {
                    neutralConcepts.add(concept);
                }
            } else {
                neutralConcepts.add(concept);
            }
        }

        Collections.shuffle(bucketCritical);
        Collections.shuffle(bucketPractice);
        Collections.shuffle(bucketUnassessed);
        Collections.shuffle(bucketMastered);

        List<AssessmentQuestion> selected = new ArrayList<>();
        for (AssessmentQuestion q : bucketCritical) {
            if (selected.size() < count) selected.add(q);
        }
        for (AssessmentQuestion q : bucketPractice) {
            if (selected.size() < count) selected.add(q);
        }
        for (AssessmentQuestion q : bucketUnassessed) {
            if (selected.size() < count) selected.add(q);
        }
        for (AssessmentQuestion q : bucketMastered) {
            if (selected.size() < count) selected.add(q);
        }

        if (selected.isEmpty()) {
            Collections.shuffle(questions);
            selected = questions.stream().limit(count).collect(Collectors.toList());
        }

        List<String> questionIds = selected.stream().map(AssessmentQuestion::getId).collect(Collectors.toList());
        int totalMarks = selected.stream().mapToInt(AssessmentQuestion::getMarks).sum();

        String subjectName = selected.isEmpty() ? "General Computer Science" : selected.get(0).getSubjectName();

        AssessmentSession session = new AssessmentSession();
        session.setUserId(userId);
        session.setStudentProfileId(userId);
        session.setBranch(branch);
        session.setSemester(semester);
        session.setSubjectCode(subjectCode);
        session.setSubjectName(subjectName);
        session.setQuestionIds(questionIds);
        session.setUsedQuestionFingerprints(fingerprints);
        session.setTotalQuestions(10);
        session.setTotalMarks(20);
        session.setStatus(AssessmentSession.Status.IN_PROGRESS);
        session.setStartTime(LocalDateTime.now());
        session.setQuestionCount(0);
        session.setCurrentQuestionId(questionIds.isEmpty() ? null : questionIds.get(0));
        session.setActiveQuestionSubmitted(false);

        AssessmentSession savedSession = sessionRepository.save(session);

        AssessmentSessionResponse resp = new AssessmentSessionResponse();
        resp.setSessionId(savedSession.getId());
        resp.setBranch(branch);
        resp.setSemester(semester);
        resp.setSubjectCode(subjectCode);
        resp.setSubjectName(subjectName);
        resp.setTotalQuestions(10);
        resp.setTotalMarks(20);
        resp.setQuestions(new ArrayList<>());

        return resp;
    }

    public AssessmentResultResponse submitAssessment(AssessmentSubmissionRequest req) {
        AssessmentSession session = sessionRepository.findById(req.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid assessment session ID: " + req.getSessionId()));

        if (session.getStatus() == AssessmentSession.Status.COMPLETED) {
            throw new IllegalStateException("This assessment session has already been submitted.");
        }

        List<AssessmentQuestion> questions = questionRepository.findAllById(session.getQuestionIds());
        Map<String, AssessmentQuestion> questionMap = questions.stream().collect(Collectors.toMap(AssessmentQuestion::getId, q -> q));

        int totalQuestions = session.getTotalQuestions();
        int totalMarks = session.getTotalMarks();
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
                if (q == null) continue;

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
                    marksObtained = q.getMarks();
                    score += marksObtained;
                    topicStat.put("correct", ((Number) topicStat.get("correct")).intValue() + 1);
                } else {
                    incorrectAnswers++;
                }

                userAnswersList.add(new AssessmentResult.UserAnswer(q.getId(), topic, ansItem.getSelectedOption(), isCorrect, marksObtained));
            }
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
        if (percentage >= 85.0) masteryLevel = "MASTER";
        else if (percentage >= 70.0) masteryLevel = "PROFICIENT";
        else if (percentage >= 50.0) masteryLevel = "INTERMEDIATE";
        else masteryLevel = "NOVICE";

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
                Map<String, Double> masteryMap = prof.getConceptMastery() != null ? prof.getConceptMastery() : new HashMap<>();
                masteryMap.put(session.getSubjectName(), percentage);
                prof.setConceptMastery(masteryMap);
                prof.setCompletedQuizzesCount(prof.getCompletedQuizzesCount() + 1);
                profileRepository.save(prof);
            }
        }

        AssessmentResultResponse response = new AssessmentResultResponse(savedResult);

        // Build Adaptive Assessment Handoff Bridge for concepts evaluated in THIS diagnostic session
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
                        savedResult.getUserId(), savedResult.getSubjectCode(), topic, topic
                );
                if (cmOpt.isPresent()) {
                    ConceptMastery cm = cmOpt.get();
                    boolean requiresAdaptive = (cm.getStatus() == ConceptMastery.ConceptStatus.UNCERTAIN || cm.getStatus() == ConceptMastery.ConceptStatus.WEAK);
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
                            requiresAdaptive
                    ));
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

    public AdaptiveAssessmentDTOs.AdaptiveStartResponse startAdaptiveSession(AdaptiveAssessmentDTOs.AdaptiveStartRequest req, String authenticatedUserId) {
        String effectiveUserId = authenticatedUserId != null && !authenticatedUserId.isBlank() && !"anonymousUser".equals(authenticatedUserId)
                ? authenticatedUserId : req.getUserId();
        if (effectiveUserId == null || effectiveUserId.isBlank()) {
            effectiveUserId = "anonymous_student";
        }

        AssessmentSession diagSession = sessionRepository.findById(req.getDiagnosticSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Diagnostic session not found: " + req.getDiagnosticSessionId()));

        if (diagSession.getUserId() != null && !diagSession.getUserId().equalsIgnoreCase(effectiveUserId) && !"anonymous_student".equals(effectiveUserId)) {
            throw new SecurityException("Unauthorized access to diagnostic session: User does not own session " + req.getDiagnosticSessionId());
        }

        // Identify concepts evaluated in diagnostic that are UNCERTAIN or WEAK for THIS SUBJECT ONLY
        String targetSubj = req.getSubjectName() != null && !req.getSubjectName().isBlank()
                ? req.getSubjectName().trim() : diagSession.getSubjectName();

        List<ConceptMastery> userConcepts = conceptRepository.findByUserId(effectiveUserId);
        Map<String, ConceptMastery> cmMap = new HashMap<>();
        if (userConcepts != null) {
            for (ConceptMastery cm : userConcepts) {
                String cmSubject = cm.getSubjectName();
                if (cmSubject != null && cmSubject.equalsIgnoreCase(targetSubj)) {
                    if (cm.getTopic() != null) cmMap.put(cm.getTopic().trim().toLowerCase(), cm);
                    if (cm.getConceptName() != null) cmMap.put(cm.getConceptName().trim().toLowerCase(), cm);
                }
            }
        }

        List<String> targetConcepts = new ArrayList<>();
        Set<String> diagTopics = new LinkedHashSet<>();

        // Extract concepts evaluated in current diagnostic session from user answers
        if (diagSession.getUserAnswers() != null && !diagSession.getUserAnswers().isEmpty()) {
            for (AssessmentResult.UserAnswer ans : diagSession.getUserAnswers()) {
                if (ans.getTopic() != null && !ans.getTopic().isBlank() && RecommendationService.isConceptValidForSubject(targetSubj, ans.getTopic().trim())) {
                    diagTopics.add(ans.getTopic().trim());
                }
            }
        }

        // Supplement/fallback with QuizQuestionRepository lookup for session questionIds
        if (diagTopics.isEmpty() && diagSession.getQuestionIds() != null && !diagSession.getQuestionIds().isEmpty()) {
            List<QuizQuestion> diagQuestions = quizQuestionRepository.findAllById(diagSession.getQuestionIds());
            for (QuizQuestion q : diagQuestions) {
                if (q.getConcept() != null && !q.getConcept().isBlank() && RecommendationService.isConceptValidForSubject(targetSubj, q.getConcept().trim())) {
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
            if (!RecommendationService.isConceptValidForSubject(targetSubj, top)) continue;
            ConceptMastery cm = cmMap.get(top.toLowerCase());
            if (cm == null || cm.getStatus() == ConceptMastery.ConceptStatus.UNCERTAIN || cm.getStatus() == ConceptMastery.ConceptStatus.UNASSESSED) {
                uncertainConcepts.add(top);
            } else if (cm.getStatus() == ConceptMastery.ConceptStatus.WEAK) {
                weakConcepts.add(top);
            }
        }

        targetConcepts.addAll(uncertainConcepts);
        targetConcepts.addAll(weakConcepts);

        if (targetConcepts.isEmpty()) {
            AdaptiveSession completedSession = new AdaptiveSession(null, req.getDiagnosticSessionId(), effectiveUserId, diagSession.getStudentProfileId(), req.getSubjectCode(), diagSession.getSubjectName(), targetConcepts);
            completedSession.setStatus(AdaptiveSession.Status.COMPLETED);
            adaptiveSessionRepository.save(completedSession);
            return new AdaptiveAssessmentDTOs.AdaptiveStartResponse(completedSession.getId(), req.getSubjectCode(), targetConcepts, 15, 0, true);
        }

        String subjectName = req.getSubjectName() != null && !req.getSubjectName().isBlank()
                ? req.getSubjectName().trim() : diagSession.getSubjectName();

        // Build Stage 2 10-question adaptive blueprint
        List<QuizGenerationService.QuestionBlueprintSpec> blueprint = buildAdaptiveBlueprint(subjectName, effectiveUserId);

        Map<String, Object> genContext = new HashMap<>();
        genContext.put("adaptiveSummary", "Stage 2 10-question adaptive assessment batch");
        List<QuizQuestion> generatedBatch = quizGenerationService.generateBatchDiagnosticQuestionsViaGroq(subjectName, blueprint, genContext);

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

        AdaptiveSession session = new AdaptiveSession(null, req.getDiagnosticSessionId(), effectiveUserId, diagSession.getStudentProfileId(), req.getSubjectCode(), subjectName, targetConcepts);
        session.setUsedQuestionIds(questionIds);
        session.setUsedQuestionFingerprints(fingerprints);
        session.setMaxQuestions(10);
        session.setQuestionCount(0);
        session.setCurrentQuestionId(questionIds.isEmpty() ? null : questionIds.get(0));
        session.setActiveQuestionSubmitted(false);
        session.setStatus(AdaptiveSession.Status.IN_PROGRESS);

        AdaptiveSession saved = adaptiveSessionRepository.save(session);

        return new AdaptiveAssessmentDTOs.AdaptiveStartResponse(saved.getId(), req.getSubjectCode(), targetConcepts, 10, targetConcepts.size(), false);
    }

    public AdaptiveAssessmentDTOs.AdaptiveNextResponse getAdaptiveNextQuestion(AdaptiveAssessmentDTOs.AdaptiveNextRequest req, String authenticatedUserId) {
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

            int count = session.getQuestionCount();
            List<String> qIds = session.getUsedQuestionIds();

            System.out.println("[SESSION DEBUG BEFORE getAdaptiveNextQuestion] sessionId=" + session.getId() + ", currentQuestionIndex=" + count + ", completedCount=" + count + ", isComplete=" + (session.getStatus() == AdaptiveSession.Status.COMPLETED));

            if (session.getStatus() != AdaptiveSession.Status.IN_PROGRESS || qIds == null || count >= qIds.size() || count >= 10) {
                session.setStatus(AdaptiveSession.Status.COMPLETED);
                adaptiveSessionRepository.save(session);
                return new AdaptiveAssessmentDTOs.AdaptiveNextResponse(session.getId(), true, null, 10, 10, session.getCurrentConcept(), session.getCurrentDifficulty() != null ? session.getCurrentDifficulty().name() : "MEDIUM");
            }

            String activeQId = qIds.get(count);
            QuizQuestion newQuestion = quizQuestionRepository.findById(activeQId)
                    .orElseThrow(() -> new IllegalStateException("Question ID " + activeQId + " not found in database"));

            session.setCurrentConcept(newQuestion.getConcept());
            session.setCurrentDifficulty(newQuestion.getDifficulty());
            session.setCurrentQuestionId(newQuestion.getId());
            session.setActiveQuestionSubmitted(false);
            adaptiveSessionRepository.save(session);

            int overallQNum = count + 1;
            System.out.println("[SESSION DEBUG AFTER getAdaptiveNextQuestion] sessionId=" + session.getId() + ", responseQuestionNumber=" + overallQNum + ", responseQuestionId=" + newQuestion.getId() + ", isComplete=false");

            AdaptiveAssessmentDTOs.QuestionItemDTO dto = new AdaptiveAssessmentDTOs.QuestionItemDTO(
                    newQuestion.getId(), newQuestion.getSubject(), newQuestion.getConcept(), newQuestion.getDifficulty().name(),
                    newQuestion.getQuestionText(), newQuestion.getOptions(), newQuestion.getCorrectOptionIndex(), newQuestion.getConceptualExplanation()
            );
            return new AdaptiveAssessmentDTOs.AdaptiveNextResponse(session.getId(), false, dto, overallQNum, 10, newQuestion.getConcept(), newQuestion.getDifficulty().name());
        }
    }

    public AdaptiveAssessmentDTOs.AdaptiveSubmitResponse submitAdaptiveAnswer(AdaptiveAssessmentDTOs.AdaptiveSubmitRequest req, String authenticatedUserId) {
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

    // =========================================================================
    // PHASE 6: GROQ INITIAL DIAGNOSTIC BATCH METHODS
    // =========================================================================

    public AdaptiveAssessmentDTOs.AdaptiveNextResponse getInitialNextQuestion(AdaptiveAssessmentDTOs.AdaptiveNextRequest req, String authenticatedUserId) {
        if (req == null || req.getAdaptiveSessionId() == null || req.getAdaptiveSessionId().isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        Object lock = sessionLocks.computeIfAbsent(req.getAdaptiveSessionId(), k -> new Object());
        synchronized (lock) {
            AssessmentSession session = sessionRepository.findById(req.getAdaptiveSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Initial assessment session not found: " + req.getAdaptiveSessionId()));

            String effectiveUserId = authenticatedUserId != null && !authenticatedUserId.isBlank() && !"anonymousUser".equals(authenticatedUserId)
                    ? authenticatedUserId : session.getUserId();

            if (session.getUserId() != null && !session.getUserId().equalsIgnoreCase(effectiveUserId) && !"anonymous_student".equals(effectiveUserId)) {
                throw new SecurityException("Unauthorized session access: User does not own assessment session " + req.getAdaptiveSessionId());
            }

            int count = session.getQuestionCount();
            List<String> qIds = session.getQuestionIds();

            if (session.getStatus() != AssessmentSession.Status.IN_PROGRESS || qIds == null || count >= qIds.size() || count >= 10) {
                return new AdaptiveAssessmentDTOs.AdaptiveNextResponse(session.getId(), true, null, 10, 10, "Complete", "MEDIUM");
            }

            String activeQId = qIds.get(count);
            QuizQuestion newQuestion = quizQuestionRepository.findById(activeQId)
                    .orElseThrow(() -> new IllegalStateException("Question ID " + activeQId + " not found in database"));

            session.setCurrentQuestionId(newQuestion.getId());
            session.setActiveQuestionSubmitted(false);
            sessionRepository.save(session);

            int currentQNum = count + 1;
            AdaptiveAssessmentDTOs.QuestionItemDTO dto = new AdaptiveAssessmentDTOs.QuestionItemDTO(
                    newQuestion.getId(), newQuestion.getSubject(), newQuestion.getConcept(), newQuestion.getDifficulty().name(),
                    newQuestion.getQuestionText(), newQuestion.getOptions(), newQuestion.getCorrectOptionIndex(), newQuestion.getConceptualExplanation()
            );
            return new AdaptiveAssessmentDTOs.AdaptiveNextResponse(session.getId(), false, dto, currentQNum, 10, newQuestion.getConcept(), newQuestion.getDifficulty().name());
        }
    }

    public AdaptiveAssessmentDTOs.AdaptiveSubmitResponse submitInitialAnswer(AdaptiveAssessmentDTOs.AdaptiveSubmitRequest req, String authenticatedUserId) {
        AssessmentSession session = sessionRepository.findById(req.getAdaptiveSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Initial assessment session not found: " + req.getAdaptiveSessionId()));

        String effectiveUserId = authenticatedUserId != null && !authenticatedUserId.isBlank() && !"anonymousUser".equals(authenticatedUserId)
                ? authenticatedUserId : session.getUserId();

        if (session.getUserId() != null && !session.getUserId().equalsIgnoreCase(effectiveUserId) && !"anonymous_student".equals(effectiveUserId)) {
            throw new SecurityException("Unauthorized session access: User does not own assessment session " + req.getAdaptiveSessionId());
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

        int totalQuestions = session.getTotalQuestions() > 0 ? session.getTotalQuestions() : 10;
        boolean completed = session.getQuestionCount() >= totalQuestions;

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

            try {
                knowledgeService.syncKnowledgeProfileSummary(effectiveUserId, session.getSubjectName());
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

        return new AdaptiveAssessmentDTOs.AdaptiveSubmitResponse(
                session.getId(),
                isCorrect,
                question.getCorrectOptionIndex(),
                question.getConceptualExplanation(),
                completed,
                updatedStatus,
                updatedConf,
                "MEDIUM"
        );
    }
}
