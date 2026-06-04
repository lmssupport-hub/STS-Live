package com.example.nexus.service;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.nexus.dto.LoginRequest;
import com.example.nexus.dto.UserResponseDTO;
import com.example.nexus.entity.User;
import com.example.nexus.repository.UserRepository;
import com.example.nexus.util.JwtUtil;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // ✅ BCrypt for password hashing
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public ResponseEntity<?> registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already exists"));
        }

        // ✅ Hash password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);

        // ✅ Return DTO, not raw entity (no password exposure)
        UserResponseDTO dto = new UserResponseDTO(
            savedUser.getId(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            savedUser.getEmail(),
            savedUser.getPhoneNumber()
        );

        return ResponseEntity.ok(Map.of(
            "message", "User registered successfully",
            "user", dto
        ));
    }

    public ResponseEntity<?> loginUser(LoginRequest loginRequest) {
        Optional<User> optionalUser = userRepository.findByEmail(loginRequest.getEmail());

        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Email does not exist"));
        }

        User user = optionalUser.get();

        // ✅ BCrypt comparison instead of plain text equals
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid Email or Password"));
        }

        String token = jwtUtil.generateToken(user.getEmail());

        UserResponseDTO userData = new UserResponseDTO(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getPhoneNumber()
        );

        return ResponseEntity.ok(Map.of(
            "message", "Login successfully",
            "token", token,
            "user", userData
        ));
    }
}