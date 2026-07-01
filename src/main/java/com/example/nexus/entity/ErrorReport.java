package com.example.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "error_reports")
public class ErrorReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Field 3: Project Name (Dropdown, Mandatory)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Field 4: Task Name (Dropdown, Mandatory, loaded based on Project)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    // Field 5: Page Title / Page No (Text Input, Mandatory)
    @Column(name = "page_title", nullable = false)
    private String pageTitle;

    // Field 6: Error Description (Text Area, Mandatory, max 1000 chars)
    @Column(name = "error_description", nullable = false, length = 1000)
    private String errorDescription;

    // Field 7: Expected Result / Expected Behavior (Text Area, Mandatory, max 1000 chars)
    @Column(name = "expected_result", nullable = false, length = 1000)
    private String expectedResult;

    // Field 8: Priority (Dropdown, Mandatory: Low, Medium, High, Critical)
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority;

    // Field 9: Screenshot (File Upload, Optional, JPG/PNG/JPEG max 5MB)
    @Column(name = "screenshot_path")
    private String screenshotPath;

    @Column(name = "screenshot_name")
    private String screenshotName;

    // Field 10: Assign User (Read-only, auto-populated from Task's assigned user)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id", nullable = false)
    private User assignedUser;

    // Field 11: Status (Dropdown, Mandatory: Open, In Progress, Resolved, Closed) Default Open
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ErrorStatus status = ErrorStatus.Open;

    // Field 12: Comments (Text Area, Optional, max 500 chars)
    @Column(name = "comments", length = 500)
    private String comments;

    // Field 17: Created Date (Date, Mandatory, DD/MM/YYYY)
    @Column(name = "created_date", updatable = false, nullable = false)
    private LocalDate createdDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Edge Case #4: optimistic locking — guards against two users
    // updating the same error at the same time. JPA auto-increments this
    // on every save and throws ObjectOptimisticLockingFailureException
    // if the row was changed by someone else in between.
    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.createdDate == null) this.createdDate = LocalDate.now();
        if (this.status == null) this.status = ErrorStatus.Open;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Enums ────────────────────────────────────────────────────────────
    public enum Priority {
        Low, Medium, High, Critical
    }

    public enum ErrorStatus {
        Open, In_Progress, Resolved, Closed
    }

    // ── Getters & Setters ───────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public Task getTask() { return task; }
    public void setTask(Task task) { this.task = task; }

    public String getPageTitle() { return pageTitle; }
    public void setPageTitle(String pageTitle) { this.pageTitle = pageTitle; }

    public String getErrorDescription() { return errorDescription; }
    public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }

    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public String getScreenshotPath() { return screenshotPath; }
    public void setScreenshotPath(String screenshotPath) { this.screenshotPath = screenshotPath; }

    public String getScreenshotName() { return screenshotName; }
    public void setScreenshotName(String screenshotName) { this.screenshotName = screenshotName; }

    public User getAssignedUser() { return assignedUser; }
    public void setAssignedUser(User assignedUser) { this.assignedUser = assignedUser; }

    public ErrorStatus getStatus() { return status; }
    public void setStatus(ErrorStatus status) { this.status = status; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
