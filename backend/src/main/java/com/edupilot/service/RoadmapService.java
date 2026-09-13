package com.edupilot.service;

import com.edupilot.model.ConceptMastery;
import com.edupilot.model.StudentProfile;
import com.edupilot.model.Subject;
import com.edupilot.model.SubjectRoadmap;
import com.edupilot.repository.ConceptMasteryRepository;
import com.edupilot.repository.SubjectRepository;
import com.edupilot.repository.SubjectRoadmapRepository;
import com.edupilot.service.llm.RoadmapGroqProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoadmapService {

    @Autowired
    private SubjectRoadmapRepository roadmapRepository;

    @Autowired
    private RoadmapGroqProvider roadmapGroqProvider;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ConceptMasteryRepository conceptMasteryRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Primary entry point: Fetches an active roadmap for (userId, subjectCode) or generates a new one.
     */
    public SubjectRoadmap getOrCreateRoadmap(String rawUserId, String subjectCode, String rawSubjectName) {
        String canonicalUserId = studentService.resolveUserId(rawUserId);

        String resolvedCode = (subjectCode != null && !subjectCode.isBlank()) ? subjectCode.trim().toUpperCase() : "CS302";
        String resolvedName = resolveSubjectName(resolvedCode, rawSubjectName);

        Optional<SubjectRoadmap> existingOpt = roadmapRepository.findByUserIdAndSubjectCodeAndStatus(
                canonicalUserId, resolvedCode, SubjectRoadmap.RoadmapStatus.ACTIVE
        );

        if (existingOpt.isPresent()) {
            SubjectRoadmap updated = updateTopicStatesWithExistingMastery(existingOpt.get(), canonicalUserId);
            return roadmapRepository.save(updated);
        }

        return generateAndSaveRoadmap(canonicalUserId, resolvedCode, resolvedName);
    }

    /**
     * Generates a new personalized roadmap using RoadmapGroqProvider with strict validation and deterministic fallback.
     */
    public SubjectRoadmap generateAndSaveRoadmap(String userId, String subjectCode, String subjectName) {
        StudentProfile profile = studentService.findOrCreateProfile(userId);

        List<String> canonicalConceptNames = RecommendationService.getSubjectBlueprintConcepts(subjectName);
        if (canonicalConceptNames == null || canonicalConceptNames.isEmpty()) {
            canonicalConceptNames = List.of(
                    "Relational Data Modeling",
                    "SQL Query Optimization",
                    "Database Normalization",
                    "ACID Transactions",
                    "Indexing & B-Trees"
            );
        }

        Map<String, String> canonicalIdToNameMap = new LinkedHashMap<>();
        Map<String, String> canonicalNameToIdMap = new LinkedHashMap<>();
        for (String cName : canonicalConceptNames) {
            String cId = slugifyConceptName(cName);
            canonicalIdToNameMap.put(cId, cName);
            canonicalNameToIdMap.put(cName.toLowerCase(), cId);
        }

        List<ConceptMastery> cmList = conceptMasteryRepository.findByUserId(userId);
        List<String> masteredList = new ArrayList<>();
        List<String> weakList = new ArrayList<>();

        if (cmList != null) {
            for (ConceptMastery cm : cmList) {
                if (cm.getConceptName() != null) {
                    if (cm.getAccuracy() >= 70.0 || cm.getStatus() == ConceptMastery.ConceptStatus.STRONG) {
                        masteredList.add(cm.getConceptName());
                    } else if (cm.getAccuracy() < 70.0 || cm.getStatus() == ConceptMastery.ConceptStatus.WEAK) {
                        weakList.add(cm.getConceptName());
                    }
                }
            }
        }

        List<SubjectRoadmap.RoadmapTopicNode> generatedNodes = null;

        try {
            String systemPrompt = buildSystemPrompt(subjectCode, subjectName, canonicalIdToNameMap);
            String userPrompt = buildUserPrompt(subjectName, profile, masteredList, weakList);

            Map<String, Object> context = new HashMap<>();
            context.put("purpose", "ROADMAP_GENERATION");

            String rawAiResponse = roadmapGroqProvider.generateResponse(systemPrompt, userPrompt, context);

            if (rawAiResponse != null && !rawAiResponse.isBlank() && !rawAiResponse.contains("\"success\": false")) {
                generatedNodes = parseAndValidateAiResponse(rawAiResponse, canonicalIdToNameMap, subjectName);
            }
        } catch (Exception ex) {
            System.err.println("[RoadmapService] AI generation error: " + ex.getMessage() + ". Using deterministic fallback.");
        }

        if (generatedNodes == null || generatedNodes.isEmpty()) {
            System.out.println("[RoadmapService] Using deterministic canonical fallback roadmap for subject: " + subjectName);
            generatedNodes = buildFallbackRoadmapNodes(canonicalIdToNameMap);
        }

        // Archive any previous active roadmap for this subject
        Optional<SubjectRoadmap> oldActive = roadmapRepository.findByUserIdAndSubjectCodeAndStatus(userId, subjectCode, SubjectRoadmap.RoadmapStatus.ACTIVE);
        if (oldActive.isPresent()) {
            SubjectRoadmap prev = oldActive.get();
            prev.setStatus(SubjectRoadmap.RoadmapStatus.INACTIVE);
            prev.setUpdatedAt(LocalDateTime.now());
            roadmapRepository.save(prev);
        }

        SubjectRoadmap roadmap = new SubjectRoadmap();
        roadmap.setUserId(userId);
        roadmap.setSubjectCode(subjectCode);
        roadmap.setSubjectName(subjectName);
        roadmap.setVersion(1);
        roadmap.setStatus(SubjectRoadmap.RoadmapStatus.ACTIVE);
        roadmap.setCreatedAt(LocalDateTime.now());
        roadmap.setUpdatedAt(LocalDateTime.now());
        roadmap.setTopics(generatedNodes);

        SubjectRoadmap updatedRoadmap = updateTopicStatesWithExistingMastery(roadmap, userId);
        return roadmapRepository.save(updatedRoadmap);
    }

    /**
     * Parses and strictly validates AI response JSON to prevent unknown concepts, duplicates, or circular dependencies.
     */
    public List<SubjectRoadmap.RoadmapTopicNode> parseAndValidateAiResponse(String jsonResponse, Map<String, String> canonicalMap, String targetSubjectName) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        if (!root.has("topics") || !root.get("topics").isArray()) {
            throw new IllegalArgumentException("AI response missing 'topics' array.");
        }

        JsonNode topicsNode = root.get("topics");
        if (topicsNode.isEmpty()) {
            throw new IllegalArgumentException("'topics' array is empty.");
        }

        List<SubjectRoadmap.RoadmapTopicNode> nodes = new ArrayList<>();
        Set<String> seenConceptIds = new HashSet<>();
        Map<String, List<String>> prereqGraph = new HashMap<>();

        for (int i = 0; i < topicsNode.size(); i++) {
            JsonNode item = topicsNode.get(i);
            String conceptId = item.has("conceptId") ? item.get("conceptId").asText().trim().toLowerCase() : "";
            int sequence = item.has("sequence") ? item.get("sequence").asInt() : (i + 1);
            String aiRationale = item.has("aiRationale") ? item.get("aiRationale").asText().trim() : "Recommended for subject progression.";

            if (!canonicalMap.containsKey(conceptId)) {
                throw new IllegalArgumentException("AI returned non-canonical conceptId: '" + conceptId + "'");
            }

            if (seenConceptIds.contains(conceptId)) {
                throw new IllegalArgumentException("AI returned duplicate conceptId: '" + conceptId + "'");
            }
            seenConceptIds.add(conceptId);

            List<String> prereqs = new ArrayList<>();
            if (item.has("prerequisites") && item.get("prerequisites").isArray()) {
                for (JsonNode pNode : item.get("prerequisites")) {
                    String pId = pNode.asText().trim().toLowerCase();
                    if (canonicalMap.containsKey(pId) && !pId.equals(conceptId)) {
                        prereqs.add(pId);
                    }
                }
            }

            prereqGraph.put(conceptId, prereqs);

            SubjectRoadmap.RoadmapTopicNode node = new SubjectRoadmap.RoadmapTopicNode();
            node.setConceptId(conceptId);
            node.setConceptName(canonicalMap.get(conceptId));
            node.setSequence(sequence);
            node.setDescription("Core module covering " + canonicalMap.get(conceptId));
            node.setPrerequisiteConceptIds(prereqs);
            node.setStatus(SubjectRoadmap.TopicStatus.LOCKED);
            node.setMasteryRequiredAccuracy(70.0);
            node.setCurrentAccuracy(0.0);
            node.setCompleted(false);
            node.setAiRationale(aiRationale);

            nodes.add(node);
        }

        if (hasCircularDependency(prereqGraph)) {
            throw new IllegalArgumentException("Circular prerequisite dependency detected in AI output.");
        }

        nodes.sort(Comparator.comparingInt(SubjectRoadmap.RoadmapTopicNode::getSequence));
        return nodes;
    }

    /**
     * Cycle detection using DFS graph traversal.
     */
    private boolean hasCircularDependency(Map<String, List<String>> graph) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String node : graph.keySet()) {
            if (detectCycleDFS(node, graph, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean detectCycleDFS(String node, Map<String, List<String>> graph, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(node)) return true;
        if (visited.contains(node)) return false;

        visited.add(node);
        recursionStack.add(node);

        List<String> neighbors = graph.getOrDefault(node, Collections.emptyList());
        for (String neighbor : neighbors) {
            if (detectCycleDFS(neighbor, graph, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(node);
        return false;
    }

    /**
     * Builds a safe, deterministic canonical fallback roadmap when AI is unavailable or produces invalid JSON.
     */
    public List<SubjectRoadmap.RoadmapTopicNode> buildFallbackRoadmapNodes(Map<String, String> canonicalMap) {
        List<SubjectRoadmap.RoadmapTopicNode> fallbackNodes = new ArrayList<>();
        int seq = 1;
        String previousConceptId = null;

        for (Map.Entry<String, String> entry : canonicalMap.entrySet()) {
            String cId = entry.getKey();
            String cName = entry.getValue();

            List<String> prereqs = new ArrayList<>();
            if (previousConceptId != null) {
                prereqs.add(previousConceptId);
            }

            SubjectRoadmap.RoadmapTopicNode node = new SubjectRoadmap.RoadmapTopicNode();
            node.setConceptId(cId);
            node.setConceptName(cName);
            node.setSequence(seq);
            node.setDescription("Foundational topic covering " + cName);
            node.setPrerequisiteConceptIds(prereqs);
            node.setStatus(SubjectRoadmap.TopicStatus.LOCKED);
            node.setMasteryRequiredAccuracy(70.0);
            node.setCurrentAccuracy(0.0);
            node.setCompleted(false);
            node.setAiRationale("Sequential curriculum progression for " + cName + ".");

            fallbackNodes.add(node);
            previousConceptId = cId;
            seq++;
        }

        return fallbackNodes;
    }

    /**
     * Evaluates existing ConceptMastery metrics and updates topic completion and lock/unlock states.
     * Enforces strict prerequisite dependency resolution:
     * - A topic can ONLY be COMPLETED if its own ConceptMastery is mastered AND ALL of its prerequisite topics are COMPLETED.
     * - A non-completed topic is UNLOCKED ONLY IF ALL of its prerequisite topics are COMPLETED AND it is the FIRST eligible uncompleted topic.
     * - All other non-completed topics remain LOCKED.
     */
    public SubjectRoadmap updateTopicStatesWithExistingMastery(SubjectRoadmap roadmap, String userId) {
        if (roadmap == null || roadmap.getTopics() == null) return roadmap;

        List<ConceptMastery> userCmList = conceptMasteryRepository.findByUserId(userId);
        
        List<SubjectRoadmap.RoadmapTopicNode> topics = roadmap.getTopics();
        topics.sort(Comparator.comparingInt(SubjectRoadmap.RoadmapTopicNode::getSequence));

        // Step 1: Update currentAccuracy and check raw self-mastery for each node
        Map<String, Boolean> selfMasteredMap = new HashMap<>();
        for (SubjectRoadmap.RoadmapTopicNode node : topics) {
            ConceptMastery cm = findMatchingConceptMastery(node, userCmList);
            double acc = cm != null ? cm.getAccuracy() : 0.0;
            double conf = cm != null ? cm.getConfidenceScore() : 0.0;
            double reqAcc = node.getMasteryRequiredAccuracy() > 0 ? node.getMasteryRequiredAccuracy() : 70.0;

            node.setCurrentAccuracy(acc);

            // Self-mastery condition: Accuracy >= 70% AND Confidence >= 50% (or status STRONG)
            boolean isSelfMastered = (cm != null && acc >= reqAcc && (conf >= 50.0 || cm.getStatus() == ConceptMastery.ConceptStatus.STRONG));
            selfMasteredMap.put(node.getConceptId(), isSelfMastered);
        }

        // Step 2: Iteratively determine COMPLETED topics respecting prerequisite dependency chains
        Set<String> completedConceptIds = new HashSet<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (SubjectRoadmap.RoadmapTopicNode node : topics) {
                String cId = node.getConceptId();
                if (completedConceptIds.contains(cId)) {
                    continue;
                }

                boolean selfMastered = selfMasteredMap.getOrDefault(cId, false);
                if (!selfMastered) {
                    continue;
                }

                boolean allPrereqsCompleted = true;
                if (node.getPrerequisiteConceptIds() != null && !node.getPrerequisiteConceptIds().isEmpty()) {
                    for (String pId : node.getPrerequisiteConceptIds()) {
                        if (!completedConceptIds.contains(pId)) {
                            allPrereqsCompleted = false;
                            break;
                        }
                    }
                }

                if (allPrereqsCompleted) {
                    completedConceptIds.add(cId);
                    changed = true;
                }
            }
        }

        // Step 3: Assign status and completion flags to all nodes
        boolean firstIncompleteUnlocked = false;

        for (SubjectRoadmap.RoadmapTopicNode node : topics) {
            String cId = node.getConceptId();
            if (completedConceptIds.contains(cId)) {
                node.setCompleted(true);
                node.setStatus(SubjectRoadmap.TopicStatus.COMPLETED);
                continue;
            }

            node.setCompleted(false);

            boolean allPrereqsCompleted = true;
            if (node.getPrerequisiteConceptIds() != null && !node.getPrerequisiteConceptIds().isEmpty()) {
                for (String pId : node.getPrerequisiteConceptIds()) {
                    if (!completedConceptIds.contains(pId)) {
                        allPrereqsCompleted = false;
                        break;
                    }
                }
            }

            if (allPrereqsCompleted && !firstIncompleteUnlocked) {
                node.setStatus(SubjectRoadmap.TopicStatus.UNLOCKED);
                firstIncompleteUnlocked = true;
            } else {
                node.setStatus(SubjectRoadmap.TopicStatus.LOCKED);
            }
        }

        roadmap.setUpdatedAt(LocalDateTime.now());
        return roadmap;
    }

    /**
     * Helper to match a roadmap topic node to its corresponding user ConceptMastery record.
     */
    private ConceptMastery findMatchingConceptMastery(SubjectRoadmap.RoadmapTopicNode node, List<ConceptMastery> userCmList) {
        if (userCmList == null || userCmList.isEmpty() || node == null) return null;
        String targetName = node.getConceptName() != null ? node.getConceptName().toLowerCase().trim() : "";
        String targetId = node.getConceptId() != null ? node.getConceptId().toLowerCase().trim() : "";

        ConceptMastery bestMatch = null;
        for (ConceptMastery cm : userCmList) {
            String cmName = cm.getConceptName() != null ? cm.getConceptName().toLowerCase().trim() : "";
            String cmTopic = cm.getTopic() != null ? cm.getTopic().toLowerCase().trim() : "";
            String cmSlugName = slugifyConceptName(cmName);
            String cmSlugTopic = slugifyConceptName(cmTopic);

            boolean isMatch = cmName.equalsIgnoreCase(targetName) || 
                              cmTopic.equalsIgnoreCase(targetName) ||
                              cmSlugName.equalsIgnoreCase(targetId) || 
                              cmSlugTopic.equalsIgnoreCase(targetId);

            if (isMatch) {
                if (bestMatch == null || cm.getAttemptCount() > bestMatch.getAttemptCount() ||
                   (cm.getLastAssessedAt() != null && (bestMatch.getLastAssessedAt() == null || cm.getLastAssessedAt().isAfter(bestMatch.getLastAssessedAt())))) {
                    bestMatch = cm;
                }
            }
        }
        return bestMatch;
    }

    private String buildSystemPrompt(String subjectCode, String subjectName, Map<String, String> canonicalMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert AI Curriculum Engineer for EduRise AI.\n");
        sb.append("Your task is to generate a personalized learning roadmap for the subject: '").append(subjectName).append("' (Code: ").append(subjectCode).append(").\n\n");
        sb.append("CRITICAL CONSTRAINTS:\n");
        sb.append("1. You MUST ONLY use the following canonical concept IDs. Do NOT invent new concept IDs.\n");
        for (Map.Entry<String, String> entry : canonicalMap.entrySet()) {
            sb.append("   - conceptId: \"").append(entry.getKey()).append("\" | Name: \"").append(entry.getValue()).append("\"\n");
        }
        sb.append("\n2. Respond ONLY with a valid JSON object matching this schema:\n");
        sb.append("{\n");
        sb.append("  \"subjectName\": \"").append(subjectName).append("\",\n");
        sb.append("  \"topics\": [\n");
        sb.append("    {\n");
        sb.append("      \"conceptId\": \"canonical_id\",\n");
        sb.append("      \"sequence\": 1,\n");
        sb.append("      \"prerequisites\": [\"canonical_id\"],\n");
        sb.append("      \"aiRationale\": \"Personalized rationale for topic order.\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n");
        sb.append("3. Do NOT include markdown blocks or extra commentary. Return pure JSON.");
        return sb.toString();
    }

    private String buildUserPrompt(String subjectName, StudentProfile profile, List<String> mastered, List<String> weak) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate personalized roadmap for subject: ").append(subjectName).append("\n");
        sb.append("Student Profile:\n");
        sb.append("- Branch: ").append(profile.getBranch() != null ? profile.getBranch() : "Computer Science & Engineering").append("\n");
        sb.append("- Semester: ").append(profile.getSemester()).append("\n");
        sb.append("- Target CGPA: ").append(profile.getTargetCgpa()).append("\n");
        sb.append("- Preferred Study Hours/Day: ").append(profile.getPreferredStudyHoursPerDay()).append("\n");
        sb.append("- Career Goals: ").append(profile.getCareerGoals() != null ? String.join(", ", profile.getCareerGoals()) : "Software Engineer").append("\n");
        sb.append("- Mastered Topics: ").append(mastered.isEmpty() ? "None" : String.join(", ", mastered)).append("\n");
        sb.append("- Weak Topics: ").append(weak.isEmpty() ? "None" : String.join(", ", weak)).append("\n");
        return sb.toString();
    }

    private String slugifyConceptName(String conceptName) {
        if (conceptName == null) return "concept";
        return conceptName.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "_")
                .trim();
    }

    private String resolveSubjectName(String subjectCode, String rawName) {
        if (rawName != null && !rawName.isBlank()) return rawName.trim();

        Optional<Subject> subjOpt = subjectRepository.findBySubjectCode(subjectCode);
        if (subjOpt.isPresent()) {
            return subjOpt.get().getSubjectName();
        }

        if ("CS302".equalsIgnoreCase(subjectCode)) return "Database Management Systems";
        if ("CS301".equalsIgnoreCase(subjectCode)) return "Data Structures & Algorithms";
        if ("CS401".equalsIgnoreCase(subjectCode)) return "Operating Systems";
        if ("CS402".equalsIgnoreCase(subjectCode)) return "Computer Networks";

        return "Database Management Systems";
    }
}
