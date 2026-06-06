package com.example.nexus.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public class Taskdtos {

    // ── Create / Update request ──────────────────────────────────────────
    public record CreateTaskRequest(

        @NotNull(message = "Project selection is required")
        Long projectId,

        @NotBlank(message = "Main Task Name is required")
        @Size(min = 3, message = "Main Task Name must be at least 3 characters")
        String taskName,

        @NotBlank(message = "Main Task Description is required")
        String description,

        @NotNull(message = "Start Date is required")
        LocalDate startDate,

        @NotNull(message = "Due Date is required")
        LocalDate dueDate,

        @NotNull(message = "Target Count is required")
        @DecimalMin(value = "0.1", message = "Value must be greater than 0")
        Double targetCount,

        @NotBlank(message = "Priority selection is required")
        String priority,

        // status: optional on creation (forced to "Not Started"), editable on update
        String status,

        
        Long assignedUserId,

        @Valid
        List<SubTaskRequest> subTasks

    ) {}

    // ── Sub-task nested in request ───────────────────────────────────────
    public record SubTaskRequest(

        @Size(min = 3, message = "Sub Task Name must be at least 3 characters")
        String title,

        String description

    ) {}

    // ── Task response ────────────────────────────────────────────────────
    public record TaskResponse(
        Long         id,
        Long         projectId,
        String       taskName,
        String       description,
        LocalDate    startDate,
        LocalDate    dueDate,
        Double       targetCount,
        String       priority,
        String       status,
        Long         assignedUserId,
        String       assignedUserName,
        List<SubTaskResponse> subTasks,
        String       createdAt,
        String       updatedAt
    ) {}

    // ── Sub-task response ────────────────────────────────────────────────
    public record SubTaskResponse(
        Long   id,
        String title,
        String description,
        String createdAt,
        String updatedAt
    ) {}
}