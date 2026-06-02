package com.example.nexus.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "project_formula_extra_fields")
public class ProjectFormulaExtraField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
    private String parameter;
    private String operator;
    private Double value;

    @ManyToOne
    @JoinColumn(name = "formula_row_id")
    @JsonIgnore
    private ProjectFormulaRow formulaRow;

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public ProjectFormulaRow getFormulaRow() {
        return formulaRow;
    }

    public void setFormulaRow(ProjectFormulaRow formulaRow) {
        this.formulaRow = formulaRow;
    }
}