package com.edupilot.config;

import com.edupilot.repository.*;
import com.edupilot.service.LegacyAccountClassificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private StudentProfileRepository profileRepository;

    @Autowired
    private QuizQuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LegacyAccountClassificationService legacyClassificationService;

    @Override
    public void run(String... args) throws Exception {
        // Production Ready: Automatic database seed insertions removed.
        // Runs idempotent legacy student classification on startup for legacy unclassified accounts.
        int updated = legacyClassificationService.classifyLegacyStudentAccounts();
        System.out.println(">>> Database initialization: Ready with clean empty database support. Legacy accounts classified: " + updated);
    }
}
