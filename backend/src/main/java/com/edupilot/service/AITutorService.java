package com.edupilot.service;

import com.edupilot.dto.ChatRequest;
import com.edupilot.dto.ChatResponse;
import com.edupilot.model.AiConversation;
import com.edupilot.repository.AiConversationRepository;
import com.edupilot.service.llm.LLMProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AITutorService {

    @Value("${llm.provider:gemini}")
    private String llmProviderConfig;

    @Autowired
    private Map<String, LLMProvider> llmProviders;

    @Autowired
    private StudentContextService contextService;

    @Autowired
    private StudentContextBuilder contextBuilder;

    @Autowired
    private PromptBuilderService promptBuilderService;

    @Autowired
    private AiConversationRepository conversationRepository;

    public ChatResponse processChatMessage(ChatRequest req) {
        if (req == null || req.getStudentId() == null || req.getMessage() == null) {
            throw new IllegalArgumentException("Invalid chat request parameters.");
        }

        String conversationId = req.getConversationId();
        AiConversation conversation;

        if (conversationId != null && !conversationId.isBlank()) {
            Optional<AiConversation> opt = conversationRepository.findByConversationId(conversationId);
            if (opt.isPresent()) {
                conversation = opt.get();
            } else {
                conversation = createNewConversationEntity(req);
            }
        } else {
            conversation = createNewConversationEntity(req);
        }

        // 1. Append user message
        String userMsgId = "msg_u_" + UUID.randomUUID().toString().substring(0, 8);
        AiConversation.ChatMessage userChatMsg = new AiConversation.ChatMessage(
                userMsgId, "user", req.getMessage(), LocalDateTime.now()
        );
        conversation.getMessages().add(userChatMsg);

        // Update conversation learning mode if provided
        if (req.getLearningMode() != null) {
            conversation.setLearningMode(req.getLearningMode());
        }
        com.edupilot.model.LearningMode activeMode = conversation.getLearningMode() != null ? conversation.getLearningMode() : com.edupilot.model.LearningMode.LEARN;

        // 2. Build Context DTO & Structured System Prompt
        String concept = req.getReferencedConcept() != null ? req.getReferencedConcept() : conversation.getReferencedConcept();
        com.edupilot.dto.StudentContextDTO contextDTO = contextBuilder.buildCompleteContext(req.getStudentId(), concept);
        
        // Add sliding-window conversation memory transcript (up to last 6 messages, trimmed for input token efficiency)
        if (conversation.getMessages() != null && conversation.getMessages().size() > 1) {
            StringBuilder memorySb = new StringBuilder();
            var history = conversation.getMessages();
            int startIdx = Math.max(0, history.size() - 7);
            for (int i = startIdx; i < history.size() - 1; i++) {
                var m = history.get(i);
                String roleLabel = m.getRole().equalsIgnoreCase("user") ? "Student Question: " : "Tutor Key Points: ";
                String rawContent = m.getContent() != null ? m.getContent() : "";
                String trimmedContent = rawContent.length() > 400 ? rawContent.substring(0, 400) + "... [transcript compressed]" : rawContent;
                memorySb.append(roleLabel).append(trimmedContent).append("\n\n");
            }
            if (memorySb.length() > 0) {
                contextDTO.setConversationSummary(memorySb.toString());
            }
        }

        String systemPrompt = promptBuilderService.buildStructuredSystemPrompt(contextDTO, activeMode);
        Map<String, Object> contextMap = Map.of(
            "studentName", contextDTO.getStudentName(),
            "referencedConcept", concept != null ? concept : (contextDTO.getTodayFocusTask() != null ? contextDTO.getTodayFocusTask() : "General Studies"),
            "conversationId", conversation.getConversationId(),
            "learningMode", activeMode.name()
        );

        System.out.println("======== AI TUTOR REQUEST ========");
        System.out.println("Conversation ID: " + conversation.getConversationId());
        System.out.println("Learning Mode: " + activeMode);
        System.out.println("Student ID: " + req.getStudentId());
        System.out.println("User Question: " + req.getMessage());
        System.out.println("System Prompt Length: " + systemPrompt.length() + " chars");
        System.out.println("Thread History Messages Included: " + Math.max(0, conversation.getMessages().size() - 1));
        System.out.println("==================================");

        // 3. Select active LLM Provider
        LLMProvider provider = selectProvider();

        // 4. Generate AI response
        String aiTextResponse = provider.generateResponse(systemPrompt, req.getMessage(), contextMap);

        // 5. Append assistant message
        String assistantMsgId = "msg_a_" + UUID.randomUUID().toString().substring(0, 8);
        AiConversation.ChatMessage assistantChatMsg = new AiConversation.ChatMessage(
                assistantMsgId, "assistant", aiTextResponse, LocalDateTime.now()
        );
        conversation.getMessages().add(assistantChatMsg);
        conversation.setUpdatedAt(LocalDateTime.now());

        conversationRepository.save(conversation);

        return new ChatResponse(
                conversation.getConversationId(),
                assistantMsgId,
                "assistant",
                aiTextResponse,
                LocalDateTime.now()
        );
    }

    public LLMProvider getActiveProvider() {
        return selectProvider();
    }

    public String getActiveModelName() {
        LLMProvider provider = selectProvider();
        if (provider instanceof com.edupilot.service.llm.GeminiProvider) {
            return ((com.edupilot.service.llm.GeminiProvider) provider).getModelName();
        }
        return provider.getProviderName();
    }

    private LLMProvider selectProvider() {
        if ("openai".equalsIgnoreCase(llmProviderConfig) && llmProviders.containsKey("openAIProvider")) {
            return llmProviders.get("openAIProvider");
        }
        if (llmProviders.containsKey("geminiProvider")) {
            return llmProviders.get("geminiProvider");
        }
        return llmProviders.values().iterator().next();
    }

    private AiConversation createNewConversationEntity(ChatRequest req) {
        AiConversation conv = new AiConversation();
        conv.setConversationId("conv_" + UUID.randomUUID().toString().substring(0, 8));
        conv.setStudentId(req.getStudentId());
        conv.setTitle(req.getReferencedConcept() != null ? "Discussion: " + req.getReferencedConcept() : "AI Tutor Session");
        conv.setLearningPlanTaskId(req.getLearningPlanTaskId());
        conv.setReferencedConcept(req.getReferencedConcept());
        conv.setLearningMode(req.getLearningMode() != null ? req.getLearningMode() : com.edupilot.model.LearningMode.LEARN);
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());
        return conv;
    }
}
