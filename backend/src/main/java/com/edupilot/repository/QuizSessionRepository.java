package com.edupilot.repository;

import com.edupilot.model.QuizSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizSessionRepository extends MongoRepository<QuizSession, String> {
    List<QuizSession> findByUserId(String userId);
    List<QuizSession> findByStudentProfileId(String studentProfileId);
    List<QuizSession> findByUserIdOrderByLastAnswerTimeDesc(String userId);
    Optional<QuizSession> findFirstByUserIdOrderByLastAnswerTimeDesc(String userId);
    Optional<QuizSession> findFirstByUserIdAndSubjectNameAndStatusOrderByLastAnswerTimeDesc(String userId, String subjectName, QuizSession.Status status);
}
