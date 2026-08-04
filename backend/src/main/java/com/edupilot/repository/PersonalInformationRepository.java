package com.edupilot.repository;

import com.edupilot.model.PersonalInformation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonalInformationRepository extends MongoRepository<PersonalInformation, String> {
    Optional<PersonalInformation> findByUserId(String userId);
}
