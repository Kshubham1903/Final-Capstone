package com.edupilot.repository;

import com.edupilot.model.LifestyleQuestionnaire;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LifestyleQuestionnaireRepository extends MongoRepository<LifestyleQuestionnaire, String> {
    Optional<LifestyleQuestionnaire> findByStudentProfileId(String studentProfileId);
}
