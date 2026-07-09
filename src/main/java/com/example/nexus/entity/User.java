package com.example.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(unique = true, nullable = false, length = 254)
    private String email;

    @Column(length = 15)
    private String phoneNumber;

    @Column(nullable = false)
    private String password; // ✅ stores BCrypt hash

    @Column(nullable = false)
    private boolean termsAccepted;

    // ── Forgot Password support ─────────────────────────────────────
    @Column(length = 10)
    private String resetOtp;
    private LocalDateTime resetOtpExpiry;

    @Column(length = 100)
    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int otpAttempts = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int resetSendCount = 0;

    @Column(nullable = false, length = 20)
    private String role = "ADMIN"; // default until role management is built

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Column
    private LocalDateTime lastLoginAt;

    @Column(name = "assigned_package_id")
    private Long assignedPackageId;

    @Column(name = "assigned_role_id")
    private Long assignedRoleId;

    // NEW — set only for users who registered through an Admin's invite link.
    // Lets an Admin's "Our Circle" list show exactly the members THEY invited.
    @Column(name = "created_by_admin_id")
    private Long createdByAdminId;


    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(boolean termsAccepted) { this.termsAccepted = termsAccepted; }

    public String getResetOtp() { return resetOtp; }
    public void setResetOtp(String resetOtp) { this.resetOtp = resetOtp; }

    public LocalDateTime getResetOtpExpiry() { return resetOtpExpiry; }
    public void setResetOtpExpiry(LocalDateTime resetOtpExpiry) { this.resetOtpExpiry = resetOtpExpiry; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }

    public int getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(int otpAttempts) { this.otpAttempts = otpAttempts; }

    public int getResetSendCount() { return resetSendCount; }
    public void setResetSendCount(int resetSendCount) { this.resetSendCount = resetSendCount; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Long getAssignedPackageId() { return assignedPackageId; }
    public void setAssignedPackageId(Long assignedPackageId) { this.assignedPackageId = assignedPackageId; }

    public Long getAssignedRoleId() { return assignedRoleId; }
    public void setAssignedRoleId(Long assignedRoleId) { this.assignedRoleId = assignedRoleId; }

    public Long getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(Long createdByAdminId) { this.createdByAdminId = createdByAdminId; }

}