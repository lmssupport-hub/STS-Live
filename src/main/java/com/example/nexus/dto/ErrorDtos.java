package com.example.nexus.dto;

import com.example.nexus.entity.ErrorReport.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * All Error Report DTOs grouped in one file, same convention as Taskdtos.java.
 * Usage: import com.example.nexus.dto.ErrorDtos.*;
 */
public class ErrorDtos {

    private ErrorDtos() {
    }

    // ── Create Error (Field 14: Add Error) ──────────────────────────────
    public static class CreateErrorRequest {

        @NotNull(message = "Project is required")
        private Long projectId;

        @NotNull(message = "Task is required")
        private Long taskId;

        @NotBlank(message = "Page title/page no is required")
        private String pageTitle;

        @NotBlank(message = "Error description is required")
        @Size(max = 1000, message = "Error description must not exceed 1000 characters")
        private String errorDescription;

        @NotBlank(message = "Expected result is required")
        @Size(max = 1000, message = "Expected result must not exceed 1000 characters")
        private String expectedResult;

        @NotNull(message = "Priority is required")
        private Priority priority;

        @Size(max = 500, message = "Comments must not exceed 500 characters")
        private String comments;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }

        public String getPageTitle() { return pageTitle; }
        public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }

        public String getErrorDescription() { return errorDescription; }
        public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }

        public String getExpectedResult() { return expectedResult; }
        public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }

        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }

        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }

    // ── Update Error (Field 15: full edit from expanded row) ────────────
    public static class UpdateErrorRequest {

        private Long projectId;
        private Long taskId;

        @NotBlank(message = "Page title/page no is required")
        private String pageTitle;

        @NotBlank(message = "Error description is required")
        @Size(max = 1000, message = "Error description must not exceed 1000 characters")
        private String errorDescription;

        @NotBlank(message = "Expected result is required")
        @Size(max = 1000, message = "Expected result must not exceed 1000 characters")
        private String expectedResult;

        @NotNull(message = "Priority is required")
        private Priority priority;

        @Size(max = 500, message = "Comments must not exceed 500 characters")
        private String comments;

        // Optional inline status change from the same edit form
        private String status;

        // Edge Case #4: client must send back the version it last fetched.
        // If it no longer matches the DB row, the save is rejected with a 409 Conflict
        // instead of silently overwriting another user's changes.
        @NotNull(message = "Version is required for update")
        private Long version;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }

        public String getPageTitle() { return pageTitle; }
        public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }

        public String getErrorDescription() { return errorDescription; }
        public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }

        public String getExpectedResult() { return expectedResult; }
        public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }

        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }

        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Long getVersion() { return version; }
        public void setVersion(Long version) { this.version = version; }
    }

    // ── Quick Status Update (Field 11) ──────────────────────────────────
    public static class UpdateStatusRequest {

        @NotBlank(message = "Error status is required")
        private String status;

        // Edge Case #4 applies here too — quick status patch also needs version check
        @NotNull(message = "Version is required for update")
        private Long version;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Long getVersion() { return version; }
        public void setVersion(Long version) { this.version = version; }
    }

    // ── Response (Error List grid — collapsed + expanded view) ──────────
    public static class ErrorResponse {

        private Long id;

        private Long projectId;
        private String projectName;

        private Long taskId;
        private String taskName;

        private String pageTitle;
        private String errorDescription;
        private String expectedResult;
        private String priority;

        private String screenshotUrl;
        private String screenshotName;

        private Long assignedUserId;
        private String assignedUserName;

        private String status;
        private String comments;

        private LocalDate createdDate;
        private LocalDateTime updatedAt;
        private Long version;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }

        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }

        public String getPageTitle() { return pageTitle; }
        public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }

        public String getErrorDescription() { return errorDescription; }
        public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }

        public String getExpectedResult() { return expectedResult; }
        public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getScreenshotUrl() { return screenshotUrl; }
        public void setScreenshotUrl(String screenshotUrl) { this.screenshotUrl = screenshotUrl; }

        public String getScreenshotName() { return screenshotName; }
        public void setScreenshotName(String screenshotName) { this.screenshotName = screenshotName; }

        public Long getAssignedUserId() { return assignedUserId; }
        public void setAssignedUserId(Long assignedUserId) { this.assignedUserId = assignedUserId; }

        public String getAssignedUserName() { return assignedUserName; }
        public void setAssignedUserName(String assignedUserName) { this.assignedUserName = assignedUserName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }

        public LocalDate getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        public Long getVersion() { return version; }
        public void setVersion(Long version) { this.version = version; }
    }
}
