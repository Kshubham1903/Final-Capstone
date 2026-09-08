package com.edupilot.service.llm;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("groqProvider")
public class GroqProvider implements LLMProvider {

    @Value("${llm.groq.api-key:mock-key}")
    private String apiKey;

    @Value("${llm.groq.model:qwen/qwen3.8-27b}")
    private String modelName;

    @Value("${llm.temperature:0.7}")
    private double temperature;

    @Value("${llm.max-tokens:450}")
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
        // Enforce hard upper bound of 450 maxTokens for Groq requests (enforced OTPM limit is 1000)
        effectiveMaxTokens = Math.min(effectiveMaxTokens, 450);

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
                if (body.get("usage") != null && body.get("usage") instanceof Map) {
                    Map usageMap = (Map) body.get("usage");
                    Object promptTokens = usageMap.get("prompt_tokens");
                    Object completionTokens = usageMap.get("completion_tokens");
                    Object totalTokens = usageMap.get("total_tokens");
                    System.out.println("[GroqProvider] Token Usage -> Prompt: " + promptTokens + ", Output (Completion): " + completionTokens + ", Total: " + totalTokens);
                    if (context != null) {
                        if (promptTokens instanceof Number) {
                            context.put("lastPromptTokens", ((Number) promptTokens).intValue());
                        }
                        if (completionTokens instanceof Number) {
                            context.put("lastCompletionTokens", ((Number) completionTokens).intValue());
                        }
                        if (totalTokens instanceof Number) {
                            context.put("lastTotalTokens", ((Number) totalTokens).intValue());
                        }
                    }
                }
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

            if (status == 404 && rawBody != null && rawBody.contains("model_not_found") && !"llama-3.3-70b-versatile".equalsIgnoreCase(modelName)) {
                System.out.println("[GroqProvider] Primary model " + modelName + " returned 404. Retrying with fallback model llama-3.3-70b-versatile...");
                requestBody.put("model", "llama-3.3-70b-versatile");
                HttpEntity<Map<String, Object>> fallbackEntity = new HttpEntity<>(requestBody, headers);
                try {
                    ResponseEntity<Map> fallbackResp = restTemplate.postForEntity(GROQ_ENDPOINT, fallbackEntity, Map.class);
                    if (fallbackResp.getStatusCode().is2xxSuccessful() && fallbackResp.getBody() != null) {
                        List choices = (List) fallbackResp.getBody().get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map firstChoice = (Map) choices.get(0);
                            Map message = (Map) fallbackResp.getBody().get("message");
                            if (message != null && message.get("content") != null) {
                                return (String) message.get("content");
                            }
                        }
                    }
                } catch (Exception fallbackEx) {
                    System.err.println("[GroqProvider] Fallback model llama-3.3-70b-versatile also failed: " + fallbackEx.getMessage());
                }
            }

            if (status == 429) {
                long retryDelayMs = parseRetryDelayMs(hsce, rawBody);

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

    long parseRetryDelayMs(HttpStatusCodeException hsce, String rawBody) {
        String selectedSource = "default (3000ms)";
        long parsedMs = 3000;

        // 1. Check HTTP Retry-After header
        String retryAfterHeader = hsce != null && hsce.getResponseHeaders() != null ? hsce.getResponseHeaders().getFirst("Retry-After") : null;
        if (retryAfterHeader != null && !retryAfterHeader.isBlank()) {
            try {
                double sec = Double.parseDouble(retryAfterHeader.replaceAll("[^0-9.]", ""));
                if (sec > 0) {
                    parsedMs = (long) (sec * 1000);
                    selectedSource = "Header 'Retry-After' (" + retryAfterHeader + ")";
                }
            } catch (Exception ignored) {}
        }

        // 2. Check x-ratelimit-reset-requests header (Do NOT use x-ratelimit-reset-tokens which is the full token window reset time)
        if (selectedSource.startsWith("default") && hsce != null && hsce.getResponseHeaders() != null) {
            String resetReqHeader = hsce.getResponseHeaders().getFirst("x-ratelimit-reset-requests");
            if (resetReqHeader != null && !resetReqHeader.isBlank()) {
                try {
                    if (resetReqHeader.endsWith("s")) {
                        double sec = Double.parseDouble(resetReqHeader.substring(0, resetReqHeader.length() - 1));
                        parsedMs = (long) (sec * 1000);
                        selectedSource = "Header 'x-ratelimit-reset-requests' (" + resetReqHeader + ")";
                    } else if (resetReqHeader.endsWith("ms")) {
                        parsedMs = Long.parseLong(resetReqHeader.substring(0, resetReqHeader.length() - 2));
                        selectedSource = "Header 'x-ratelimit-reset-requests' (" + resetReqHeader + ")";
                    }
                } catch (Exception ignored) {}
            }
        }

        // 3. Check raw body regex (e.g. "Please try again in 14.399999999s", "try again in 14.4s", "wait 15s")
        if (selectedSource.startsWith("default") && rawBody != null && !rawBody.isBlank()) {
            try {
                Pattern patternSec = Pattern.compile("(?:try again in|retry after|wait)\\s+([0-9]+(?:\\.[0-9]+)?)\\s*s", Pattern.CASE_INSENSITIVE);
                Matcher matcherSec = patternSec.matcher(rawBody);
                if (matcherSec.find()) {
                    double sec = Double.parseDouble(matcherSec.group(1));
                    parsedMs = (long) (sec * 1000);
                    selectedSource = "Body regex match '" + matcherSec.group(0) + "' (" + sec + "s)";
                } else {
                    Pattern patternMin = Pattern.compile("(?:try again in|retry after|wait)\\s+([0-9]+(?:\\.[0-9]+)?)\\s*m", Pattern.CASE_INSENSITIVE);
                    Matcher matcherMin = patternMin.matcher(rawBody);
                    if (matcherMin.find()) {
                        double min = Double.parseDouble(matcherMin.group(1));
                        parsedMs = (long) (min * 60 * 1000);
                        selectedSource = "Body regex match '" + matcherMin.group(0) + "' (" + min + "m)";
                    }
                }
            } catch (Exception ignored) {}
        }

        long finalMs = parsedMs + 1000; // 1-second safety buffer
        System.out.println("[GroqProvider Retry-After Parser] Source: " + selectedSource + " -> Parsed: " + parsedMs + " ms | Final wait (+1s safety buffer): " + finalMs + " ms");
        return finalMs;
    }

    public String getModelName() {
        return modelName;
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

    @PostConstruct
    public void logGroqConfig() {
        System.out.println("========== GROQ CONFIG ==========");
        System.out.println("Groq API Key Loaded: " + (apiKey != null && !apiKey.isBlank()));
        System.out.println("Groq API Key Length: " + (apiKey != null ? apiKey.length() : 0));
        System.out.println("Groq Model: " + modelName);
        System.out.println("=================================");
    }
}
