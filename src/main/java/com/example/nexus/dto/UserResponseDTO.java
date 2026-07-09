package com.example.nexus.dto;

import java.time.LocalDateTime;

public class UserResponseDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private Long assignedPackageId;
    private Long assignedRoleId;      // NEW
    private Long createdByAdminId;    // NEW

    public UserResponseDTO(Long id, String firstName, String lastName,
                           String email, String phoneNumber,
                           String role, LocalDateTime createdAt, LocalDateTime lastLoginAt,
                           Long assignedPackageId, Long assignedRoleId, Long createdByAdminId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.assignedPackageId = assignedPackageId;
        this.assignedRoleId = assignedRoleId;
        this.createdByAdminId = createdByAdminId;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public Long getAssignedPackageId() { return assignedPackageId; }
    public Long getAssignedRoleId() { return assignedRoleId; }
    public Long getCreatedByAdminId() { return createdByAdminId; }
}