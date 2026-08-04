package com.edupilot.config;

import com.edupilot.repository.StudentProfileRepository;
import com.edupilot.repository.QuizQuestionRepository;
import com.edupilot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Production Ready: Automatic database seed insertions removed.
        // Database starts cleanly with 0 pre-populated records.
        System.out.println(">>> Database initialization: Ready with clean empty database and BCrypt password support.");
    }
}
