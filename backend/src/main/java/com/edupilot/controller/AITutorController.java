package com.edupilot.controller;

import com.edupilot.dto.ChatRequest;
import com.edupilot.dto.ChatResponse;
import com.edupilot.dto.ConversationResponse;
import com.edupilot.service.AITutorService;
import com.edupilot.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AITutorController {

    @Autowired
    private AITutorService aiTutorService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private com.edupilot.service.StudentContextBuilder contextBuilder;

    @GetMapping("/context/{studentId}")
    public ResponseEntity<com.edupilot.dto.StudentContextDTO> getStudentContext(
            @PathVariable String studentId,
            @RequestParam(required = false) String concept) {
        return ResponseEntity.ok(contextBuilder.buildCompleteContext(studentId, concept));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> processChat(@RequestBody ChatRequest req) {
        return ResponseEntity.ok(aiTutorService.processChatMessage(req));
    }

    @PostMapping("/new-conversation")
    public ResponseEntity<ConversationResponse> createNewConversation(
            @RequestParam String studentId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String concept,
            @RequestParam(required = false) com.edupilot.model.LearningMode mode) {
        return ResponseEntity.ok(conversationService.createNewConversation(studentId, title, taskId, concept, mode));
    }

    @GetMapping("/history/{studentId}")
    public ResponseEntity<List<ConversationResponse>> getConversationHistory(@PathVariable String studentId) {
        return ResponseEntity.ok(conversationService.getStudentHistory(studentId));
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getConversationById(@PathVariable String conversationId) {
        return conversationService.getConversationById(conversationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<?> deleteConversation(@PathVariable String conversationId) {
        boolean deleted = conversationService.deleteConversation(conversationId);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Conversation deleted successfully."));
        }
        return ResponseEntity.notFound().build();
    }
}
