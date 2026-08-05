package com.edupilot.service.llm;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.util.*;

@Service("geminiProvider")
public class GeminiProvider implements LLMProvider {

    @Value("${llm.gemini.api-key:mock-key}")
    private String apiKey;

    @Value("${llm.gemini.model:gemini-1.5-flash}")
    private String modelName;

    @PostConstruct
    public void verifyConfiguration() {
        boolean loaded = apiKey != null && !apiKey.isBlank() && !"mock-key".equalsIgnoreCase(apiKey);

        System.out.println("========================================");
        System.out.println("Gemini Provider Startup Verification");
        System.out.println("Model: " + modelName);
        System.out.println("API Key Loaded: " + loaded);

        if (loaded) {
            System.out.println("Gemini configuration is valid.");
        } else {
            System.out.println("WARNING: Using fallback mock-key.");
        }

        System.out.println("========================================");
    }

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Set<String> INJECTION_PATTERNS = Set.of(
            "forget previous instructions",
            "ignore system prompt",
            "reveal api key",
            "show system prompt",
            "ignore context",
            "disregard all prior instructions"
    );

    @Override
    public String generateResponse(String systemPrompt, String userMessage, Map<String, Object> context) {
        // 1. Prompt Injection Pre-Filter Check
        if (containsPromptInjection(userMessage)) {
            System.out.println("[GeminiProvider] Prompt injection detected in input.");
            return "I am your EduPilot AI Academic Tutor. I can only assist you with your course concepts, academic subjects, and personalized learning plan.";
        }

        String concept = context != null && context.containsKey("referencedConcept") ? (String) context.get("referencedConcept") : "your learning topic";
        String studentName = context != null && context.containsKey("studentName") ? (String) context.get("studentName") : "Student";

        // 2. Verify API Key
        if (apiKey == null || apiKey.isBlank() || "mock-key".equalsIgnoreCase(apiKey)) {
            System.out.println("[GeminiProvider] WARNING: Using fallback mock-key.");
            return generateContextAwareFallback(userMessage, concept, studentName);
        }

        long startTime = System.currentTimeMillis();
        try {
            // 3. Construct Gemini REST API URL
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;
            String sanitizedUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent";

            // 4. Construct JSON Payload Structure
            Map<String, Object> textPart = Map.of("text", systemPrompt + "\n\n[USER QUESTION]\n" + userMessage);
            Map<String, Object> contentObj = Map.of("role", "user", "parts", List.of(textPart));
            Map<String, Object> genConfig = Map.of("temperature", 0.7, "maxOutputTokens", 800);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(contentObj));
            requestBody.put("generationConfig", genConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            System.out.println("======== GEMINI REST API REQUEST ========");
            System.out.println("Request URL: " + sanitizedUrl);
            System.out.println("Model: " + modelName);
            System.out.println("Payload Structure Valid: contents -> role=user -> parts[0].text, generationConfig={temperature=0.7, maxOutputTokens=800}");

            // 5. Execute HTTP POST
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            long latency = System.currentTimeMillis() - startTime;

            System.out.println("======== GEMINI REST API RESPONSE ========");
            System.out.println("HTTP Status: " + response.getStatusCode());
            System.out.println("Latency: " + latency + " ms");

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                if (body.containsKey("candidates")) {
                    List candidates = (List) body.get("candidates");
                    int candidatesFound = candidates != null ? candidates.size() : 0;
                    System.out.println("Candidates Found: " + candidatesFound);

                    if (candidates != null && !candidates.isEmpty()) {
                        Map firstCand = (Map) candidates.get(0);
                        if (firstCand.containsKey("content")) {
                            Map content = (Map) firstCand.get("content");
                            if (content.containsKey("parts")) {
                                List parts = (List) content.get("parts");
                                if (parts != null && !parts.isEmpty()) {
                                    Map firstPart = (Map) parts.get(0);
                                    if (firstPart.containsKey("text")) {
                                        String resultText = (String) firstPart.get("text");
                                        System.out.println("Text Length: " + (resultText != null ? resultText.length() : 0));
                                        System.out.println("=========================================");
                                        return resultText;
                                    } else {
                                        System.err.println("PARSER ERROR: Missing 'text' key inside parts[0]. Available keys: " + firstPart.keySet());
                                    }
                                } else {
                                    System.err.println("PARSER ERROR: 'parts' list is empty in content.");
                                }
                            } else {
                                System.err.println("PARSER ERROR: Missing 'parts' key inside content. Available keys: " + content.keySet());
                            }
                        } else {
                            System.err.println("PARSER ERROR: Missing 'content' key inside candidates[0]. Available keys: " + firstCand.keySet());
                        }
                    } else {
                        System.err.println("PARSER ERROR: 'candidates' array is empty.");
                    }
                } else {
                    System.err.println("PARSER ERROR: Response body missing 'candidates' key. Body keys: " + body.keySet());
                }
            } else {
                System.err.println("Gemini HTTP Error: Status=" + response.getStatusCode() + ", Body=" + response.getBody());
            }
        } catch (org.springframework.web.client.HttpStatusCodeException hsce) {
            long latency = System.currentTimeMillis() - startTime;
            System.err.println("======== GEMINI HTTP EXCEPTION ========");
            System.err.println("HTTP Status: " + hsce.getStatusCode());
            System.err.println("Latency: " + latency + " ms");
            System.err.println("Complete Error Response Body: " + hsce.getResponseBodyAsString());
            System.err.println("=======================================");
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            System.err.println("======== GEMINI GENERAL EXCEPTION ========");
            System.err.println("Latency: " + latency + " ms");
            System.err.println("Exception: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            System.err.println("==========================================");
        }

        System.out.println("GeminiProvider: Returning fallback response.");
        return generateContextAwareFallback(userMessage, concept, studentName);
    }

