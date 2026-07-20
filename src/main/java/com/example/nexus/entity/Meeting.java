package com.example.nexus.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meetings")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDateTime meetingDateTime;

    @Column(nullable = false, length = 2000)
    private String agenda;

    @Column(length = 1000)
    private String decisionsPolls;

    @Column(nullable = false)
    private String status;   // Scheduled | In Progress | Completed | Expiry

    @Column(nullable = false)
    private Long ownerId;

    private Long projectId;

    // Member user IDs
    @ElementCollection
    @CollectionTable(name = "meeting_members", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "user_id")
    private List<Long> memberIds = new ArrayList<>();

    // Uploaded document S3/storage URLs
    @ElementCollection
    @CollectionTable(name = "meeting_documents", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "document_url", length = 1000)
    private List<String> documentUrls = new ArrayList<>();

    // Action items parsed from agenda lines
    @ElementCollection
    @CollectionTable(name = "meeting_action_items", joinColumns = @JoinColumn(name = "meeting_id"))
    @Column(name = "action_item", length = 500)
    private List<String> actionItems = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "team_admin_id", nullable = false)
    private Long teamAdminId;
    
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────────

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getTitle()                   { return title; }
    public void setTitle(String title)         { this.title = title; }

    public LocalDateTime getMeetingDateTime()               { return meetingDateTime; }
    public void setMeetingDateTime(LocalDateTime meetingDateTime) { this.meetingDateTime = meetingDateTime; }

    public String getAgenda()                  { return agenda; }
    public void setAgenda(String agenda)       { this.agenda = agenda; }

    public String getDecisionsPolls()                      { return decisionsPolls; }
    public void setDecisionsPolls(String decisionsPolls)   { this.decisionsPolls = decisionsPolls; }

    public String getStatus()                  { return status; }
    public void setStatus(String status)       { this.status = status; }

    public Long getOwnerId()                   { return ownerId; }
    public void setOwnerId(Long ownerId)       { this.ownerId = ownerId; }

    public Long getProjectId()                 { return projectId; }
    public void setProjectId(Long projectId)   { this.projectId = projectId; }

    public List<Long> getMemberIds()                   { return memberIds; }
    public void setMemberIds(List<Long> memberIds)     { this.memberIds = memberIds; }

    public List<String> getDocumentUrls()              { return documentUrls; }
    public void setDocumentUrls(List<String> documentUrls) { this.documentUrls = documentUrls; }

    public List<String> getActionItems()               { return actionItems; }
    public void setActionItems(List<String> actionItems) { this.actionItems = actionItems; }

    public LocalDateTime getCreatedAt()                { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)  { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)  { this.updatedAt = updatedAt; }
    
    public Long getTeamAdminId() { return teamAdminId; }
    public void setTeamAdminId(Long teamAdminId) { this.teamAdminId = teamAdminId; }
}