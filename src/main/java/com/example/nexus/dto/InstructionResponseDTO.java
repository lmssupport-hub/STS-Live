package com.example.nexus.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class InstructionResponseDTO {
    private Long id;
    private String title;
    private String category;
    private String description;
    private String priority;
    private String status;
    private LocalDate effectiveDate;
    private String ownerEmail;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> targetUsersOrTeams;
    private List<DocumentResponseDTO> documents;
    private List<AcknowledgementResponseDTO> acknowledgements;

    public InstructionResponseDTO(Long id, String title, String category, String description, String priority,
            String status, LocalDate effectiveDate, String ownerEmail, Long version, LocalDateTime createdAt,
            LocalDateTime updatedAt, List<String> targetUsersOrTeams, List<DocumentResponseDTO> documents,
            List<AcknowledgementResponseDTO> acknowledgements) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.effectiveDate = effectiveDate;
        this.ownerEmail = ownerEmail;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.targetUsersOrTeams = targetUsersOrTeams;
        this.documents = documents;
        this.acknowledgements = acknowledgements;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public String getOwnerEmail() { return ownerEmail; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<String> getTargetUsersOrTeams() { return targetUsersOrTeams; }
    public List<DocumentResponseDTO> getDocuments() { return documents; }
    public List<AcknowledgementResponseDTO> getAcknowledgements() { return acknowledgements; }

    // Payload the Angular team sends for Create / Update
    public static class InstructionRequestDTO {
        private String title;
        private String category;
        private String description;
        private String priority;
        private String status;
        private LocalDate effectiveDate;
        private List<String> targetUsersOrTeams;

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
        public List<String> getTargetUsersOrTeams() { return targetUsersOrTeams; }
        public void setTargetUsersOrTeams(List<String> targetUsersOrTeams) { this.targetUsersOrTeams = targetUsersOrTeams; }
    }

    // Nested inside InstructionResponseDTO.documents
    public static class DocumentResponseDTO {
        private Long id;
        private String fileName;
        private String fileType;
        private long fileSize;
        private LocalDateTime uploadedAt;

        public DocumentResponseDTO(Long id, String fileName, String fileType, long fileSize, LocalDateTime uploadedAt) {
            this.id = id;
            this.fileName = fileName;
            this.fileType = fileType;
            this.fileSize = fileSize;
            this.uploadedAt = uploadedAt;
        }

        public Long getId() { return id; }
        public String getFileName() { return fileName; }
        public String getFileType() { return fileType; }
        public long getFileSize() { return fileSize; }
        public LocalDateTime getUploadedAt() { return uploadedAt; }
    }

    // Nested inside InstructionResponseDTO.acknowledgements
    public static class AcknowledgementResponseDTO {
        private Long id;
        private String userEmail;
        private String status;
        private LocalDateTime acknowledgedAt;

        public AcknowledgementResponseDTO(Long id, String userEmail, String status, LocalDateTime acknowledgedAt) {
            this.id = id;
            this.userEmail = userEmail;
            this.status = status;
            this.acknowledgedAt = acknowledgedAt;
        }

        public Long getId() { return id; }
        public String getUserEmail() { return userEmail; }
        public String getStatus() { return status; }
        public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    }
}