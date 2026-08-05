package com.edupilot.repository;

import com.edupilot.model.Subject;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends MongoRepository<Subject, String> {
    List<Subject> findByIsActiveTrue();
    List<Subject> findByBranchAndIsActiveTrue(String branch);
    List<Subject> findByBranchAndSemesterAndIsActiveTrue(String branch, int semester);
    Optional<Subject> findBySubjectCode(String subjectCode);
}
