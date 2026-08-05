package com.edupilot.service.llm;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("geminiProvider")
public class GeminiProvider implements LLMProvider {

    @Value("${llm.gemini.api-key:mock-key}")
    private String apiKey;

    @Value("${llm.gemini.model:gemini-flash-latest}")
    private String modelName;

    @Value("${llm.temperature:0.7}")
    private double temperature;

    @Value("${llm.top-p:0.95}")
    private double topP;

    @Value("${llm.top-k:40}")
    private int topK;

    @Value("${llm.timeout-seconds:15}")
    private int timeoutSeconds;

    @Value("${llm.max-tokens:8192}")
    private int maxTokens;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Set<String> INJECTION_PATTERNS = Set.of(
            "forget previous instructions",
            "ignore system prompt",
            "reveal api key",
            "show system prompt",
            "ignore context",
            "disregard all prior instructions"
    );

    private static final int MAX_ATTEMPTS = 3;
    private static final long[] BACKOFF_DELAYS_MS = { 1000L, 2000L, 4000L };

    public String getModelName() {
        return modelName;
    }

    @PostConstruct
    public void verifyConfiguration() {
        boolean loaded = apiKey != null && !apiKey.isBlank() && !"mock-key".equalsIgnoreCase(apiKey);
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent";

        System.out.println("========== GEMINI CONFIG ==========");
        System.out.println("Provider: Google Gemini");
        System.out.println("API Key Loaded: " + loaded);
        System.out.println("Resolved Model: " + modelName);
        System.out.println("REST Endpoint:");
        System.out.println(endpoint);
        System.out.println("==================================");
    }

