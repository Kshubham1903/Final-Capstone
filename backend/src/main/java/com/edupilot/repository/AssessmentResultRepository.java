package com.edupilot.repository;

import com.edupilot.model.AssessmentResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentResultRepository extends MongoRepository<AssessmentResult, String> {
    List<AssessmentResult> findByUserId(String userId);
    List<AssessmentResult> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<AssessmentResult> findTopByUserIdOrderByCreatedAtDesc(String userId);
    Optional<AssessmentResult> findBySessionId(String sessionId);
}
