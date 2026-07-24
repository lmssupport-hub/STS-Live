package com.example.nexus.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.nexus.entity.Project;
import com.example.nexus.entity.ProjectFormulaExtraField;
import com.example.nexus.entity.ProjectFormulaRow;
import com.example.nexus.entity.User;
import com.example.nexus.repository.ProjectRepository;
import com.example.nexus.repository.UserRepository;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    // NEW — fires notifications when users get assigned to a project
    @Autowired
    private NotificationService notificationService;

    // ── Resolve which "team" the requester belongs to ───────────────
    // Admin/Super Admin → their own id. Member → the admin who invited them.
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

    public Project createProject(Project project, String requesterEmail) {
        project.setTeamAdminId(resolveTeamAdminId(requesterEmail));
        validateProject(project, null);

        double target = calculateTarget(project.getFormulaRows());
        project.setTarget(target);

        for (ProjectFormulaRow row : project.getFormulaRows()) {
            row.setProject(project);
            for (ProjectFormulaExtraField extra : row.getExtraFields()) {
                extra.setFormulaRow(row);
            }
        }

        Project saved = projectRepository.save(project);

        // NEW — notify every assigned user that they've been added to this project
        notifyAssignedUsers(saved, saved.getAssignedUsers());

        return saved;
    }

    public List<Project> getAllProjects(String requesterEmail) {
        return projectRepository.findByTeamAdminId(resolveTeamAdminId(requesterEmail));
    }

    public Project getProjectById(Long id, String requesterEmail) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getTeamAdminId().equals(resolveTeamAdminId(requesterEmail))) {
            throw new RuntimeException("You don't have access to this project");
        }
        return project;
    }

    public Project updateProject(Long id, Project updatedProject, String requesterEmail) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (!existing.getTeamAdminId().equals(resolveTeamAdminId(requesterEmail))) {
            throw new RuntimeException("You don't have access to this project");
        }

        validateProject(updatedProject, id);

        double target = calculateTarget(updatedProject.getFormulaRows());

        // NEW — remember who was assigned before the update so we only notify
        // users that are newly added, not everyone on every edit.
        Set<String> previouslyAssigned = existing.getAssignedUsers() == null
                ? new HashSet<>()
                : new HashSet<>(existing.getAssignedUsers());

        existing.setProjectName(updatedProject.getProjectName());
        existing.setProjectDescription(updatedProject.getProjectDescription());
        existing.setProjectReceivedDate(updatedProject.getProjectReceivedDate());
        existing.setStartDate(updatedProject.getStartDate());
        existing.setDueDate(updatedProject.getDueDate());
        existing.setTarget(target);
        existing.setAssignedUsers(updatedProject.getAssignedUsers());

        existing.getFormulaRows().clear();
        projectRepository.saveAndFlush(existing);

        for (ProjectFormulaRow row : updatedProject.getFormulaRows()) {
            row.setId(null);
            row.setProject(existing);
            for (ProjectFormulaExtraField extra : row.getExtraFields()) {
                extra.setId(null);
                extra.setFormulaRow(row);
            }
            existing.getFormulaRows().add(row);
        }

        Project saved = projectRepository.save(existing);

        // NEW — notify only the newly-added assigned users
        List<String> newlyAssigned = saved.getAssignedUsers() == null
                ? List.of()
                : saved.getAssignedUsers().stream()
                        .filter(email -> !previouslyAssigned.contains(email))
                        .toList();
        notifyAssignedUsers(saved, newlyAssigned);

        return saved;
    }

    public void deleteProject(Long id, String requesterEmail) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!existing.getTeamAdminId().equals(resolveTeamAdminId(requesterEmail))) {
            throw new RuntimeException("You don't have access to this project");
        }
        projectRepository.deleteById(id);
    }

    // NEW — sends one "New Instruction" notification per assigned user email
    private void notifyAssignedUsers(Project project, List<String> assignedUserEmails) {
        if (assignedUserEmails == null || assignedUserEmails.isEmpty()) return;

        String title = "New Instruction: " + project.getProjectName();
        String message = "You have been assigned to the project \"" + project.getProjectName()
                + "\". Due date: " + project.getDueDate() + ".";

        for (String email : assignedUserEmails) {
            notificationService.notifyUserByEmail(
                    email, title, message, "New Instruction", "PROJECT", project.getId());
        }
    }

    private void validateProject(Project project, Long currentId) {
        if (isBlank(project.getProjectName())) {
            throw new RuntimeException("Project Name is required");
        }

        String trimmedName = project.getProjectName().trim();

        boolean nameExists = (currentId == null)
                ? projectRepository.existsByProjectNameIgnoreCase(trimmedName)
                : projectRepository.existsByProjectNameIgnoreCaseAndIdNot(trimmedName, currentId);

        if (nameExists) {
            throw new RuntimeException("Project with the same name already exists");
        }

        if (project.getProjectReceivedDate() == null) {
            throw new RuntimeException("Project Received Date is required");
        }
        if (project.getStartDate() == null) {
            throw new RuntimeException("Start Date is required");
        }
        if (project.getDueDate() == null) {
            throw new RuntimeException("Due Date is required");
        }
        if (project.getProjectReceivedDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("Project Received Date cannot be a future date");
        }
        if (project.getStartDate().isBefore(project.getProjectReceivedDate())) {
            throw new RuntimeException("Start Date must be >= Project Received Date");
        }
        if (project.getDueDate().isBefore(project.getStartDate())) {
            throw new RuntimeException("Due Date must be >= Start Date");
        }
        if (project.getAssignedUsers() == null || project.getAssignedUsers().isEmpty()) {
            throw new RuntimeException("Assign User is required");
        }

        validateDescription(project.getProjectDescription());
        validateFormulaRows(project.getFormulaRows());
    }

    private void validateDescription(String description) {
        if (isBlank(description)) return;
        String[] words = description.trim().split("\\s+");
        if (words.length > 500) {
            throw new RuntimeException("Project Description must not exceed 500 words");
        }
    }

    private void validateFormulaRows(List<ProjectFormulaRow> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new RuntimeException("Formula is required");
        }
        for (ProjectFormulaRow row : rows) {
            if (isBlank(row.getParameter1()))
                throw new RuntimeException("Formula Parameter 1 selection is required");
            if (!isPositive(row.getValue1()))
                throw new RuntimeException("Parameter 1 Value must be greater than 0");
            if (!isValidOperator(row.getOperator()))
                throw new RuntimeException("Formula Operator selection is required");
            if (isBlank(row.getParameter2()))
                throw new RuntimeException("Formula Parameter 2 selection is required");
            if (!isPositive(row.getValue2()))
                throw new RuntimeException("Parameter 2 Value must be greater than 0");
            validateExtraFields(row.getExtraFields());
        }
    }

    private void validateExtraFields(List<ProjectFormulaExtraField> extraFields) {
        if (extraFields == null) return;
        for (ProjectFormulaExtraField extra : extraFields) {
            if (isBlank(extra.getType()))
                throw new RuntimeException("Formula extra field type is required");
            if ("operator".equalsIgnoreCase(extra.getType())) {
                if (!isValidOperator(extra.getOperator()))
                    throw new RuntimeException("Formula Operator selection is required");
            } else if ("parameter".equalsIgnoreCase(extra.getType())) {
                if (isBlank(extra.getParameter()))
                    throw new RuntimeException("Formula Parameter selection is required");
                if (!isPositive(extra.getValue()))
                    throw new RuntimeException("Parameter Value must be greater than 0");
            } else {
                throw new RuntimeException("Invalid formula extra field type");
            }
        }
    }

    private double calculateTarget(List<ProjectFormulaRow> rows) {
        double total = 0;
        for (ProjectFormulaRow row : rows) {
            double rowValue = applyOperator(row.getValue1(), row.getOperator(), row.getValue2());
            String pendingOperator = "";
            if (row.getExtraFields() != null) {
                for (ProjectFormulaExtraField extra : row.getExtraFields()) {
                    if ("operator".equalsIgnoreCase(extra.getType())) {
                        pendingOperator = extra.getOperator();
                    }
                    if ("parameter".equalsIgnoreCase(extra.getType())) {
                        if (isBlank(pendingOperator))
                            throw new RuntimeException("Operator required before extra parameter");
                        rowValue = applyOperator(rowValue, pendingOperator, extra.getValue());
                        pendingOperator = "";
                    }
                }
            }
            if (!isBlank(pendingOperator))
                throw new RuntimeException("Parameter required after extra operator");
            total += rowValue;
        }
        return Math.round(total * 100.0) / 100.0;
    }

    private double applyOperator(double value1, String operator, double value2) {
        if ("+".equals(operator)) return value1 + value2;
        if ("-".equals(operator)) return value1 - value2;
        if ("×".equals(operator) || "*".equals(operator)) return value1 * value2;
        if ("÷".equals(operator) || "/".equals(operator)) {
            if (value2 <= 0) throw new RuntimeException("Division value must be > 0");
            return value1 / value2;
        }
        throw new RuntimeException("Unsupported operator");
    }

    private boolean isValidOperator(String op) {
        return "+".equals(op) || "-".equals(op) || "×".equals(op)
                || "÷".equals(op) || "*".equals(op) || "/".equals(op);
    }

    private boolean isPositive(Double value) {
        return value != null && value > 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}