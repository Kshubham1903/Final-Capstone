package com.edupilot.controller;

import com.edupilot.model.User;
import com.edupilot.repository.UserRepository;
import com.edupilot.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is already taken."));
        }
        
        user.setCreatedAt(LocalDateTime.now());
        // Secure BCrypt password hashing before database persistence
        user.setPassword(passwordEncoder.encode(user.getPassword()));
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
        if (userOpt.isEmpty() || !passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid email or password."));
        }
        
        User user = userOpt.get();
        // Generate real HMAC-SHA256 signed JWT token
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("userId", user.getId());
        
        return ResponseEntity.ok(response);
    }
}
