package com.example.nexus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class RoleDto {

    public static class Request {
        @NotBlank(message = "Role name is required")
        private String name;
        private String description;

        @NotNull(message = "Permissions are required")
        private List<PackageDto.Category> permissions;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<PackageDto.Category> getPermissions() { return permissions; }
        public void setPermissions(List<PackageDto.Category> permissions) { this.permissions = permissions; }
    }

    public static class Response {
        private Long id;
        private String name;
        private String description;
        private LocalDateTime createdAt;
        private List<PackageDto.Category> permissions;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public List<PackageDto.Category> getPermissions() { return permissions; }
        public void setPermissions(List<PackageDto.Category> permissions) { this.permissions = permissions; }
    }
}