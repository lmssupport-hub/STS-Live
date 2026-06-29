package com.example.nexus.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.nexus.dto.ForgotPasswordRequest;
import com.example.nexus.dto.LoginRequest;
import com.example.nexus.dto.ResetPasswordRequest;
import com.example.nexus.dto.UserResponseDTO;
import com.example.nexus.dto.VerifyOtpRequest;
import com.example.nexus.entity.User;
import com.example.nexus.repository.UserRepository;
import com.example.nexus.util.JwtUtil;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    // ✅ BCrypt for password hashing
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ── Configurable thresholds from the validation spec (all marked TBD there) ──
    private static final int OTP_LENGTH = 4;                 // spec: 4–6 digits, TBD
    private static final int OTP_VALIDITY_MINUTES = 5;        // spec: e.g. 5 minutes, TBD
    private static final int MAX_OTP_VERIFY_ATTEMPTS = 5;     // spec: e.g. 5 attempts, TBD
    private static final int MAX_OTP_SEND_ATTEMPTS = 4;       // spec: e.g. 3 *resends*, TBD (1 initial + 3 resends)
    private static final int RESET_TOKEN_VALIDITY_MINUTES = 10;

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,16}$"
    );

    public ResponseEntity<?> registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already exists"));
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);

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

    // ── Forgot Password: Step 1 — send / resend OTP ──────────────────────────
    public ResponseEntity<?> forgotPassword(ForgotPasswordRequest request) {
        String rawEmail = request.getEmail() == null ? "" : request.getEmail().trim();

        if (rawEmail.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email is required"));
        }
        if (rawEmail.length() > 254) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Enter a valid email address"));
        }

        // ✅ spec: "Email validation should be case-insensitive"
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(rawEmail);
        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Email ID not registered"));
        }

        User user = optionalUser.get();

     // ✅ spec: "Maximum resend attempts should be limited"
        if (user.getResetSendCount() >= MAX_OTP_SEND_ATTEMPTS) {
            // ✅ Auto-reset after 24 hours
            boolean canReset = user.getResetOtpExpiry() == null ||
                LocalDateTime.now().isAfter(user.getResetOtpExpiry().plusHours(24));

            if (canReset) {
                user.setResetSendCount(0);
                user.setOtpAttempts(0);
                user.setResetOtp(null);
                user.setResetOtpExpiry(null);
            } else {
                return ResponseEntity
                        .status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("message", "Maximum attempts reached. Please try again after 24 hours."));
            }
        }

        String otp = generateOtp();
        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        user.setOtpAttempts(0); // ✅ fresh attempt counter for the new OTP
        user.setResetSendCount(user.getResetSendCount() + 1);
        userRepository.save(user);

        try {
            emailService.sendOtpEmail(user.getEmail(), otp);
        } catch (Exception e) {
            System.err.println("❌ Failed to send OTP email: " + e.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to send OTP email: " + e.getMessage()));
        }

        return ResponseEntity.ok(Map.of("message", "OTP sent to your email"));
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int value = new Random().nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", value);
    }

    // ── Forgot Password: Step 1b — verify OTP, issue a reset token ───────────
    public ResponseEntity<?> verifyOtp(VerifyOtpRequest request) {
        String rawEmail = request.getEmail() == null ? "" : request.getEmail().trim();
        String otp = request.getOtp() == null ? "" : request.getOtp().trim();

        if (otp.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "OTP is required"));
        }
        if (!otp.matches("\\d{" + OTP_LENGTH + "}")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid OTP"));
        }

        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(rawEmail);
        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Email ID not registered"));
        }

        User user = optionalUser.get();

        if (user.getResetOtp() == null
                || user.getResetOtpExpiry() == null
                || LocalDateTime.now().isAfter(user.getResetOtpExpiry())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "OTP expired. Please resend."));
        }

        if (!user.getResetOtp().equals(otp)) {
            // ✅ spec: "System should allow limited OTP verification attempts"
            int attempts = user.getOtpAttempts() + 1;
            user.setOtpAttempts(attempts);

            if (attempts >= MAX_OTP_VERIFY_ATTEMPTS) {
                // Invalidate the OTP so it can't be brute-forced further; user must resend
                user.setResetOtp(null);
                user.setResetOtpExpiry(null);
                userRepository.save(user);
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Maximum OTP attempts exceeded. Please resend OTP."));
            }

            userRepository.save(user);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid OTP"));
        }

        // ✅ Success — issue a one-time reset token, clear OTP + counters
        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(RESET_TOKEN_VALIDITY_MINUTES));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        user.setOtpAttempts(0);
        user.setResetSendCount(0);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "OTP verified",
            "resetToken", resetToken
        ));
    }

    // ── Forgot Password: Step 2 — set the new password ───────────────────────
    public ResponseEntity<?> resetPassword(ResetPasswordRequest request) {
        String resetToken = request.getResetToken();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        if (resetToken == null || resetToken.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid or expired reset link"));
        }

        Optional<User> optionalUser = userRepository.findByResetToken(resetToken);
        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Invalid or expired reset link"));
        }

        User user = optionalUser.get();

        if (user.getResetTokenExpiry() == null || LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Reset link expired. Please start again."));
        }

        if (newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "New Password is required"));
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Confirm Password is required"));
        }

        // ✅ spec: 8–16 chars, upper+lower+digit+special, "Spaces should not be allowed"
        if (newPassword.contains(" ") || !STRONG_PASSWORD.matcher(newPassword).matches()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Password does not meet password policy requirements"));
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Password does not match"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setOtpAttempts(0);
        user.setResetSendCount(0);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
