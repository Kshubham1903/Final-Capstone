package com.edupilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class EduPilotApplication {
    public static void main(String[] args) {
        SpringApplication.run(EduPilotApplication.class, args);
    }
}
