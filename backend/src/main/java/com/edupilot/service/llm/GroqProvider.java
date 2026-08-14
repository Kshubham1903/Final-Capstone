package com.edupilot.service.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service("groqProvider")
public class GroqProvider implements LLMProvider {

    @Value("${llm.groq.api-key:mock-key}")
    private String apiKey;

    @Value("${llm.groq.model:llama-3.3-70b-versatile}")
    private String modelName;

    @Value("${llm.temperature:0.7}")
    private double temperature;

    @Value("${llm.max-tokens:1200}")
    private int maxTokens;

    private static final String GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String generateResponse(String systemPrompt, String userMessage, Map<String, Object> context) {
        if (apiKey == null || apiKey.isBlank() || "mock-key".equalsIgnoreCase(apiKey)) {
            return buildStructuredError(
                "UNAUTHENTICATED",
                "GROQ_API_KEY is not configured or set to default 'mock-key'.",
                "Set GROQ_API_KEY in your environment or .env file."
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
            } catch (Exception ignored) {}
        }

        int promptChars = (systemPrompt != null ? systemPrompt.length() : 0) + (userMessage != null ? userMessage.length() : 0);
        int estPromptTokens = promptChars / 4;
        String purpose = (context != null && context.get("purpose") != null) ? context.get("purpose").toString() : "GENERAL";

        System.out.println("[GroqProvider] model = " + modelName +
                ", maxTokens = " + effectiveMaxTokens +
                ", promptChars = " + promptChars +
                ", estimatedPromptTokens = " + estPromptTokens +
                ", requestPurpose = " + purpose);

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
                return buildStructuredError("PARSE_ERROR", "Groq response had no usable content.", "Retry the request.");
            }
            return buildStructuredError("HTTP_ERROR_" + response.getStatusCode().value(), "Groq request failed.", "Retry the request.");

        } catch (HttpStatusCodeException hsce) {
            int status = hsce.getStatusCode().value();
            String rawBody = hsce.getResponseBodyAsString();
            System.err.println("[GroqProvider] HTTP " + status + ": " + rawBody);

            if (status == 429) {
                String retryAfterHeader = hsce.getResponseHeaders() != null ? hsce.getResponseHeaders().getFirst("Retry-After") : null;
                long retryDelayMs = 2000;
                if (retryAfterHeader != null) {
                    try {
                        retryDelayMs = (long) (Double.parseDouble(retryAfterHeader) * 1000);
                    } catch (Exception ignored) {}
                }

                boolean isTpd = (rawBody != null && (
                        rawBody.toLowerCase().contains("per day") ||
                        rawBody.toLowerCase().contains("tpd") ||
                        rawBody.toLowerCase().contains("rpd") ||
                        rawBody.toLowerCase().contains("daily"))) ||
                        retryDelayMs >= 60000;

                String rateLimitType = isTpd ? "RATE_LIMIT_TPD" : "RATE_LIMIT_TPM";

                return buildStructuredError(
                    rateLimitType,
                    "Groq rate limit exceeded (" + rateLimitType + "). retryAfterMs=" + retryDelayMs,
                    isTpd ? "Groq daily token quota (TPD) reached. Assessment question not consumed. Please retry after quota reset."
                          : "Groq is temporarily rate limited (TPM). Please retry after the indicated delay."
                );
            }
            return buildStructuredError("HTTP_ERROR_" + status, "Groq API request failed: " + hsce.getStatusText(), rawBody != null ? rawBody : "No details available.");

        } catch (Exception e) {
            System.err.println("[GroqProvider] Exception: " + e.getMessage());
            return buildStructuredError("INTERNAL_ERROR", "Groq provider internal exception: " + e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String buildStructuredError(String errorType, String message, String suggestion) {
        return "{\"success\": false, \"provider\": \"Groq\", \"errorType\": \"" + errorType +
               "\", \"message\": \"" + message.replace("\"", "'") +
               "\", \"suggestion\": \"" + suggestion.replace("\"", "'") + "\"}";
    }

    @Override
    public String getProviderName() {
        return "Groq (" + modelName + ")";
    }
}
