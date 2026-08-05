package com.edupilot.repository;

import com.edupilot.model.KnowledgeProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KnowledgeProfileRepository extends MongoRepository<KnowledgeProfile, String> {
    Optional<KnowledgeProfile> findByUserId(String userId);
}