    @Override
    public String generateResponse(String systemPrompt, String userMessage, Map<String, Object> context) {
        long requestStartInstant = System.currentTimeMillis();

        // 1. Prompt Injection Pre-Filter Check
        if (containsPromptInjection(userMessage)) {
            System.out.println("[GeminiProvider] Prompt injection detected in input.");
            return "I am your EduPilot AI Academic Tutor. I can only assist you with your course concepts, academic subjects, and personalized learning plan.";
        }

        // 2. Extract Context Metadata for Diagnostics & Tracing
        String conversationId = context != null && context.containsKey("conversationId") ? String.valueOf(context.get("conversationId")) : "N/A";
        String learningMode = context != null && context.containsKey("learningMode") ? String.valueOf(context.get("learningMode")) : "N/A";

        String fullPrompt = (systemPrompt != null ? systemPrompt : "") + "\n\n[USER QUESTION]\n" + (userMessage != null ? userMessage : "");
        int promptLength = fullPrompt.length();
        String sanitizedUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent";
        String apiUrl = sanitizedUrl + "?key=" + apiKey;

        System.out.println("\n[STAGE: GeminiProvider] [" + Instant.now() + "] ConvID: " + conversationId + " | Mode: " + learningMode + " | Model: " + modelName + " | Elapsed: " + (System.currentTimeMillis() - requestStartInstant) + "ms");

        // 3. Verify API Key Configuration
        if (apiKey == null || apiKey.isBlank() || "mock-key".equalsIgnoreCase(apiKey)) {
            System.err.println("[GeminiProvider Error] GEMINI_API_KEY is not configured or set to default 'mock-key'.");
            return buildStructuredError("UNAUTHENTICATED", "GEMINI_API_KEY is not configured or set to default 'mock-key'. Please set a valid GEMINI_API_KEY.", "0s", "Configure GEMINI_API_KEY in application.yml or environment variables.");
        }

        // 4. Construct JSON Payload Structure according to Google Gemini v1beta REST Specification
        Map<String, Object> textPart = Map.of("text", fullPrompt);
        Map<String, Object> contentObj = Map.of("role", "user", "parts", List.of(textPart));
        
        Map<String, Object> genConfig = new HashMap<>();
        genConfig.put("temperature", temperature);
        genConfig.put("topP", topP);
        genConfig.put("topK", topK);
        genConfig.put("maxOutputTokens", maxTokens);
        genConfig.put("candidateCount", 1);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(contentObj));
        requestBody.put("generationConfig", genConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 5. Exponential Backoff Retry Loop for HTTP 429 RESOURCE_EXHAUSTED
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            long startTime = System.currentTimeMillis();
            System.out.println("[STAGE: Google Gemini REST API Outbound] [" + Instant.now() + "] Attempt: " + attempt + "/" + MAX_ATTEMPTS + " | Endpoint: " + sanitizedUrl);

            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
                long latency = System.currentTimeMillis() - startTime;
                int httpStatus = response.getStatusCode().value();

                // --- RAW GOOGLE RESPONSE AUDIT LOGGING ---
                System.out.println("\n================ RAW GOOGLE RESPONSE AUDIT ================");
                System.out.println("HTTP Status Code: " + httpStatus);
                System.out.println("Latency: " + latency + " ms");
                System.out.println("Response Headers: " + response.getHeaders());
                System.out.println("Full Response Body: " + response.getBody());
                System.out.println("============================================================\n");

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map body = response.getBody();
                    System.out.println("[STAGE: Response Parser] [" + Instant.now() + "] Parsing candidates[0].content.parts[0].text...");

                    if (body.containsKey("candidates")) {
                        List candidates = (List) body.get("candidates");
                        int candidateCount = candidates != null ? candidates.size() : 0;
                        printRuntimeDiagnostics(conversationId, learningMode, modelName, sanitizedUrl, promptLength, httpStatus + " OK", latency, candidateCount);

                        if (candidates != null && !candidates.isEmpty()) {
                            Map firstCand = (Map) candidates.get(0);
                            if (firstCand.containsKey("content")) {
                                Map contentObjMap = (Map) firstCand.get("content");
                                if (contentObjMap.containsKey("parts")) {
                                    List parts = (List) contentObjMap.get("parts");
                                    if (parts != null && !parts.isEmpty()) {
                                        Map firstPart = (Map) parts.get(0);
                                        if (firstPart.containsKey("text")) {
                                            String generatedText = (String) firstPart.get("text");
                                            String initialFinishReason = (String) firstCand.get("finishReason");
                                            System.out.println("[STAGE: Response Parser SUCCESS] Extracted " + (generatedText != null ? generatedText.length() : 0) + " chars | Initial FinishReason: " + initialFinishReason);

                                            // AUTOMATIC CONTINUATION LOOP IF TRUNCATED BY MAX_TOKENS
                                            if (generatedText != null && ("MAX_TOKENS".equalsIgnoreCase(initialFinishReason) || generatedText.length() >= 7500)) {
                                                StringBuilder mergedText = new StringBuilder(generatedText);
                                                String currentFinishReason = initialFinishReason;
                                                int maxContinuations = 5;
                                                int continuationCount = 0;

                                                while (("MAX_TOKENS".equalsIgnoreCase(currentFinishReason) || (mergedText.length() > 0 && mergedText.length() % 7500 == 0)) && continuationCount < maxContinuations) {
                                                    continuationCount++;
                                                    System.out.println("[STAGE: Automatic Continuation] [" + Instant.now() + "] Attempt " + continuationCount + "/" + maxContinuations + " | Merged Length so far: " + mergedText.length() + " chars.");

                                                    String tailSnippet = mergedText.length() > 400 ? mergedText.substring(mergedText.length() - 400) : mergedText.toString();
                                                    String continuationPrompt = fullPrompt + "\n\n[CONTINUATION DIRECTIVE]\nYour previous output was cut off mid-response due to token limits. Continue EXACTLY from where you left off below:\n\"" 
                                                            + tailSnippet + "\"\nDo NOT repeat any previous content or header. Resume mid-sentence or mid-code seamlessly.";

                                                    Map<String, Object> contTextPart = Map.of("text", continuationPrompt);
                                                    Map<String, Object> contContentObj = Map.of("role", "user", "parts", List.of(contTextPart));
                                                    Map<String, Object> contRequestBody = new HashMap<>();
                                                    contRequestBody.put("contents", List.of(contContentObj));
                                                    contRequestBody.put("generationConfig", genConfig);

                                                    HttpEntity<Map<String, Object>> contEntity = new HttpEntity<>(contRequestBody, headers);

                                                    try {
                                                        ResponseEntity<Map> contResponse = restTemplate.postForEntity(apiUrl, contEntity, Map.class);
                                                        if (contResponse.getStatusCode().is2xxSuccessful() && contResponse.getBody() != null) {
                                                            Map contBody = contResponse.getBody();
                                                            if (contBody.containsKey("candidates")) {
                                                                List contCands = (List) contBody.get("candidates");
                                                                if (contCands != null && !contCands.isEmpty()) {
                                                                    Map contCand = (Map) contCands.get(0);
                                                                    currentFinishReason = (String) contCand.get("finishReason");
                                                                    if (contCand.containsKey("content")) {
                                                                        Map contContent = (Map) contCand.get("content");
                                                                        if (contContent.containsKey("parts")) {
                                                                            List contParts = (List) contContent.get("parts");
                                                                            if (contParts != null && !contParts.isEmpty()) {
                                                                                Map contPart = (Map) contParts.get(0);
                                                                                if (contPart.containsKey("text")) {
                                                                                    String chunk = (String) contPart.get("text");
                                                                                    if (chunk == null || chunk.isBlank() || mergedText.toString().endsWith(chunk.trim())) {
                                                                                        System.out.println("[STAGE: Continuation Safety Guard] Empty or duplicate chunk received. Stopping continuation loop.");
                                                                                        break;
                                                                                    }
                                                                                    mergedText.append("\n").append(chunk);
                                                                                    System.out.println("[STAGE: Continuation SUCCESS] Appended " + chunk.length() + " chars. Total Length: " + mergedText.length() + " chars | FinishReason: " + currentFinishReason);
                                                                                    if (!"MAX_TOKENS".equalsIgnoreCase(currentFinishReason)) {
                                                                                        break;
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } catch (Exception contEx) {
                                                        System.err.println("[STAGE: Continuation Error] " + contEx.getMessage() + ". Returning merged text so far.");
                                                        break;
                                                    }
                                                }
                                                return mergedText.toString();
                                            }

                                            return generatedText;
                                        } else {
                                            System.err.println("[Response Parser FAIL] 'text' key missing in parts[0]. Part keys: " + firstPart.keySet());
                                        }
                                    } else {
                                        System.err.println("[Response Parser FAIL] 'parts' list is empty.");
                                    }
                                } else {
                                    System.err.println("[Response Parser FAIL] 'parts' key missing in content. Content keys: " + contentObjMap.keySet());
                                }
                            } else {
                                System.err.println("[Response Parser FAIL] 'content' key missing in candidates[0]. Candidate keys: " + firstCand.keySet());
                            }
                        } else {
                            System.err.println("[Response Parser FAIL] 'candidates' list is empty.");
                        }
                    } else {
                        System.err.println("[Response Parser FAIL] 'candidates' key missing in response body. Body keys: " + body.keySet());
                    }

                    return body.toString();
                }
            } catch (HttpStatusCodeException hsce) {
                long latency = System.currentTimeMillis() - startTime;
                int httpStatus = hsce.getStatusCode().value();
                String rawErrorBody = hsce.getResponseBodyAsString();

                System.out.println("\n================ RAW GOOGLE ERROR RESPONSE AUDIT ================");
                System.out.println("HTTP Status Code: " + httpStatus + " " + hsce.getStatusText());
                System.out.println("Latency: " + latency + " ms");
                System.out.println("Response Headers: " + hsce.getResponseHeaders());
                System.out.println("Raw Error Body JSON:\n" + rawErrorBody);
                System.out.println("==================================================================\n");

                printRuntimeDiagnostics(conversationId, learningMode, modelName, sanitizedUrl, promptLength, httpStatus + " " + hsce.getStatusText(), latency, 0);

                boolean isQuotaExceeded = httpStatus == 429 || (rawErrorBody != null && rawErrorBody.contains("RESOURCE_EXHAUSTED"));
                int remainingRetries = MAX_ATTEMPTS - attempt;

                if (isQuotaExceeded) {
                    System.err.println("[GeminiProvider Audit] Quota Exceeded (HTTP 429). Remaining retries: " + remainingRetries);

                    if (attempt < MAX_ATTEMPTS) {
                        long backoff = BACKOFF_DELAYS_MS[attempt - 1];
                        System.out.println("[GeminiProvider Backoff] Sleeping " + backoff + " ms before retry attempt " + (attempt + 1) + "...");
                        try {
                            Thread.sleep(backoff);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    } else {
                        System.err.println("[GeminiProvider Audit] Quota Exceeded. Final failure after " + MAX_ATTEMPTS + " attempts.");
                        String retryAfter = extractRetryAfter(rawErrorBody);
                        return buildStructuredError(
                                "QUOTA_EXCEEDED",
                                "Daily Gemini API quota exceeded.",
                                retryAfter,
                                "Please try again later or use another configured provider."
                        );
                    }
                } else {
                    return buildStructuredError(
                            "HTTP_ERROR_" + httpStatus,
                            "Gemini REST API request failed with status " + httpStatus + ".",
                            "0s",
                            rawErrorBody != null && !rawErrorBody.isBlank() ? rawErrorBody : hsce.getStatusText()
                    );
                }
            } catch (Exception e) {
                long latency = System.currentTimeMillis() - startTime;
                printRuntimeDiagnostics(conversationId, learningMode, modelName, sanitizedUrl, promptLength, "500 INTERNAL_SERVER_ERROR", latency, 0);

                return buildStructuredError(
                        "INTERNAL_ERROR",
                        "Gemini provider internal exception: " + e.getClass().getSimpleName(),
                        "0s",
                        e.getMessage()
                );
            }
        }

        return buildStructuredError("QUOTA_EXCEEDED", "Daily Gemini API quota exceeded.", "a few minutes", "Please try again later or use another configured provider.");
    }

    private String extractRetryAfter(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return "a few minutes";
        Pattern pattern = Pattern.compile("retryDelay\":\\s*\"(\\d+s)\"|retry in (\\d+\\.\\d+s|\\d+s)");
        Matcher matcher = pattern.matcher(rawJson);
        if (matcher.find()) {
            if (matcher.group(1) != null) return matcher.group(1);
            if (matcher.group(2) != null) return matcher.group(2);
        }
        return "a few minutes";
    }

    private String buildStructuredError(String errorType, String message, String retryAfter, String suggestion) {
        return "{\n" +
               "  \"success\": false,\n" +
               "  \"provider\": \"Gemini\",\n" +
               "  \"errorType\": \"" + escapeJson(errorType) + "\",\n" +
               "  \"message\": \"" + escapeJson(message) + "\",\n" +
               "  \"retryAfter\": \"" + escapeJson(retryAfter) + "\",\n" +
               "  \"suggestion\": \"" + escapeJson(suggestion) + "\"\n" +
               "}";
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                    .replace("\r", "");
    }

    private void printRuntimeDiagnostics(String conversationId, String learningMode, String model, String endpoint, int promptLength, String httpStatus, long latency, int candidateCount) {
        System.out.println("========== GEMINI REQUEST DIAGNOSTICS ==========");
        System.out.println("Conversation ID: " + conversationId);
        System.out.println("Learning Mode: " + learningMode);
        System.out.println("Selected Model: " + model);
        System.out.println("REST Endpoint: " + endpoint);
        System.out.println("Prompt Length: " + promptLength + " chars");
        System.out.println("HTTP Status: " + httpStatus);
        System.out.println("Latency: " + latency + " ms");
        System.out.println("Returned Candidate Count: " + candidateCount);
        System.out.println("===============================================");
    }

    private boolean containsPromptInjection(String input) {
        if (input == null) return false;
        String lower = input.toLowerCase();
        return INJECTION_PATTERNS.stream().anyMatch(lower::contains);
    }

    @Override
    public String getProviderName() {
        return "Google Gemini (" + modelName + ")";
    }
}
