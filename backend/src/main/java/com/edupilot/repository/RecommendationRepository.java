package com.edupilot.repository;

import com.edupilot.model.Recommendation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends MongoRepository<Recommendation, String> {
    List<Recommendation> findByUserId(String userId);
    List<Recommendation> findByUserIdAndStatus(String userId, Recommendation.Status status);
    List<Recommendation> findByUserIdAndStatusIn(String userId, List<Recommendation.Status> statuses);
    List<Recommendation> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, Recommendation.Status status);
    List<Recommendation> findByUserIdAndPriorityInAndStatus(String userId, List<Recommendation.Priority> priorities, Recommendation.Status status);
    Optional<Recommendation> findByUserIdAndSubjectCodeAndConceptNameAndStatus(String userId, String subjectCode, String conceptName, Recommendation.Status status);
    Optional<Recommendation> findFirstByUserIdAndSubjectNameAndConceptNameAndStatusIn(String userId, String subjectName, String conceptName, List<Recommendation.Status> statuses);
}
