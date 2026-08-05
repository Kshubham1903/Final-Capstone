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
        // Infrastructure stub returning formatted AI response based on student context
        String concept = context != null && context.containsKey("referencedConcept") ? (String) context.get("referencedConcept") : "your learning plan topic";
        return "[OpenAI " + modelName + "] Hello! I am your AI Tutor. Regarding " + concept + ": " + userMessage + ". Let's break down this concept step-by-step so you can master it!";
    }

    @Override
    public String getProviderName() {
        return "OpenAI (" + modelName + ")";
    }
}
