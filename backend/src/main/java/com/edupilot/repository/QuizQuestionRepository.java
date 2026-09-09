package com.edupilot.repository;

import com.edupilot.model.ModuleType;
import com.edupilot.model.QuizQuestion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends MongoRepository<QuizQuestion, String> {
    List<QuizQuestion> findBySubject(String subject);
    List<QuizQuestion> findBySubjectAndDifficulty(String subject, QuizQuestion.Difficulty difficulty);
    List<QuizQuestion> findBySubjectAndModuleSource(String subject, ModuleType moduleSource);
    List<QuizQuestion> findBySubjectAndModuleSourceIn(String subject, List<ModuleType> moduleSources);
    List<QuizQuestion> findBySubjectAndConcept(String subject, String concept);
}
