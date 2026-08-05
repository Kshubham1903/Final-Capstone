package com.edupilot.repository;

import com.edupilot.model.StudySession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudySessionRepository extends MongoRepository<StudySession, String> {
    List<StudySession> findByUserIdOrderByStartTimeDesc(String userId);
    Optional<StudySession> findByUserIdAndTaskIdAndStatus(String userId, String taskId, StudySession.SessionStatus status);
}
