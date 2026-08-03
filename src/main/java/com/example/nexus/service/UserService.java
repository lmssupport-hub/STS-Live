package com.example.nexus.service;
import java.util.List;          
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
import com.example.nexus.entity.Invite;
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

    @Autowired
    private InviteService inviteService;

    @Autowired
    private RoleService roleService; // used for the Authentication category login gate

   
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    
    private static final int OTP_LENGTH = 4;                 
    private static final int OTP_VALIDITY_MINUTES = 5;        
    private static final int MAX_OTP_VERIFY_ATTEMPTS = 5;     
    private static final int MAX_OTP_SEND_ATTEMPTS = 4;       
    private static final int RESET_TOKEN_VALIDITY_MINUTES = 10;

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,16}$"
    );

    
    public ResponseEntity<?> registerUser(User user, String inviteToken) {
    	if (userRepository.existsByEmailIgnoreCase(user.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email already exists"));
        }

        Invite invite = null;
        if (inviteToken != null && !inviteToken.isBlank()) {
            try {
                invite = inviteService.validateToken(inviteToken);
            } catch (Exception e) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", e.getMessage()));
            }

           
            if (!invite.getEmail().equalsIgnoreCase(user.getEmail())) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "This invite link was sent to a different email address"));
            }
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (invite != null) {
            user.setCreatedByAdminId(invite.getInvitedByAdminId());
            user.setAssignedRoleId(invite.getRoleId());
            user.setRole("MEMBER"); 
        }

        User savedUser = userRepository.save(user);

        if (invite != null) {
            inviteService.markUsed(invite);
        }

        UserResponseDTO dto = toDto(savedUser);
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

        // ── Access-gate: block login if the account has no permission source yet ──
        // SUPER_ADMIN never needs a check, ella screenum full access.
        if ("ADMIN".equals(user.getRole()) && user.getAssignedPackageId() == null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No package has been assigned to your account yet. Contact your Super Admin."));
        }
        if (!"SUPER_ADMIN".equals(user.getRole())
                && !"ADMIN".equals(user.getRole())
                && user.getAssignedRoleId() == null) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No role has been assigned to your account yet. Contact your Admin."));
        }

        // ── Access-gate: "Authentication" category toggle must be ON in the
        //    assigned package/role, or login itself is blocked (even with correct password).
        if (!roleService.hasAuthenticationAccess(user.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Login access has been disabled for your account. Contact your Admin."));
        }

      
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        UserResponseDTO userData = toDto(user);
        return ResponseEntity.ok(Map.of(
            "message", "Login successfully",
            "token", token,
            "user", userData
        ));
    }

 
    public ResponseEntity<?> getAllUsers(String requesterEmail) {
        Optional<User> requesterOpt = userRepository.findByEmail(requesterEmail);
        if (requesterOpt.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid session"));
        }

        User requester = requesterOpt.get();

        List<User> targetUsers;
        if ("SUPER_ADMIN".equals(requester.getRole())) {
            targetUsers = userRepository.findByRole("ADMIN");  
        } else if ("ADMIN".equals(requester.getRole())) {
           
            targetUsers = userRepository.findByCreatedByAdminId(requester.getId());
        } else {
            targetUsers = List.of();
        }

        var users = targetUsers.stream()
            .map(this::toDto)
            .toList();
        return ResponseEntity.ok(users);
    }
    
    
    public ResponseEntity<?> deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    private UserResponseDTO toDto(User u) {
        return new UserResponseDTO(
            u.getId(),
            u.getFirstName(),
            u.getLastName(),
            u.getEmail(),
            u.getPhoneNumber(),
            u.getRole(),
            u.getCreatedAt(),
            u.getLastLoginAt(),
            u.getAssignedPackageId(),
            u.getAssignedRoleId(),
            u.getCreatedByAdminId()
        );
    }

    
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

        
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(rawEmail);
        if (optionalUser.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Email ID not registered"));
        }

        User user = optionalUser.get();

     
        if (user.getResetSendCount() >= MAX_OTP_SEND_ATTEMPTS) {
        
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
        user.setOtpAttempts(0); 
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
           
            int attempts = user.getOtpAttempts() + 1;
            user.setOtpAttempts(attempts);

            if (attempts >= MAX_OTP_VERIFY_ATTEMPTS) {
                
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
    
    
    
    public ResponseEntity<?> getTeamMembers(String requesterEmail) {
        Optional<User> requesterOpt = userRepository.findByEmail(requesterEmail);
        if (requesterOpt.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid session"));
        }

        User requester = requesterOpt.get();

        Long teamAdminId = ("ADMIN".equals(requester.getRole()) || "SUPER_ADMIN".equals(requester.getRole()))
                ? requester.getId()
                : requester.getCreatedByAdminId();

        if (teamAdminId == null) {
            return ResponseEntity.ok(List.of());
        }

        List<User> team = new java.util.ArrayList<>();
        userRepository.findById(teamAdminId).ifPresent(team::add);
        team.addAll(userRepository.findByCreatedByAdminId(teamAdminId));

        var result = team.stream()
                .distinct()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(result);
    }
    
    
    
    
}