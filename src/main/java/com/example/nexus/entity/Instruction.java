package com.example.nexus.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "instructions")
public class Instruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private String status;

    private LocalDate effectiveDate;

    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;

    @Column(name = "team_admin_id", nullable = false)
    private Long teamAdminId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @ElementCollection
    @CollectionTable(name = "instruction_target_users", joinColumns = @JoinColumn(name = "instruction_id"))
    @Column(name = "target_user")
    private List<String> targetUsersOrTeams = new ArrayList<>();

    @OneToMany(mappedBy = "instruction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InstructionDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "instruction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InstructionAcknowledgement> acknowledgements = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public Long getTeamAdminId() { return teamAdminId; }
    public void setTeamAdminId(Long teamAdminId) { this.teamAdminId = teamAdminId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<String> getTargetUsersOrTeams() { return targetUsersOrTeams; }
    public void setTargetUsersOrTeams(List<String> targetUsersOrTeams) { this.targetUsersOrTeams = targetUsersOrTeams; }
    public List<InstructionDocument> getDocuments() { return documents; }
    public void setDocuments(List<InstructionDocument> documents) { this.documents = documents; }
    public List<InstructionAcknowledgement> getAcknowledgements() { return acknowledgements; }
    public void setAcknowledgements(List<InstructionAcknowledgement> acknowledgements) { this.acknowledgements = acknowledgements; }

    @Entity
    @Table(name = "instruction_documents")
    public static class InstructionDocument {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String fileName;

        @Column(nullable = false)
        private String fileType;

        @Column(nullable = false)
        private long fileSize;

        @Column(nullable = false)
        private String filePath;

        @Column(name = "uploaded_at")
        private LocalDateTime uploadedAt;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "instruction_id")
        private Instruction instruction;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public LocalDateTime getUploadedAt() { return uploadedAt; }
        public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
        public Instruction getInstruction() { return instruction; }
        public void setInstruction(Instruction instruction) { this.instruction = instruction; }
    }

    @Entity
    @Table(name = "instruction_acknowledgements")
    public static class InstructionAcknowledgement {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "user_email", nullable = false)
        private String userEmail;

        @Column(nullable = false)
        private String status = "Pending";

        @Column(name = "acknowledged_at")
        private LocalDateTime acknowledgedAt;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "instruction_id")
        private Instruction instruction;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUserEmail() { return userEmail; }
        public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
        public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
        public Instruction getInstruction() { return instruction; }
        public void setInstruction(Instruction instruction) { this.instruction = instruction; }
    }
}