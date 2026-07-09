package com.example.nexus.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class InviteDto {

    // Sent by the Admin from the "Invite peoples" modal
    public static class Request {
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        private String email;

        // Nullable — no role selected = "Categories: can access all public items"
        private Long roleId;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
    }

    // Returned to the register page so it can prefill/lock the email field
    public static class InviteInfo {
        private String email;
        private String roleName; // null if the invite carries no role

        public InviteInfo(String email, String roleName) {
            this.email = email;
            this.roleName = roleName;
        }

        public String getEmail() { return email; }
        public String getRoleName() { return roleName; }
    }
}