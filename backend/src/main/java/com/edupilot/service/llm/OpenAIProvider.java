package com.edupilot.service.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service("openAIProvider")
public class OpenAIProvider implements LLMProvider {

    @Value("${llm.openai.api-key:mock-key}")
    private String apiKey;

    @Value("${llm.openai.model:gpt-4o-mini}")
    private String modelName;

    @Override
    public String generateResponse(String systemPrompt, String userMessage, Map<String, Object> context) {
        if (apiKey == null || apiKey.isBlank() || "mock-key".equalsIgnoreCase(apiKey)) {
            return "⚠️ **OpenAI API Configuration Error**\n\n" +
                   "- **Status**: API Key Not Configured\n" +
                   "- **Reason**: The `OPENAI_API_KEY` environment variable is missing or set to default `mock-key`.\n" +
                   "- **Action**: Please set a valid OpenAI API key or select Gemini provider (`llm.provider=gemini`).";
        }
        return "⚠️ **OpenAI Service Not Configured**\n\nOpenAI integration requires active API endpoint implementation. Please select `llm.provider=gemini`.";
    }

    @Override
    public String getProviderName() {
        return "OpenAI (" + modelName + ")";
    }
}
