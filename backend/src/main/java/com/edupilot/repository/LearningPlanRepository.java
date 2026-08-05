package com.edupilot.repository;

import com.edupilot.model.LearningPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LearningPlanRepository extends MongoRepository<LearningPlan, String> {
    Optional<LearningPlan> findByUserIdAndPlanDate(String userId, LocalDate planDate);
    List<LearningPlan> findByUserIdAndPlanDateBetweenOrderByPlanDateAsc(String userId, LocalDate startDate, LocalDate endDate);
    List<LearningPlan> findByUserIdOrderByPlanDateDesc(String userId);
}
