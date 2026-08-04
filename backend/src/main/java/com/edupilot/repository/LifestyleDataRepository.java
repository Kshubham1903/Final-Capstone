package com.edupilot.repository;

import com.edupilot.model.LifestyleData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LifestyleDataRepository extends MongoRepository<LifestyleData, String> {
    List<LifestyleData> findByStudentProfileId(String studentProfileId);
    Optional<LifestyleData> findByStudentProfileIdAndDate(String studentProfileId, LocalDate date);
}
