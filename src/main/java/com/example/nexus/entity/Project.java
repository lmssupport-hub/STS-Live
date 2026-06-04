package com.example.nexus.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectName;

    @Column(length = 5000)
    private String projectDescription;

    private LocalDate projectReceivedDate;
    private LocalDate startDate;
    private LocalDate dueDate;

    private Double target;

    @ElementCollection
    @CollectionTable(name = "project_assigned_users", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "assigned_user")
    private List<String> assignedUsers = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectFormulaRow> formulaRows = new ArrayList<>();

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public LocalDate getProjectReceivedDate() {
        return projectReceivedDate;
    }

    public void setProjectReceivedDate(LocalDate projectReceivedDate) {
        this.projectReceivedDate = projectReceivedDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Double getTarget() {
        return target;
    }

    public void setTarget(Double target) {
        this.target = target;
    }

    public List<String> getAssignedUsers() {
        return assignedUsers;
    }

    public void setAssignedUsers(List<String> assignedUsers) {
        this.assignedUsers = assignedUsers;
    }

    public List<ProjectFormulaRow> getFormulaRows() {
        return formulaRows;
    }

    public void setFormulaRows(List<ProjectFormulaRow> formulaRows) {
        this.formulaRows = formulaRows;
    }
}