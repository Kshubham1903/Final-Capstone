package com.edupilot.repository;

import com.edupilot.model.AcademicProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicProfileRepository extends MongoRepository<AcademicProfile, String> {
    Optional<AcademicProfile> findByUserId(String userId);
}
