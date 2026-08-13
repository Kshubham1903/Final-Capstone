package com.edupilot.repository;

import com.edupilot.model.DashboardTestResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardTestResultRepository extends MongoRepository<DashboardTestResult, String> {
    Optional<DashboardTestResult> findTopByStudentIdOrderByCreatedAtDesc(String studentId);
    List<DashboardTestResult> findByStudentId(String studentId);
}
