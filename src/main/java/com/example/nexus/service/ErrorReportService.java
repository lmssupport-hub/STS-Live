package com.example.nexus.service;

import com.example.nexus.dto.ErrorDtos.*;
import com.example.nexus.entity.ErrorReport;
import com.example.nexus.entity.ErrorReport.ErrorStatus;
import com.example.nexus.entity.ErrorReport.Priority;
import com.example.nexus.entity.Project;
import com.example.nexus.entity.Task;
import com.example.nexus.entity.User;
import com.example.nexus.exception.BadRequestException;
import com.example.nexus.exception.ConflictException;
import com.example.nexus.exception.ResourceNotFoundException;
import com.example.nexus.repository.ErrorReportRepository;
import com.example.nexus.repository.ProjectRepository;
import com.example.nexus.repository.TaskRepository;
import com.example.nexus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ErrorReportService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    @Value("${app.upload.dir:uploads/error-screenshots}")
    private String uploadDir;

    @Autowired
    private ErrorReportRepository errorReportRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService; // NEW

    // ── Resolve which "team" the requester belongs to ───────────────
    private Long resolveTeamAdminId(String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid session"));
        if ("ADMIN".equals(requester.getRole()) || "SUPER_ADMIN".equals(requester.getRole())) {
            return requester.getId();
        }
        if (requester.getCreatedByAdminId() == null) {
            throw new BadRequestException("Your account is not linked to a team yet");
        }
        return requester.getCreatedByAdminId();
    }

    private void assertProjectInTeam(Project project, Long teamAdminId) {
        if (!project.getTeamAdminId().equals(teamAdminId)) {
            throw new BadRequestException("You don't have access to this project");
        }
    }

    private void assertErrorInTeam(ErrorReport error, Long teamAdminId) {
        assertProjectInTeam(error.getProject(), teamAdminId);
    }

    // ── CREATE (Field 14) ─────────────────────────────────────────────────
    @Transactional
    public ErrorResponse createError(CreateErrorRequest request, MultipartFile screenshot, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));
        assertProjectInTeam(project, teamAdminId);

        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + request.getTaskId()));

        if (task.getProject() == null || !task.getProject().getId().equals(project.getId())) {
            throw new BadRequestException("Selected task does not belong to the selected project");
        }

        User assignedUser = task.getAssignedUser();
        if (assignedUser == null) {
            throw new BadRequestException("Selected task has no assigned user; cannot auto-populate Assign User");
        }

        ErrorReport error = new ErrorReport();
        error.setProject(project);
        error.setTask(task);
        error.setPageTitle(request.getPageTitle());
        error.setErrorDescription(request.getErrorDescription());
        error.setExpectedResult(request.getExpectedResult());
        error.setPriority(request.getPriority());
        error.setComments(request.getComments());
        error.setAssignedUser(assignedUser);
        error.setStatus(ErrorStatus.Open);

        if (screenshot != null && !screenshot.isEmpty()) {
            String path = storeScreenshot(screenshot);
            error.setScreenshotPath(path);
            error.setScreenshotName(screenshot.getOriginalFilename());
        }

        ErrorReport saved = errorReportRepository.save(error);

        // NEW — notify the assigned user that an error has landed on them
        notifyErrorAssignment(saved);

        return toResponse(saved);
    }

    // ── LIST (Field 1 & 2: search + filter) ─────────────────────────────
    public List<ErrorResponse> getErrors(String keyword, String status, String priority,
                                          Long assignedUserId, Long projectId, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);

        ErrorStatus statusEnum = parseEnumOrNull(status, ErrorStatus.class, "Status");
        Priority priorityEnum = parseEnumOrNull(priority, Priority.class, "Priority");

        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();

        List<ErrorReport> results = errorReportRepository.searchAndFilter(
                normalizedKeyword, statusEnum, priorityEnum, assignedUserId, projectId, teamAdminId);

        return results.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── GET BY ID (Field 13: Show More) ─────────────────────────────────
    public ErrorResponse getErrorById(Long id, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);
        ErrorReport error = findOrThrow(id);
        assertErrorInTeam(error, teamAdminId);
        return toResponse(error);
    }

    // ── UPDATE (Field 15) ────────────────────────────────────────────────
    @Transactional
    public ErrorResponse updateError(Long id, UpdateErrorRequest request, MultipartFile screenshot, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);
        ErrorReport error = findOrThrow(id);
        assertErrorInTeam(error, teamAdminId);

        if (!error.getVersion().equals(request.getVersion())) {
            throw new ConflictException(
                    "This error was updated by another user. Please refresh and try again.");
        }

        // NEW — remember who was assigned before the update
        Long previousAssignedUserId = error.getAssignedUser() != null
                ? error.getAssignedUser().getId()
                : null;

        if (request.getProjectId() != null && !request.getProjectId().equals(error.getProject().getId())) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));
            assertProjectInTeam(project, teamAdminId);
            error.setProject(project);
        }

        if (request.getTaskId() != null && !request.getTaskId().equals(error.getTask().getId())) {
            Task task = taskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + request.getTaskId()));

            if (!task.getProject().getId().equals(error.getProject().getId())) {
                throw new BadRequestException("Selected task does not belong to the selected project");
            }
            error.setTask(task);
            error.setAssignedUser(task.getAssignedUser());
        }

        error.setPageTitle(request.getPageTitle());
        error.setErrorDescription(request.getErrorDescription());
        error.setExpectedResult(request.getExpectedResult());
        error.setPriority(request.getPriority());
        error.setComments(request.getComments());

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            error.setStatus(parseEnumOrThrow(request.getStatus(), ErrorStatus.class, "Status"));
        }

        if (screenshot != null && !screenshot.isEmpty()) {
            validateScreenshot(screenshot);
            deleteScreenshot(error.getScreenshotPath());
            String newPath = storeScreenshot(screenshot);
            error.setScreenshotPath(newPath);
            error.setScreenshotName(screenshot.getOriginalFilename());
        }

        ErrorReport saved = errorReportRepository.save(error);

        // NEW — only notify if the error's assigned user actually changed
        Long newAssignedUserId = saved.getAssignedUser() != null ? saved.getAssignedUser().getId() : null;
        if (!Objects.equals(previousAssignedUserId, newAssignedUserId)) {
            notifyErrorAssignment(saved);
        }

        return toResponse(saved);
    }

    // ── UPDATE STATUS (Field 11 quick action) ────────────────────────────
    @Transactional
    public ErrorResponse updateStatus(Long id, UpdateStatusRequest request, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);
        ErrorReport error = findOrThrow(id);
        assertErrorInTeam(error, teamAdminId);

        if (!error.getVersion().equals(request.getVersion())) {
            throw new ConflictException(
                    "This error was updated by another user. Please refresh and try again.");
        }

        ErrorStatus newStatus = parseEnumOrThrow(request.getStatus(), ErrorStatus.class, "Status");
        error.setStatus(newStatus);
        ErrorReport saved = errorReportRepository.save(error);
        return toResponse(saved);
    }

    // ── DELETE (Field 14, S#14) ───────────────────────────────────────────
    @Transactional
    public void deleteError(Long id, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);
        ErrorReport error = findOrThrow(id);
        assertErrorInTeam(error, teamAdminId);
        deleteScreenshot(error.getScreenshotPath());
        errorReportRepository.delete(error);
    }

    // ── SCREENSHOT ACCESS ─────────────────────────────────────────────────
    public ErrorReport getErrorForScreenshot(Long id, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);
        ErrorReport error = findOrThrow(id);
        assertErrorInTeam(error, teamAdminId);
        return error;
    }

    // ════════════════════════════════════════════════════════════════════
    //  NEW — Notification helper
    // ════════════════════════════════════════════════════════════════════

    private void notifyErrorAssignment(ErrorReport error) {
        User assignee = error.getAssignedUser();
        if (assignee == null) return;

        String title = "New Error Assigned: " + error.getPageTitle();
        String message = "An error has been reported on \"" + error.getPageTitle()
                + "\" in task \"" + error.getTask().getTaskName()
                + "\". Priority: " + error.getPriority() + ".";

        notificationService.notifyUser(
                assignee.getId(), title, message, "New Instruction", "ERROR", error.getId());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private ErrorReport findOrThrow(Long id) {
        return errorReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Error report not found with id: " + id));
    }

    private <E extends Enum<E>> E parseEnumOrNull(String value, Class<E> enumClass, String fieldLabel) {
        if (value == null || value.isBlank()) return null;
        return parseEnumOrThrow(value, enumClass, fieldLabel);
    }

    private <E extends Enum<E>> E parseEnumOrThrow(String value, Class<E> enumClass, String fieldLabel) {
        try {
            String normalized = value.trim().replace(" ", "_");
            return Enum.valueOf(enumClass, normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid " + fieldLabel + " value: " + value);
        }
    }

    private void validateScreenshot(MultipartFile file) {
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("Invalid file format or file size exceeds 5 MB");
        }
        String extension = getExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BadRequestException("Invalid file format or file size exceeds 5 MB");
        }
    }

    private String storeScreenshot(MultipartFile file) {
        validateScreenshot(file);
        try {
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            String extension = getExtension(file.getOriginalFilename());
            String storedFileName = UUID.randomUUID() + "." + extension;
            Path targetPath = dirPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString();
        } catch (IOException e) {
            throw new BadRequestException("Unable to store screenshot file: " + e.getMessage());
        }
    }

    private void deleteScreenshot(String filePath) {
        if (filePath == null) return;
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException ignored) {
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    // ── Response mapping ──────────────────────────────────────────────────

    private ErrorResponse toResponse(ErrorReport e) {
        ErrorResponse dto = new ErrorResponse();
        dto.setId(e.getId());

        dto.setProjectId(e.getProject().getId());
        dto.setProjectName(e.getProject().getProjectName());

        dto.setTaskId(e.getTask().getId());
        dto.setTaskName(e.getTask().getTaskName());

        dto.setPageTitle(e.getPageTitle());
        dto.setErrorDescription(e.getErrorDescription());
        dto.setExpectedResult(e.getExpectedResult());
        dto.setPriority(e.getPriority() != null ? e.getPriority().name() : null);

        dto.setScreenshotUrl(e.getScreenshotPath() != null ? "/api/errors/" + e.getId() + "/screenshot" : null);
        dto.setScreenshotName(e.getScreenshotName());

        if (e.getAssignedUser() != null) {
            dto.setAssignedUserId(e.getAssignedUser().getId());
            dto.setAssignedUserName(resolveUserDisplayName(e.getAssignedUser()));
        }

        dto.setStatus(e.getStatus() != null ? e.getStatus().name().replace("_", " ") : null);
        dto.setComments(e.getComments());
        dto.setCreatedDate(e.getCreatedDate());
        dto.setUpdatedAt(e.getUpdatedAt());
        dto.setVersion(e.getVersion());

        return dto;
    }

    private String resolveUserDisplayName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? "Unknown" : fullName;
    }
}