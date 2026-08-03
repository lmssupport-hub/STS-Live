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
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository    taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository    userRepository;
    private final NotificationService notificationService; // NEW

    public TaskService(TaskRepository taskRepository,
                       ProjectRepository projectRepository,
                       UserRepository userRepository,
                       NotificationService notificationService) {
        this.taskRepository    = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository    = userRepository;
        this.notificationService = notificationService;
    }

    // ── Resolve which "team" the requester belongs to ───────────────
    private Long resolveTeamAdminId(String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("Invalid session"));
        if ("ADMIN".equals(requester.getRole()) || "SUPER_ADMIN".equals(requester.getRole())) {
            return requester.getId();
        }
        if (requester.getCreatedByAdminId() == null) {
            throw new RuntimeException("Your account is not linked to a team yet");
        }
        return requester.getCreatedByAdminId();
    }

    private void assertProjectInTeam(Project project, Long teamAdminId) {
        if (!project.getTeamAdminId().equals(teamAdminId)) {
            throw new RuntimeException("You don't have access to this project's tasks");
        }
    }

    private void assertTaskInTeam(Task task, Long teamAdminId) {
        assertProjectInTeam(task.getProject(), teamAdminId);
    }

    // ── Get all tasks for a project ──────────────────────────────────────
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long projectId, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);
        Project project = findProjectOrThrow(projectId);
        assertProjectInTeam(project, teamAdminId);

        return taskRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Get single task ──────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);
        Task task = findTaskOrThrow(id);
        assertTaskInTeam(task, teamAdminId);
        return toResponse(task);
    }

    // ── Create task ──────────────────────────────────────────────────────
    @Transactional
    public TaskResponse createTask(CreateTaskRequest req, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);

        Project project = findProjectOrThrow(req.projectId());
        assertProjectInTeam(project, teamAdminId);

        User user = findActiveUserOrThrow(req.assignedUserId(), teamAdminId);

        validatePriority(req.priority());
        validateStartDate(req.startDate(), project);
        validateDueDate(req.startDate(), req.dueDate(), project);
        validateTargetCount(req.targetCount(), project);
        validateDuplicateTaskName(req.projectId(), req.taskName(), null); // ✅ FIX (Main Task Name-06)

        Task task = new Task();
        task.setProject(project);
        task.setAssignedUser(user);
        task.setTaskName(req.taskName());
        task.setDescription(req.description());
        task.setStartDate(req.startDate());
        task.setDueDate(req.dueDate());
        task.setTargetCount(req.targetCount());
        task.setPriority(req.priority());
        task.setStatus("Not Started");
        task.setSubTasks(new ArrayList<>());

        applySubTasks(task, req.subTasks());

        Task saved = taskRepository.save(task);

        // NEW — notify the assigned user that a new task has landed on them
        notifyTaskAssignment(saved);

        return toResponse(saved);
    }

    // ── Update task ──────────────────────────────────────────────────────
    @Transactional
    public TaskResponse updateTask(Long id, CreateTaskRequest req, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);

        Task task = findTaskOrThrow(id);
        assertTaskInTeam(task, teamAdminId);

        Project project = findProjectOrThrow(req.projectId());
        assertProjectInTeam(project, teamAdminId);

        User user = findActiveUserOrThrow(req.assignedUserId(), teamAdminId);

        validatePriority(req.priority());
        validateStatus(req.status());
        validateStartDate(req.startDate(), project);
        validateDueDate(req.startDate(), req.dueDate(), project);
        validateTargetCount(req.targetCount(), project);
        validateDuplicateTaskName(req.projectId(), req.taskName(), id); // ✅ FIX (Main Task Name-06)

        // NEW — remember who had this task before the update
        Long previousAssignedUserId = task.getAssignedUser() != null
                ? task.getAssignedUser().getId()
                : null;

        task.setProject(project);
        task.setAssignedUser(user);
        task.setTaskName(req.taskName());
        task.setDescription(req.description());
        task.setStartDate(req.startDate());
        task.setDueDate(req.dueDate());
        task.setTargetCount(req.targetCount());
        task.setPriority(req.priority());
        task.setStatus(req.status() != null ? req.status() : task.getStatus());

        task.getSubTasks().clear();
        applySubTasks(task, req.subTasks());

        Task saved = taskRepository.save(task);

        // NEW — only notify if the task was (re)assigned to a different person
        if (!Objects.equals(previousAssignedUserId, user.getId())) {
            notifyTaskAssignment(saved);
        }

        return toResponse(saved);
    }

    // ── Delete task ──────────────────────────────────────────────────────
    @Transactional
    public void deleteTask(Long id, String requesterEmail) {
        Long teamAdminId = resolveTeamAdminId(requesterEmail);
        Task task = findTaskOrThrow(id);
        assertTaskInTeam(task, teamAdminId);
        taskRepository.delete(task);
    }

    // ════════════════════════════════════════════════════════════════════
    //  Private helpers
    // ════════════════════════════════════════════════════════════════════

    // NEW — sends a "New Instruction" notification to the task's assigned user
    private void notifyTaskAssignment(Task task) {
        User assignee = task.getAssignedUser();
        if (assignee == null) return;

        String title = "New Instruction: " + task.getTaskName();
        String message = "You have been assigned the task \"" + task.getTaskName()
                + "\" in project \"" + task.getProject().getProjectName()
                + "\". Due date: " + task.getDueDate() + ".";

        notificationService.notifyUser(
                assignee.getId(), title, message, "New Instruction", "TASK", task.getId());
    }

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

    // ✅ UPDATED — assignedUserId, given if not, must be team's own admin;
    // never picks an arbitrary user from the whole DB anymore.
    private User findActiveUserOrThrow(Long userId, Long teamAdminId) {
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            Long userTeamId = ("ADMIN".equals(user.getRole()) || "SUPER_ADMIN".equals(user.getRole()))
                    ? user.getId()
                    : user.getCreatedByAdminId();
            if (userTeamId == null || !userTeamId.equals(teamAdminId)) {
                throw new RuntimeException("Assigned user does not belong to your team");
            }
            return user;
        }

        return userRepository.findById(teamAdminId)
                .orElseThrow(() -> new RuntimeException("Team admin not found"));
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

    // ✅ FIX (Main Task Name-06): block duplicate Main Task Name within the same project.
    // taskIdToExclude = null on CREATE, current task's id on UPDATE (so a task
    // isn't flagged as a duplicate of itself when its own name is unchanged).
    private void validateDuplicateTaskName(Long projectId, String taskName, Long taskIdToExclude) {
        String trimmedName = taskName == null ? "" : taskName.trim();
        boolean duplicateExists = (taskIdToExclude == null)
                ? taskRepository.existsByProject_IdAndTaskNameIgnoreCase(projectId, trimmedName)
                : taskRepository.existsByProject_IdAndTaskNameIgnoreCaseAndIdNot(projectId, trimmedName, taskIdToExclude);

        if (duplicateExists) {
            throw new RuntimeException("A task with this name already exists in this project");
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
                t.getAssignedUser().getFirstName(),
                subs,
                t.getCreatedAt() != null ? t.getCreatedAt().toString() : null,
                t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null
        );
    }
}