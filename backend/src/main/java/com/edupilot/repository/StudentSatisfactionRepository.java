package com.edupilot.repository;

import com.edupilot.model.StudentSatisfaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentSatisfactionRepository extends MongoRepository<StudentSatisfaction, String> {
    List<StudentSatisfaction> findByStudentId(String studentId);
}
