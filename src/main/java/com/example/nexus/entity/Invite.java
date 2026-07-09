package com.example.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "invites")
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    // Nullable — an invite with no role means "no elevated access, default only"
    @Column(name = "role_id")
    private Long roleId;

    // Whoever clicked "Send Invite" — the new user's records get stamped with this,
    // so they show up under that specific Admin's Our Circle list, not everyone's.
    @Column(name = "invited_by_admin_id", nullable = false)
    private Long invitedByAdminId;

    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }

    public Long getInvitedByAdminId() { return invitedByAdminId; }
    public void setInvitedByAdminId(Long invitedByAdminId) { this.invitedByAdminId = invitedByAdminId; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}