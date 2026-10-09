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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private com.edupilot.service.StudentService studentService;

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
        System.out.println("Global Fallback Key Configured: " + loaded + " (Note: Per-student keys required for student requests)");
        System.out.println("Resolved Model: " + modelName);
        System.out.println("REST Endpoint: " + endpoint);
        System.out.println("==================================");
    }

    private String resolveEffectiveApiKey(Map<String, Object> context) {
        String resolvedUserId = null;
        if (context != null) {
            if (context.get("userId") != null) {
                resolvedUserId = context.get("userId").toString();
            } else if (context.get("studentId") != null) {
                resolvedUserId = context.get("studentId").toString();
            }
        }

        if ((resolvedUserId == null || resolvedUserId.isBlank()) && studentService != null) {
            try {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                    if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                        resolvedUserId = ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
                    } else if (auth.getPrincipal() instanceof String) {
                        resolvedUserId = (String) auth.getPrincipal();
                    }
                }
            } catch (Exception ignored) {}
        }

        if (resolvedUserId != null && !resolvedUserId.isBlank() && studentService != null) {
            try {
                String personalKey = studentService.getDecryptedGeminiApiKey(resolvedUserId);
                if (personalKey != null && !personalKey.isBlank()) {
                    System.out.println("[GeminiProvider] Using student-specific Gemini API key for userId: " + resolvedUserId);
                    return personalKey;
                }
            } catch (Exception ex) {
                System.err.println("[GeminiProvider] Failed to resolve personal Gemini key for user " + resolvedUserId + ": " + ex.getMessage());
            }
        }

        return null;
    }

    private List<String> getCandidateModels(String primaryModel) {
        List<String> list = new ArrayList<>();
        if (primaryModel != null && !primaryModel.isBlank()) {
            list.add(primaryModel);
        }
        for (String m : List.of("gemini-2.5-flash", "gemini-2.5-flash-lite", "gemini-3.5-flash")) {
            if (!list.contains(m)) {
                list.add(m);
            }
        }
        return list;
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

        // 3. Resolve Per-Student Gemini API Key
        String effectiveApiKey = resolveEffectiveApiKey(context);
        if (effectiveApiKey == null || effectiveApiKey.isBlank() || "mock-key".equalsIgnoreCase(effectiveApiKey)) {
            System.err.println("[GeminiProvider Error] Personal Gemini API key is missing or not configured for student.");
            return buildStructuredError(
                    "UNAUTHENTICATED",
                    "Personal Gemini API key is required. Please configure your Gemini API key in your profile.",
                    "0s",
                    "Please add your personal Gemini API key in your Profile."
            );
        }

        // 4. Construct Base Payload Structure according to Google Gemini v1beta REST Specification
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

        List<String> candidateModels = getCandidateModels(modelName);
        String lastErrorResponse = null;

        // 5. Model Fallback & Retry Loop using SAME student's API key
        for (int mIdx = 0; mIdx < candidateModels.size(); mIdx++) {
            String currentModel = candidateModels.get(mIdx);
            String sanitizedUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + currentModel + ":generateContent";
            String apiUrl = sanitizedUrl + "?key=" + effectiveApiKey;

            System.out.println("\n[STAGE: GeminiProvider Attempting Model] [" + Instant.now() + "] ConvID: " + conversationId + " | Mode: " + learningMode + " | Target Model: " + currentModel + " (" + (mIdx + 1) + "/" + candidateModels.size() + ") | Elapsed: " + (System.currentTimeMillis() - requestStartInstant) + "ms");

            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                long startTime = System.currentTimeMillis();
                System.out.println("[STAGE: Google Gemini REST API Outbound] [" + Instant.now() + "] Attempt: " + attempt + "/" + MAX_ATTEMPTS + " | Model: " + currentModel + " | Endpoint: " + sanitizedUrl + " [KEY_PROTECTED]");

                try {
                    ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
                    long latency = System.currentTimeMillis() - startTime;
                    int httpStatus = response.getStatusCode().value();

                    System.out.println("\n================ RAW GOOGLE RESPONSE AUDIT ================");
                    System.out.println("HTTP Status Code: " + httpStatus);
                    System.out.println("Latency: " + latency + " ms");
                    System.out.println("Model Used: " + currentModel);
                    System.out.println("Response Headers: " + response.getHeaders());
                    System.out.println("============================================================\n");

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        Map body = response.getBody();

                        if (body.containsKey("candidates")) {
                            List candidates = (List) body.get("candidates");
                            int candidateCount = candidates != null ? candidates.size() : 0;
                            printRuntimeDiagnostics(conversationId, learningMode, currentModel, sanitizedUrl, promptLength, httpStatus + " OK", latency, candidateCount);

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

                                                // AUTOMATIC CONTINUATION LOOP IF TRUNCATED BY MAX_TOKENS
                                                if (generatedText != null && ("MAX_TOKENS".equalsIgnoreCase(initialFinishReason) || generatedText.length() >= 7500)) {
                                                    StringBuilder mergedText = new StringBuilder(generatedText);
                                                    String currentFinishReason = initialFinishReason;
                                                    int maxContinuations = 5;
                                                    int continuationCount = 0;

                                                    while (("MAX_TOKENS".equalsIgnoreCase(currentFinishReason) || (mergedText.length() > 0 && mergedText.length() % 7500 == 0)) && continuationCount < maxContinuations) {
                                                        continuationCount++;
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
                                                                                            break;
                                                                                        }
                                                                                        mergedText.append("\n").append(chunk);
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
                                                            break;
                                                        }
                                                    }
                                                    return mergedText.toString();
                                                }

                                                return generatedText;
                                            }
                                        }
                                    }
                                }
                            }
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
                    System.out.println("Model: " + currentModel);
                    System.out.println("Response Headers: " + hsce.getResponseHeaders());
                    System.out.println("Raw Error Body JSON:\n" + rawErrorBody);
                    System.out.println("==================================================================\n");

                    printRuntimeDiagnostics(conversationId, learningMode, currentModel, sanitizedUrl, promptLength, httpStatus + " " + hsce.getStatusText(), latency, 0);

                    // HTTP 401 / 403: Invalid or Unauthorized Key — DO NOT try other models or other keys
                    if (httpStatus == 401 || httpStatus == 403) {
                        System.err.println("[GeminiProvider Audit] Invalid or Unauthorized Gemini API key (HTTP " + httpStatus + "). Stopping model fallback.");
                        return buildStructuredError(
                                "UNAUTHENTICATED",
                                "Your Gemini API key is invalid or unauthorized. Please update your Gemini API key in your profile.",
                                "0s",
                                "Please verify and update your Gemini API key in your Profile."
                        );
                    }

                    boolean isQuotaExceeded = httpStatus == 429 || (rawErrorBody != null && rawErrorBody.contains("RESOURCE_EXHAUSTED"));
                    if (isQuotaExceeded) {
                        System.err.println("[GeminiProvider Audit] Quota Exceeded / Rate Limit on model " + currentModel + " (HTTP 429). Attempt " + attempt + "/" + MAX_ATTEMPTS);
                        if (attempt < MAX_ATTEMPTS) {
                            long backoff = BACKOFF_DELAYS_MS[attempt - 1];
                            try {
                                Thread.sleep(backoff);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                            continue;
                        } else {
                            lastErrorResponse = buildStructuredError(
                                    "QUOTA_EXHAUSTED",
                                    "Daily Gemini API quota exceeded for key.",
                                    extractRetryAfter(rawErrorBody),
                                    "Please try again later or check your Gemini API key quota."
                            );
                            // Quota error is key-level, break attempt loop to check fallback model if appropriate
                            break;
                        }
                    }

                    // HTTP 404 (Model Not Found) or 5xx Server Error -> trigger model fallback using SAME student key
                    if (httpStatus == 404 || httpStatus >= 500) {
                        System.err.println("[GeminiProvider] Model " + currentModel + " failed with status " + httpStatus + ". Will attempt fallback model if available.");
                        lastErrorResponse = buildStructuredError(
                                "HTTP_ERROR_" + httpStatus,
                                "Gemini model " + currentModel + " returned HTTP " + httpStatus + ".",
                                "0s",
                                rawErrorBody != null && !rawErrorBody.isBlank() ? rawErrorBody : hsce.getStatusText()
                        );
                        break; // Exit retry loop for this model, fallback to next model
                    }

                    lastErrorResponse = buildStructuredError(
                            "HTTP_ERROR_" + httpStatus,
                            "Gemini REST API request failed with status " + httpStatus + ".",
                            "0s",
                            rawErrorBody != null && !rawErrorBody.isBlank() ? rawErrorBody : hsce.getStatusText()
                    );

                } catch (Exception e) {
                    long latency = System.currentTimeMillis() - startTime;
                    printRuntimeDiagnostics(conversationId, learningMode, currentModel, sanitizedUrl, promptLength, "500 INTERNAL_SERVER_ERROR", latency, 0);
                    lastErrorResponse = buildStructuredError(
                            "INTERNAL_ERROR",
                            "Gemini provider internal exception: " + e.getClass().getSimpleName(),
                            "0s",
                            e.getMessage()
                    );
                    break;
                }
            }
        }

        return lastErrorResponse != null ? lastErrorResponse : buildStructuredError("GEMINI_ERROR", "All Gemini model attempts failed.", "0s", "Check Gemini API key configuration.");
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
        System.out.println("REST Endpoint: " + endpoint + " [KEY_PROTECTED]");
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
