package com.example.nexus.service;

import com.example.nexus.dto.Taskdtos.*;
import com.example.nexus.entity.Project;
import com.example.nexus.entity.SubTask;
import com.example.nexus.entity.Task;
import com.example.nexus.entity.User;
import com.example.nexus.repository.ProjectRepository;
import com.example.nexus.repository.TaskRepository;
import com.example.nexus.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository    taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository    userRepository;

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository) {
        this.taskRepository    = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository    = userRepository;
    }

    // ── Get all tasks for a project ──────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get single task ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        return toResponse(findTaskOrThrow(id));
    }

    // ── Create task ──────────────────────────────────────────────────────
    @Transactional
    public TaskResponse createTask(CreateTaskRequest req) {

        Project project = findProjectOrThrow(req.projectId());
        User    user    = findActiveUserOrThrow(req.assignedUserId());

        validatePriority(req.priority());
        validateStartDate(req.startDate(), project);
        validateDueDate(req.startDate(), req.dueDate(), project);
        validateTargetCount(req.targetCount(), project);

        Task task = new Task();
        task.setProject(project);
        task.setAssignedUser(user);
        task.setTaskName(req.taskName());
        task.setDescription(req.description());
        task.setStartDate(req.startDate());
        task.setDueDate(req.dueDate());
        task.setTargetCount(req.targetCount());
        task.setPriority(req.priority());
        task.setStatus("Not Started");   // always forced on creation
        task.setSubTasks(new ArrayList<>());

        applySubTasks(task, req.subTasks());

        return toResponse(taskRepository.save(task));
    }

    // ── Update task ──────────────────────────────────────────────────────
    @Transactional
    public TaskResponse updateTask(Long id, CreateTaskRequest req) {

        Task    task    = findTaskOrThrow(id);
        Project project = findProjectOrThrow(req.projectId());
        User    user    = findActiveUserOrThrow(req.assignedUserId());

        validatePriority(req.priority());
        validateStatus(req.status());
        validateStartDate(req.startDate(), project);
        validateDueDate(req.startDate(), req.dueDate(), project);
        validateTargetCount(req.targetCount(), project);

        task.setProject(project);
        task.setAssignedUser(user);
        task.setTaskName(req.taskName());
        task.setDescription(req.description());
        task.setStartDate(req.startDate());
        task.setDueDate(req.dueDate());
        task.setTargetCount(req.targetCount());
        task.setPriority(req.priority());
        task.setStatus(req.status() != null ? req.status() : task.getStatus());

        // Replace sub-tasks completely
        task.getSubTasks().clear();
        applySubTasks(task, req.subTasks());

        return toResponse(taskRepository.save(task));
    }

    // ── Delete task ──────────────────────────────────────────────────────
    @Transactional
    public void deleteTask(Long id) {
        taskRepository.delete(findTaskOrThrow(id));
    }

    // ════════════════════════════════════════════════════════════════════
    //  Private helpers
    // ════════════════════════════════════════════════════════════════════

    private void applySubTasks(Task task, List<SubTaskRequest> requests) {
        if (requests == null) return;
        for (SubTaskRequest st : requests) {
            if (st.title() == null || st.title().isBlank()) continue;
            SubTask sub = new SubTask();
            sub.setTask(task);
            sub.setTitle(st.title().trim());
            sub.setDescription(st.description());
            task.getSubTasks().add(sub);
        }
    }

    private Task findTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));
    }

    private User findActiveUserOrThrow(Long userId) {
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        }

        return userRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No users found in database"));
    }

    // ── Validations ───────────────────────────────────────────────────────

    private void validatePriority(String priority) {
        if (!List.of("High", "Medium", "Low").contains(priority)) {
            throw new RuntimeException("Priority must be High, Medium, or Low");
        }
    }

    private void validateStatus(String status) {
        if (status != null && !List.of("Not Started", "In Progress", "Completed").contains(status)) {
            throw new RuntimeException("Status must be Not Started, In Progress, or Completed");
        }
    }

    private void validateStartDate(java.time.LocalDate startDate, Project project) {
        if (project.getStartDate() != null && startDate.isBefore(project.getStartDate())) {
            throw new RuntimeException(
                "Start Date must be greater than or equal to Project Start Date ("
                + project.getStartDate() + ")");
        }
    }

    private void validateDueDate(java.time.LocalDate startDate,
                                  java.time.LocalDate dueDate,
                                  Project project) {
        if (!dueDate.isAfter(startDate)) {
            throw new RuntimeException("Due Date must be greater than Start Date");
        }
        if (project.getDueDate() != null && dueDate.isAfter(project.getDueDate())) {
            throw new RuntimeException(
                "Due Date cannot exceed Project Due Date (" + project.getDueDate() + ")");
        }
    }

    private void validateTargetCount(Double targetCount, Project project) {
        if (project.getTarget() != null && targetCount > project.getTarget()) {
            throw new RuntimeException(
                "Target cannot exceed Project Target (" + project.getTarget() + ")");
        }
    }

    // ── Entity → Response ─────────────────────────────────────────────────

    private TaskResponse toResponse(Task t) {
        List<SubTaskResponse> subs = t.getSubTasks() == null ? List.of() :
                t.getSubTasks().stream()
                        .map(s -> new SubTaskResponse(
                                s.getId(),
                                s.getTitle(),
                                s.getDescription(),
                                s.getCreatedAt() != null ? s.getCreatedAt().toString() : null,
                                s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null))
                        .collect(Collectors.toList());

        return new TaskResponse(
                t.getId(),
                t.getProject().getId(),
                t.getTaskName(),
                t.getDescription(),
                t.getStartDate(),
                t.getDueDate(),
                t.getTargetCount(),
                t.getPriority(),
                t.getStatus(),
                t.getAssignedUser().getId(),
                t.getAssignedUser().getFirstName(),   // ← User.getUsername()
                subs,
                t.getCreatedAt() != null ? t.getCreatedAt().toString() : null,
                t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null
        );
    }
}