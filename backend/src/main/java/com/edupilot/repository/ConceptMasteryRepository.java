package com.edupilot.repository;

import com.edupilot.model.ConceptMastery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConceptMasteryRepository extends MongoRepository<ConceptMastery, String> {
    List<ConceptMastery> findByUserId(String userId);
    List<ConceptMastery> findByUserIdAndSubjectCode(String userId, String subjectCode);
    Optional<ConceptMastery> findByUserIdAndSubjectCodeAndTopicAndConceptName(String userId, String subjectCode, String topic, String conceptName);
}