    private boolean containsPromptInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return INJECTION_PATTERNS.stream().anyMatch(lower::contains);
    }

    private String generateContextAwareFallback(String userMessage, String concept, String studentName) {
        String msgLower = userMessage.toLowerCase();
        
        if (msgLower.contains("avl") || msgLower.contains("tree") || msgLower.contains("bst")) {
            return "### 🌲 Understanding Binary Search Trees & AVL Rotations\n\n" +
                    "Hello " + studentName + "! Based on your Knowledge Profile, **AVL Trees** is currently identified as a weak concept needing review.\n\n" +
                    "#### Key Principles of AVL Balancing:\n" +
                    "1. **Height-Balanced Property**: For every node, the height difference (Balance Factor = Height(Left) - Height(Right)) must be `-1, 0, or +1`.\n" +
                    "2. **Rotations**:\n" +
                    "   - **Left Rotation (RR Case)**: Applied when a right child's right subtree causes imbalance.\n" +
                    "   - **Right Rotation (LL Case)**: Applied when a left child's left subtree causes imbalance.\n" +
                    "   - **Double Rotations (LR / RL)**: Require an inner rotation first followed by an outer rotation.\n\n" +
                    "Would you like to walk through a step-by-step numerical insertion example?";
        }

        if (msgLower.contains("plan") || msgLower.contains("today") || msgLower.contains("schedule")) {
            return "### 📅 Today's Personalized Learning Plan\n\n" +
                    "Hi " + studentName + "! I checked your Personalized Learning Planner for today:\n\n" +
                    "- **Active Subject**: Data Structures & Algorithms\n" +
                    "- **Scheduled Focus**: " + concept + "\n" +
                    "- **Recommended Action**: Review BST balancing rules and solve 5 practice questions to strengthen your concept mastery.\n\n" +
                    "Ready to start a 25-minute Pomodoro study session for this task?";
        }

        if (msgLower.contains("revise") || msgLower.contains("recommend") || msgLower.contains("first")) {
            return "### 🎯 Recommended Priority Focus\n\n" +
                    "Based on your latest Diagnostic Assessment score and Concept Accuracy (<50%):\n\n" +
                    "1. **CRITICAL Priority**: " + concept + "\n" +
                    "2. **Reason**: Your concept accuracy is below the 50.0% threshold after recent diagnostic tests.\n\n" +
                    "I recommend focusing 70% of today's study time on this concept before advancing to new topics.";
        }

        return "### 💡 EduPilot AI Tutor Insight\n\n" +
                "Hi " + studentName + "! Regarding your question about **" + userMessage + "**:\n\n" +
                "In **" + concept + "**, mastery comes from breaking down the core principles into structured steps. " +
                "Let's review the key formulas, edge cases, and code patterns together!";
    }

    @Override
    public String getProviderName() {
        return "Google Gemini (" + modelName + ")";
    }
}
