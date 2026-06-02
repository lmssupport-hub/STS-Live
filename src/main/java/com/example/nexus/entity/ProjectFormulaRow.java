package com.example.nexus.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "project_formula_rows")
public class ProjectFormulaRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String parameter1;
    private Double value1;
    private String operator;
    private String parameter2;
    private Double value2;

    @ManyToOne
    @JoinColumn(name = "project_id")
    @JsonIgnore
    private Project project;

    @OneToMany(mappedBy = "formulaRow", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectFormulaExtraField> extraFields = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getParameter1() {
        return parameter1;
    }

    public void setParameter1(String parameter1) {
        this.parameter1 = parameter1;
    }

    public Double getValue1() {
        return value1;
    }

    public void setValue1(Double value1) {
        this.value1 = value1;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getParameter2() {
        return parameter2;
    }

    public void setParameter2(String parameter2) {
        this.parameter2 = parameter2;
    }

    public Double getValue2() {
        return value2;
    }

    public void setValue2(Double value2) {
        this.value2 = value2;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public List<ProjectFormulaExtraField> getExtraFields() {
        return extraFields;
    }

    public void setExtraFields(List<ProjectFormulaExtraField> extraFields) {
        this.extraFields = extraFields;
    }
}