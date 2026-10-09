package com.edupilot.service;

import com.edupilot.model.ConceptMastery;
import com.edupilot.model.SubjectRoadmap;
import com.edupilot.repository.ConceptMasteryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class RoadmapProgressionPrerequisiteTest {

    @Autowired
    private RoadmapService roadmapService;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    private final String userId = "test_prereq_user_" + System.currentTimeMillis();

    @BeforeEach
    public void setup() {
        conceptMasteryRepository.deleteAll(conceptMasteryRepository.findByUserId(userId));
    }

    private SubjectRoadmap createSampleRoadmap() {
        SubjectRoadmap roadmap = new SubjectRoadmap();
        roadmap.setSubjectCode("CS301");
        roadmap.setSubjectName("Data Structures & Algorithms");

        List<SubjectRoadmap.RoadmapTopicNode> topics = new ArrayList<>();

        // Topic 1
        SubjectRoadmap.RoadmapTopicNode node1 = new SubjectRoadmap.RoadmapTopicNode();
        node1.setConceptId("arrays-linked-lists");
        node1.setConceptName("Arrays & Linked Lists");
        node1.setSequence(1);
        node1.setPrerequisiteConceptIds(List.of());
        node1.setMasteryRequiredAccuracy(70.0);
        topics.add(node1);

        // Topic 2
        SubjectRoadmap.RoadmapTopicNode node2 = new SubjectRoadmap.RoadmapTopicNode();
        node2.setConceptId("stacks-queues");
        node2.setConceptName("Stacks & Queues");
        node2.setSequence(2);
        node2.setPrerequisiteConceptIds(List.of("arrays-linked-lists"));
        node2.setMasteryRequiredAccuracy(70.0);
        topics.add(node2);

        // Topic 3
        SubjectRoadmap.RoadmapTopicNode node3 = new SubjectRoadmap.RoadmapTopicNode();
        node3.setConceptId("binary-search-trees");
        node3.setConceptName("Binary Search Trees");
        node3.setSequence(3);
        node3.setPrerequisiteConceptIds(List.of("stacks-queues"));
        node3.setMasteryRequiredAccuracy(70.0);
        topics.add(node3);

        // Topic 4
        SubjectRoadmap.RoadmapTopicNode node4 = new SubjectRoadmap.RoadmapTopicNode();
        node4.setConceptId("sorting-algorithms");
        node4.setConceptName("Sorting Algorithms");
        node4.setSequence(4);
        node4.setPrerequisiteConceptIds(List.of("Binary Search Trees")); // Uses Display Name casing
        node4.setMasteryRequiredAccuracy(70.0);
        topics.add(node4);

        roadmap.setTopics(topics);
        return roadmap;
    }

    private void saveConceptMastery(String conceptId, String conceptName, double accuracy, double confidence) {
        ConceptMastery cm = new ConceptMastery();
        cm.setUserId(userId);
        cm.setSubjectCode("CS301");
        cm.setSubjectName("Data Structures & Algorithms");
        cm.setConceptName(conceptName);
        cm.setTopic(conceptName);
        cm.setAccuracy(accuracy);
        cm.setConfidenceScore(confidence);
        cm.setAttemptCount(5);
        cm.setCorrectCount((int) (5 * (accuracy / 100.0)));
        cm.setStatus(accuracy >= 70.0 ? ConceptMastery.ConceptStatus.STRONG : ConceptMastery.ConceptStatus.WEAK);
        conceptMasteryRepository.save(cm);
    }

    @Test
    @DisplayName("Downstream topic with >=70% accuracy but incomplete prerequisites MUST NOT be marked COMPLETED")
    public void testDownstreamHighAccuracyWithIncompletePrerequisitesStaysLocked() {
        // Node 1 mastered
        saveConceptMastery("arrays-linked-lists", "Arrays & Linked Lists", 70.0, 60.0);
        // Node 2 incomplete (40%)
        saveConceptMastery("stacks-queues", "Stacks & Queues", 40.0, 30.0);
        // Node 4 has 75% accuracy (e.g. from broad diagnostic assessment)
        saveConceptMastery("sorting-algorithms", "Sorting Algorithms", 75.0, 70.0);

        SubjectRoadmap roadmap = createSampleRoadmap();
        SubjectRoadmap updated = roadmapService.updateTopicStatesWithExistingMastery(roadmap, userId);

        List<SubjectRoadmap.RoadmapTopicNode> topics = updated.getTopics();

        // Topic 1: Completed
        assertTrue(topics.get(0).isCompleted());
        assertEquals(SubjectRoadmap.TopicStatus.COMPLETED, topics.get(0).getStatus());

        // Topic 2: Unlocked (Active learning node)
        assertFalse(topics.get(1).isCompleted());
        assertEquals(SubjectRoadmap.TopicStatus.UNLOCKED, topics.get(1).getStatus());

        // Topic 3: Locked
        assertFalse(topics.get(2).isCompleted());
        assertEquals(SubjectRoadmap.TopicStatus.LOCKED, topics.get(2).getStatus());

        // Topic 4 (Sorting Algorithms): MUST stay LOCKED despite 75% accuracy because prerequisites are incomplete
        assertFalse(topics.get(3).isCompleted(), "Sorting Algorithms must not be marked completed when prerequisites are incomplete");
        assertEquals(SubjectRoadmap.TopicStatus.LOCKED, topics.get(3).getStatus(), "Sorting Algorithms must remain locked");
        assertEquals(75.0, topics.get(3).getCurrentAccuracy(), "Recorded accuracy of 75% must be preserved");
    }

    @Test
    @DisplayName("Topic becomes COMPLETED when prerequisites are completed AND mastery threshold is met")
    public void testSequentialCompletionWithPrerequisitesMet() {
        saveConceptMastery("arrays-linked-lists", "Arrays & Linked Lists", 80.0, 70.0);
        saveConceptMastery("stacks-queues", "Stacks & Queues", 75.0, 65.0);
        saveConceptMastery("binary-search-trees", "Binary Search Trees", 70.0, 60.0);
        saveConceptMastery("sorting-algorithms", "Sorting Algorithms", 85.0, 80.0);

        SubjectRoadmap roadmap = createSampleRoadmap();
        SubjectRoadmap updated = roadmapService.updateTopicStatesWithExistingMastery(roadmap, userId);

        for (SubjectRoadmap.RoadmapTopicNode topic : updated.getTopics()) {
            assertTrue(topic.isCompleted(), "All topics should be completed sequentially: " + topic.getConceptName());
            assertEquals(SubjectRoadmap.TopicStatus.COMPLETED, topic.getStatus());
        }
    }

    @Test
    @DisplayName("Prerequisite IDs expressed as display names/slugs normalize correctly")
    public void testPrerequisiteNormalizesDisplayNamesAndSlugs() {
        saveConceptMastery("arrays-linked-lists", "Arrays & Linked Lists", 80.0, 70.0);
        saveConceptMastery("stacks-queues", "Stacks & Queues", 80.0, 70.0);
        saveConceptMastery("binary-search-trees", "Binary Search Trees", 80.0, 70.0);
        // Sorting Algorithms references "Binary Search Trees" (Display Name) in prerequisite list
        saveConceptMastery("sorting-algorithms", "Sorting Algorithms", 80.0, 70.0);

        SubjectRoadmap roadmap = createSampleRoadmap();
        SubjectRoadmap updated = roadmapService.updateTopicStatesWithExistingMastery(roadmap, userId);

        SubjectRoadmap.RoadmapTopicNode node4 = updated.getTopics().get(3);
        assertTrue(node4.isCompleted(), "Sorting Algorithms should be completed because 'Binary Search Trees' normalized to match binary-search-trees");
        assertEquals(SubjectRoadmap.TopicStatus.COMPLETED, node4.getStatus());
    }

    @Test
    @DisplayName("Missing prerequisite lists enforce sequential progression fallback")
    public void testMissingPrerequisitesFallbackToSequentialProgression() {
        SubjectRoadmap roadmap = new SubjectRoadmap();
        roadmap.setSubjectCode("CS301");
        roadmap.setSubjectName("Data Structures & Algorithms");

        List<SubjectRoadmap.RoadmapTopicNode> topics = new ArrayList<>();
        
        SubjectRoadmap.RoadmapTopicNode n1 = new SubjectRoadmap.RoadmapTopicNode();
        n1.setConceptId("arrays");
        n1.setSequence(1);
        n1.setPrerequisiteConceptIds(null); // Missing
        topics.add(n1);

        SubjectRoadmap.RoadmapTopicNode n2 = new SubjectRoadmap.RoadmapTopicNode();
        n2.setConceptId("stacks");
        n2.setSequence(2);
        n2.setPrerequisiteConceptIds(null); // Missing
        topics.add(n2);

        SubjectRoadmap.RoadmapTopicNode n3 = new SubjectRoadmap.RoadmapTopicNode();
        n3.setConceptId("trees");
        n3.setSequence(3);
        n3.setPrerequisiteConceptIds(null); // Missing
        topics.add(n3);

        roadmap.setTopics(topics);

        // n1 mastered, n2 unmastered, n3 mastered
        saveConceptMastery("arrays", "Arrays", 80.0, 70.0);
        saveConceptMastery("stacks", "Stacks", 40.0, 30.0);
        saveConceptMastery("trees", "Trees", 90.0, 80.0);

        SubjectRoadmap updated = roadmapService.updateTopicStatesWithExistingMastery(roadmap, userId);
        List<SubjectRoadmap.RoadmapTopicNode> res = updated.getTopics();

        assertTrue(res.get(0).isCompleted());
        assertEquals(SubjectRoadmap.TopicStatus.UNLOCKED, res.get(1).getStatus());
        assertFalse(res.get(2).isCompleted(), "Node 3 must not complete when Node 2 is incomplete, even with null prerequisite lists");
        assertEquals(SubjectRoadmap.TopicStatus.LOCKED, res.get(2).getStatus());
    }

    @Test
    @DisplayName("Mixed roadmap: Node 2 has explicit prerequisites while Node 4 has missing prerequisite list")
    public void testMixedRoadmapPerNodeFallbackPreventsBypass() {
        SubjectRoadmap roadmap = new SubjectRoadmap();
        roadmap.setSubjectCode("CS301");
        roadmap.setSubjectName("Mixed Roadmap Test");

        List<SubjectRoadmap.RoadmapTopicNode> topics = new ArrayList<>();

        SubjectRoadmap.RoadmapTopicNode n1 = new SubjectRoadmap.RoadmapTopicNode();
        n1.setConceptId("node-1");
        n1.setConceptName("Node 1");
        n1.setSequence(1);
        n1.setPrerequisiteConceptIds(List.of());
        topics.add(n1);

        SubjectRoadmap.RoadmapTopicNode n2 = new SubjectRoadmap.RoadmapTopicNode();
        n2.setConceptId("node-2");
        n2.setConceptName("Node 2");
        n2.setSequence(2);
        n2.setPrerequisiteConceptIds(List.of("node-1")); // Explicit
        topics.add(n2);

        SubjectRoadmap.RoadmapTopicNode n3 = new SubjectRoadmap.RoadmapTopicNode();
        n3.setConceptId("node-3");
        n3.setConceptName("Node 3");
        n3.setSequence(3);
        n3.setPrerequisiteConceptIds(null); // Missing
        topics.add(n3);

        SubjectRoadmap.RoadmapTopicNode n4 = new SubjectRoadmap.RoadmapTopicNode();
        n4.setConceptId("node-4");
        n4.setConceptName("Node 4");
        n4.setSequence(4);
        n4.setPrerequisiteConceptIds(null); // Missing
        topics.add(n4);

        roadmap.setTopics(topics);

        // Student completed Node 1 (80%) and has 85% accuracy on Node 4, but Node 2 is uncompleted (30%)
        saveConceptMastery("node-1", "Node 1", 80.0, 70.0);
        saveConceptMastery("node-2", "Node 2", 30.0, 20.0);
        saveConceptMastery("node-3", "Node 3", 0.0, 0.0);
        saveConceptMastery("node-4", "Node 4", 85.0, 80.0);

        SubjectRoadmap updated = roadmapService.updateTopicStatesWithExistingMastery(roadmap, userId);
        List<SubjectRoadmap.RoadmapTopicNode> res = updated.getTopics();

        assertTrue(res.get(0).isCompleted());
        assertEquals(SubjectRoadmap.TopicStatus.UNLOCKED, res.get(1).getStatus(), "Node 2 should be active/unlocked");
        assertFalse(res.get(3).isCompleted(), "Node 4 MUST NOT complete out of order in a mixed roadmap");
        assertEquals(SubjectRoadmap.TopicStatus.LOCKED, res.get(3).getStatus(), "Node 4 must remain locked");
    }

    @Test
    @DisplayName("Explicit non-sequential graph dependencies are respected without forcing linear sequence")
    public void testExplicitNonSequentialGraphDependencies() {
        SubjectRoadmap roadmap = new SubjectRoadmap();
        roadmap.setSubjectCode("CS301");
        roadmap.setSubjectName("Non Sequential Test");

        List<SubjectRoadmap.RoadmapTopicNode> topics = new ArrayList<>();

        SubjectRoadmap.RoadmapTopicNode n1 = new SubjectRoadmap.RoadmapTopicNode();
        n1.setConceptId("basics");
        n1.setConceptName("Basics");
        n1.setSequence(1);
        topics.add(n1);

        SubjectRoadmap.RoadmapTopicNode n2 = new SubjectRoadmap.RoadmapTopicNode();
        n2.setConceptId("track-a");
        n2.setConceptName("Track A");
        n2.setSequence(2);
        n2.setPrerequisiteConceptIds(List.of("basics"));
        topics.add(n2);

        SubjectRoadmap.RoadmapTopicNode n3 = new SubjectRoadmap.RoadmapTopicNode();
        n3.setConceptId("track-b");
        n3.setConceptName("Track B");
        n3.setSequence(3);
        n3.setPrerequisiteConceptIds(List.of("basics")); // Explicitly depends on basics, NOT track-a
        topics.add(n3);

        roadmap.setTopics(topics);

        // Basics completed, track-a uncompleted, track-b 80%
        saveConceptMastery("basics", "Basics", 80.0, 70.0);
        saveConceptMastery("track-a", "Track A", 20.0, 10.0);
        saveConceptMastery("track-b", "Track B", 80.0, 70.0);

        SubjectRoadmap updated = roadmapService.updateTopicStatesWithExistingMastery(roadmap, userId);
        List<SubjectRoadmap.RoadmapTopicNode> res = updated.getTopics();

        assertTrue(res.get(0).isCompleted());
        assertTrue(res.get(2).isCompleted(), "Track B should complete because its explicit prerequisite (basics) is met, regardless of Track A");
    }

    @Test
    @DisplayName("Symbol-heavy concepts (C++, C#, C) normalize to distinct keys without collision")
    public void testSymbolNormalizationDoesNotCollide() {
        assertEquals("cpp", roadmapService.normalizeConceptKey("C++"));
        assertEquals("csharp", roadmapService.normalizeConceptKey("C#"));
        assertEquals("c", roadmapService.normalizeConceptKey("C"));
        assertEquals("dotnet", roadmapService.normalizeConceptKey(".NET"));

        assertNotEquals(roadmapService.normalizeConceptKey("C++"), roadmapService.normalizeConceptKey("C"));
        assertNotEquals(roadmapService.normalizeConceptKey("C#"), roadmapService.normalizeConceptKey("C++"));
        assertNotEquals(roadmapService.normalizeConceptKey("C#"), roadmapService.normalizeConceptKey("C"));
    }

    @Test
    @DisplayName("Repeated roadmap recalculation produces identical idempotent results")
    public void testRoadmapRecalculationIdempotency() {
        saveConceptMastery("arrays-linked-lists", "Arrays & Linked Lists", 80.0, 70.0);
        saveConceptMastery("stacks-queues", "Stacks & Queues", 40.0, 30.0);

        SubjectRoadmap roadmap = createSampleRoadmap();
        SubjectRoadmap run1 = roadmapService.updateTopicStatesWithExistingMastery(roadmap, userId);
        SubjectRoadmap run2 = roadmapService.updateTopicStatesWithExistingMastery(run1, userId);

        assertEquals(run1.getTopics().size(), run2.getTopics().size());
        for (int i = 0; i < run1.getTopics().size(); i++) {
            assertEquals(run1.getTopics().get(i).getStatus(), run2.getTopics().get(i).getStatus());
            assertEquals(run1.getTopics().get(i).isCompleted(), run2.getTopics().get(i).isCompleted());
            assertEquals(run1.getTopics().get(i).getCurrentAccuracy(), run2.getTopics().get(i).getCurrentAccuracy());
        }
    }
}
