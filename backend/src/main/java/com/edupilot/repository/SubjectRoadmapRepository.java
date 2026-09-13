package com.edupilot.repository;

import com.edupilot.model.SubjectRoadmap;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRoadmapRepository extends MongoRepository<SubjectRoadmap, String> {
    Optional<SubjectRoadmap> findByUserIdAndSubjectCode(String userId, String subjectCode);
    Optional<SubjectRoadmap> findByUserIdAndSubjectCodeAndStatus(String userId, String subjectCode, SubjectRoadmap.RoadmapStatus status);
    List<SubjectRoadmap> findByUserId(String userId);
    boolean existsByUserIdAndSubjectCode(String userId, String subjectCode);
}
