package com.edupilot.repository;

import com.edupilot.model.OnboardingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OnboardingStatusRepository extends MongoRepository<OnboardingStatus, String> {
    Optional<OnboardingStatus> findByUserId(String userId);
}
