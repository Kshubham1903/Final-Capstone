package com.edupilot.repository;

import com.edupilot.model.Recommendation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecommendationRepository extends MongoRepository<Recommendation, String> {
    List<Recommendation> findByUserIdAndStatus(String userId, Recommendation.Status status);
    List<Recommendation> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, Recommendation.Status status);
    List<Recommendation> findByUserIdAndPriorityInAndStatus(String userId, List<Recommendation.Priority> priorities, Recommendation.Status status);
    Optional<Recommendation> findByUserIdAndSubjectCodeAndConceptNameAndStatus(String userId, String subjectCode, String conceptName, Recommendation.Status status);
    List<Recommendation> findByUserIdAndSubjectCodeAndStatus(String userId, String subjectCode, Recommendation.Status status);
}
