package com.edupilot.repository;

import com.edupilot.model.AssessmentQuestion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentQuestionRepository extends MongoRepository<AssessmentQuestion, String> {
    List<AssessmentQuestion> findBySubjectCodeAndIsActiveTrue(String subjectCode);
    List<AssessmentQuestion> findByBranchAndSemesterAndIsActiveTrue(String branch, int semester);
    List<AssessmentQuestion> findByBranchAndSemesterAndSubjectCodeAndIsActiveTrue(String branch, int semester, String subjectCode);
}
