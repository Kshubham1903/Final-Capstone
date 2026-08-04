package com.edupilot.controller;

import com.edupilot.model.User;
import com.edupilot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already taken."));
        }
        
        user.setCreatedAt(LocalDateTime.now());
        // In production, encrypt the password before saving
        User saved = userRepository.save(user);
        
        return ResponseEntity.ok(Map.of(
            "message", "Registration successful",
            "userId", saved.getId(),
            "role", saved.getRole()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty() || !userOpt.get().getPassword().equals(password)) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password."));
        }
        
        User user = userOpt.get();
        // Generate mockup token (we will use proper JWT configuration in security setup)
        String mockToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mockTokenForEduPilot." + user.getId();
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", mockToken);
        response.put("role", user.getRole());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("userId", user.getId());
        
        return ResponseEntity.ok(response);
    }
}
