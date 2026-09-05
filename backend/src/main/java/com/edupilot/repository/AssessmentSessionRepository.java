package com.edupilot.repository;

import com.edupilot.model.AssessmentSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentSessionRepository extends MongoRepository<AssessmentSession, String> {
    List<AssessmentSession> findByUserId(String userId);
    Optional<AssessmentSession> findTopByUserIdOrderByStartTimeDesc(String userId);
    List<AssessmentSession> findTop5ByUserIdAndSubjectNameOrderByStartTimeDesc(String userId, String subjectName);
}
