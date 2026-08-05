package com.edupilot.service.llm;

import java.util.Map;

public interface LLMProvider {
    /**
     * Generate natural language response from LLM using constructed prompt & student context map.
     */
    String generateResponse(String systemPrompt, String userMessage, Map<String, Object> context);
    
    /**
     * Unique identifier for the provider (e.g., "OpenAI", "Gemini", "Mock").
     */
    String getProviderName();
}
