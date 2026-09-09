package com.edupilot.repository;

import com.edupilot.model.RemediationSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RemediationSessionRepository extends MongoRepository<RemediationSession, String> {
    List<RemediationSession> findByStudentIdOrderByCreatedAtDesc(String studentId);
}
