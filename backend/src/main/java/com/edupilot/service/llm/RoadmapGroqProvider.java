package com.edupilot.service.llm;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("roadmapGroqProvider")
public class RoadmapGroqProvider implements LLMProvider {

    @Value("${llm.groq.roadmap.api-key:${llm.groq.api-key:mock-key}}")
    private String apiKey;

    @Value("${llm.groq.roadmap.model:${llm.groq.model:qwen/qwen3.8-27b}}")
    private String modelName;

    @Value("${llm.temperature:0.7}")
    private double temperature;

    @Value("${llm.max-tokens:1024}")
    private int maxTokens;

    private static final String GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final Object roadmapLock = new Object();
    private static long lastRoadmapRequestTime = 0;
    private static final long MIN_ROADMAP_SPACING_MS = 2000;

    private final RestTemplate restTemplate = new RestTemplate();

    private void acquireRoadmapSlot() {
        long waitMs;
        synchronized (roadmapLock) {
            long now = System.currentTimeMillis();
            long targetSlot = Math.max(now, lastRoadmapRequestTime + MIN_ROADMAP_SPACING_MS);
            lastRoadmapRequestTime = targetSlot;
            waitMs = targetSlot - now;
        }

        if (waitMs > 0) {
            System.out.println("[RoadmapGroqProvider] Coordinated spacing delay: " + waitMs + " ms");
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public String generateResponse(String systemPrompt, String userMessage, Map<String, Object> context) {
        if (apiKey == null || apiKey.isBlank() || "mock-key".equalsIgnoreCase(apiKey)) {
            return buildStructuredError(
                    "UNAUTHENTICATED",
                    "ROADMAP_GROQ_API_KEY is not configured or set to default 'mock-key'.",
                    "Set ROADMAP_GROQ_API_KEY or GROQ_API_KEY in your environment."
            );
        }

        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userMessage != null ? userMessage : ""));

        int effectiveMaxTokens = maxTokens;
        if (context != null && context.containsKey("maxTokens")) {
            try {
                effectiveMaxTokens = Integer.parseInt(context.get("maxTokens").toString());
            } catch (Exception ignored) {
            }
        }

        acquireRoadmapSlot();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", effectiveMaxTokens);
        requestBody.put("response_format", Map.of("type", "json_object"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_ENDPOINT, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map body = response.getBody();
                List choices = (List) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map firstChoice = (Map) choices.get(0);
                    Map message = (Map) firstChoice.get("message");
                    if (message != null && message.get("content") != null) {
                        return (String) message.get("content");
                    }
                }
                return buildStructuredError("PARSE_ERROR", "Roadmap Groq response had no usable content.", "Retry request.");
            }
            return buildStructuredError("HTTP_ERROR_" + response.getStatusCode().value(), "Roadmap Groq request failed.", "Retry request.");

        } catch (HttpStatusCodeException hsce) {
            int status = hsce.getStatusCode().value();
            String rawBody = hsce.getResponseBodyAsString();
            System.err.println("[RoadmapGroqProvider] HTTP " + status + ": " + rawBody);

            if (status == 404 && rawBody != null && rawBody.contains("model_not_found") && !"llama-3.3-70b-versatile".equalsIgnoreCase(modelName)) {
                System.out.println("[RoadmapGroqProvider] Primary model " + modelName + " returned 404. Retrying with fallback llama-3.3-70b-versatile...");
                requestBody.put("model", "llama-3.3-70b-versatile");
                HttpEntity<Map<String, Object>> fallbackEntity = new HttpEntity<>(requestBody, headers);
                try {
                    ResponseEntity<Map> fallbackResp = restTemplate.postForEntity(GROQ_ENDPOINT, fallbackEntity, Map.class);
                    if (fallbackResp.getStatusCode().is2xxSuccessful() && fallbackResp.getBody() != null) {
                        List choices = (List) fallbackResp.getBody().get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map firstChoice = (Map) choices.get(0);
                            Map message = (Map) firstChoice.get("message");
                            if (message != null && message.get("content") != null) {
                                return (String) message.get("content");
                            }
                        }
                    }
                } catch (Exception fallbackEx) {
                    System.err.println("[RoadmapGroqProvider] Fallback model failed: " + fallbackEx.getMessage());
                }
            }

            return buildStructuredError("HTTP_ERROR_" + status, "Roadmap Groq API request failed: " + hsce.getStatusText(), rawBody);
        } catch (Exception e) {
            System.err.println("[RoadmapGroqProvider] Exception: " + e.getMessage());
            return buildStructuredError("INTERNAL_ERROR", "Roadmap Groq provider exception: " + e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String buildStructuredError(String errorType, String message, String suggestion) {
        String safeMessage = message != null ? message.replace("\"", "'") : "No error details available.";
        String safeSuggestion = suggestion != null ? suggestion.replace("\"", "'") : "Retry request.";
        return "{\"success\": false, \"provider\": \"RoadmapGroq\", \"errorType\": \"" + (errorType != null ? errorType : "UNKNOWN") +
                "\", \"message\": \"" + safeMessage +
                "\", \"suggestion\": \"" + safeSuggestion + "\"}";
    }

    @Override
    public String getProviderName() {
        return "Groq Roadmap (" + modelName + ")";
    }

    @PostConstruct
    public void logRoadmapGroqConfig() {
        System.out.println("========== ROADMAP GROQ CONFIG ==========");
        System.out.println("Roadmap Groq API Key Loaded: " + (apiKey != null && !apiKey.isBlank()));
        System.out.println("Roadmap Groq Model: " + modelName);
        System.out.println("=========================================");
    }
}
