package com.edupilot.repository;

import com.edupilot.model.AiConversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiConversationRepository extends MongoRepository<AiConversation, String> {
    Optional<AiConversation> findByConversationId(String conversationId);
    List<AiConversation> findByStudentIdOrderByUpdatedAtDesc(String studentId);
}
