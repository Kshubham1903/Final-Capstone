package com.edupilot.repository;

import com.edupilot.model.StudentStateSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentStateSnapshotRepository extends MongoRepository<StudentStateSnapshot, String> {
    List<StudentStateSnapshot> findByStudentIdOrderByTimestampAsc(String studentId);
    Optional<StudentStateSnapshot> findTopByStudentIdOrderByTimestampDesc(String studentId);
}
