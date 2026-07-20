package com.example.nexus.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.nexus.dto.ForgotPasswordRequest;
import com.example.nexus.dto.LoginRequest;
import com.example.nexus.dto.ResetPasswordRequest;
import com.example.nexus.dto.VerifyOtpRequest;
import com.example.nexus.entity.User;
import com.example.nexus.service.UserService;
import com.example.nexus.util.JwtUtil;
@RestController
@RequestMapping("/api/users")
@CrossOrigin("*")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user,
                                           @RequestParam(required = false) String inviteToken) {
        return userService.registerUser(user, inviteToken);
    }
    @GetMapping
    public ResponseEntity<?> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token);   // ✅ fixed
        return userService.getAllUsers(email);
    }
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        return userService.loginUser(loginRequest);
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return userService.forgotPassword(request);
    }
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        return userService.verifyOtp(request);
    }
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }
    
    @GetMapping("/team-members")
    public ResponseEntity<?> getTeamMembers(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token);
        return userService.getTeamMembers(email);
    }
    
}