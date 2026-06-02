package com.example.nexus.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.nexus.entity.Project;
import com.example.nexus.entity.ProjectFormulaExtraField;
import com.example.nexus.entity.ProjectFormulaRow;
import com.example.nexus.repository.ProjectRepository;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    public Project createProject(Project project) {
        validateProject(project);

        double target = calculateTarget(project.getFormulaRows());
        project.setTarget(target);

        for (ProjectFormulaRow row : project.getFormulaRows()) {
            row.setProject(project);

            for (ProjectFormulaExtraField extra : row.getExtraFields()) {
                extra.setFormulaRow(row);
            }
        }

        return projectRepository.save(project);
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Project getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
    }

    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    private void validateProject(Project project) {
        if (isBlank(project.getProjectName())) {
            throw new RuntimeException("Project Name is required");
        }

        if (projectRepository.existsByProjectNameIgnoreCase(project.getProjectName().trim())) {
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
            throw new RuntimeException("Start Date must be greater than or equal to Project Received Date");
        }

        if (project.getDueDate().isBefore(project.getStartDate())) {
            throw new RuntimeException("Due Date must be greater than or equal to Start Date");
        }

        if (project.getAssignedUsers() == null || project.getAssignedUsers().isEmpty()) {
            throw new RuntimeException("Assign User is required");
        }

        validateDescription(project.getProjectDescription());
        validateFormulaRows(project.getFormulaRows());
    }

    private void validateDescription(String description) {
        if (isBlank(description)) {
            return;
        }

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
            if (isBlank(row.getParameter1())) {
                throw new RuntimeException("Formula Parameter 1 selection is required");
            }

            if (!isPositive(row.getValue1())) {
                throw new RuntimeException("Parameter 1 Value must be greater than 0");
            }

            if (!isValidOperator(row.getOperator())) {
                throw new RuntimeException("Formula Operator selection is required");
            }

            if (isBlank(row.getParameter2())) {
                throw new RuntimeException("Formula Parameter 2 selection is required");
            }

            if (!isPositive(row.getValue2())) {
                throw new RuntimeException("Parameter 2 Value must be greater than 0");
            }

            validateExtraFields(row.getExtraFields());
        }
    }

    private void validateExtraFields(List<ProjectFormulaExtraField> extraFields) {
        if (extraFields == null) {
            return;
        }

        for (ProjectFormulaExtraField extra : extraFields) {
            if (isBlank(extra.getType())) {
                throw new RuntimeException("Formula extra field type is required");
            }

            if ("operator".equalsIgnoreCase(extra.getType())) {
                if (!isValidOperator(extra.getOperator())) {
                    throw new RuntimeException("Formula Operator selection is required");
                }
            } else if ("parameter".equalsIgnoreCase(extra.getType())) {
                if (isBlank(extra.getParameter())) {
                    throw new RuntimeException("Formula Parameter selection is required");
                }

                if (!isPositive(extra.getValue())) {
                    throw new RuntimeException("Parameter Value must be greater than 0");
                }
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
                        if (isBlank(pendingOperator)) {
                            throw new RuntimeException("Operator is required before extra parameter");
                        }

                        rowValue = applyOperator(rowValue, pendingOperator, extra.getValue());
                        pendingOperator = "";
                    }
                }
            }

            if (!isBlank(pendingOperator)) {
                throw new RuntimeException("Parameter is required after extra operator");
            }

            total += rowValue;
        }

        return Math.round(total * 100.0) / 100.0;
    }

    private double applyOperator(double value1, String operator, double value2) {
        if ("+".equals(operator)) {
            return value1 + value2;
        }

        if ("-".equals(operator)) {
            return value1 - value2;
        }

        if ("×".equals(operator) || "*".equals(operator)) {
            return value1 * value2;
        }

        if ("÷".equals(operator) || "/".equals(operator)) {
            if (value2 <= 0) {
                throw new RuntimeException("Division value must be greater than 0");
            }

            return value1 / value2;
        }

        throw new RuntimeException("Selected formula combination is not supported");
    }

    private boolean isValidOperator(String operator) {
        return "+".equals(operator)
                || "-".equals(operator)
                || "×".equals(operator)
                || "÷".equals(operator)
                || "*".equals(operator)
                || "/".equals(operator);
    }

    private boolean isPositive(Double value) {
        return value != null && value > 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}