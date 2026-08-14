package com.edupilot.repository;

import com.edupilot.model.AdaptiveSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdaptiveSessionRepository extends MongoRepository<AdaptiveSession, String> {
    Optional<AdaptiveSession> findByIdAndUserId(String id, String userId);
    List<AdaptiveSession> findByUserIdAndSubjectCodeAndStatus(String userId, String subjectCode, AdaptiveSession.Status status);
    List<AdaptiveSession> findTop5ByUserIdAndSubjectNameOrderByStartTimeDesc(String userId, String subjectName);
}
