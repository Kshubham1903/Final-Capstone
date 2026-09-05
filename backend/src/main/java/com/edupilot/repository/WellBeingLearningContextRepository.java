package com.edupilot.repository;

import com.edupilot.model.WellBeingLearningContext;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WellBeingLearningContextRepository extends MongoRepository<WellBeingLearningContext, String> {
    List<WellBeingLearningContext> findAllByUserIdOrderByUpdatedAtDesc(String userId);

    default Optional<WellBeingLearningContext> findLatestByUserId(String userId) {
        List<WellBeingLearningContext> list = findAllByUserIdOrderByUpdatedAtDesc(userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
