package com.edupilot.repository;

import com.edupilot.model.DashboardTestSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DashboardTestSessionRepository extends MongoRepository<DashboardTestSession, String> {
    List<DashboardTestSession> findByStudentId(String studentId);
    List<DashboardTestSession> findTop5ByStudentIdOrderByCreatedAtDesc(String studentId);
}
