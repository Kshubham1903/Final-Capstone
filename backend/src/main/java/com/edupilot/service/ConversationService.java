package com.edupilot.service;

import com.edupilot.dto.ConversationResponse;
import com.edupilot.model.AiConversation;
import com.edupilot.repository.AiConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    @Autowired
    private AiConversationRepository conversationRepository;

    public ConversationResponse createNewConversation(String studentId, String title, String taskId, String concept, com.edupilot.model.LearningMode mode) {
        String convId = "conv_" + UUID.randomUUID().toString().substring(0, 8);
        
        AiConversation conv = new AiConversation();
        conv.setConversationId(convId);
        conv.setStudentId(studentId);
        conv.setTitle(title != null && !title.isBlank() ? title : (concept != null ? "Discussion: " + concept : "New AI Tutor Discussion"));
        conv.setLearningPlanTaskId(taskId);
        conv.setReferencedConcept(concept);
        conv.setLearningMode(mode != null ? mode : com.edupilot.model.LearningMode.LEARN);
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());

        AiConversation saved = conversationRepository.save(conv);
        return new ConversationResponse(saved);
    }

    public ConversationResponse createNewConversation(String studentId, String title, String taskId, String concept) {
        return createNewConversation(studentId, title, taskId, concept, com.edupilot.model.LearningMode.LEARN);
    }

    public List<ConversationResponse> getStudentHistory(String studentId) {
        List<AiConversation> list = conversationRepository.findByStudentIdOrderByUpdatedAtDesc(studentId);
        if (list.isEmpty()) {
            ConversationResponse defaultConv = createNewConversation(studentId, "General AI Tutoring", null, "General Revision");
            return List.of(defaultConv);
        }
        return list.stream().map(ConversationResponse::new).collect(Collectors.toList());
    }

    public Optional<ConversationResponse> getConversationById(String conversationId) {
        return conversationRepository.findByConversationId(conversationId).map(ConversationResponse::new);
    }

    public boolean deleteConversation(String conversationId) {
        Optional<AiConversation> opt = conversationRepository.findByConversationId(conversationId);
        if (opt.isPresent()) {
            conversationRepository.delete(opt.get());
            return true;
        }
        return false;
    }
}
