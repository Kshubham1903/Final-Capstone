package com.edupilot.repository;

import com.edupilot.model.LearningPreferenceQuestionnaire;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LearningPreferenceQuestionnaireRepository extends MongoRepository<LearningPreferenceQuestionnaire, String> {
    Optional<LearningPreferenceQuestionnaire> findByUserId(String userId);
}
