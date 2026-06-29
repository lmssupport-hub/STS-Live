package com.example.nexus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

public class MeetingDtos {

    // ── Create / Update request ────────────────────────────────────────────────
    // @JsonIgnoreProperties(ignoreUnknown = true) silently drops any extra fields
    // the Angular frontend spreads in from the full MeetingDTO on update:
    // (ownerName, projectName, members, documentUrls, actionItems, createdAt,
    //  updatedAt) — previously these caused UnrecognizedPropertyException.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MeetingRequest {

        private Long       id;              // sent by Angular on update, safely ignored by service
        private String     title;
        private String     meetingDateTime; // "2026-06-15T13:00:00"
        private String     agenda;
        private String     decisionsPolls;
        private String     status;
        private Long       ownerId;
        private Long       projectId;
        private List<Long> memberIds;

        // ── Getters & Setters ──────────────────────────────────────────────────

        public Long   getId()                      { return id; }
        public void   setId(Long id)               { this.id = id; }

        public String getTitle()                   { return title; }
        public void   setTitle(String title)       { this.title = title; }

        public String getMeetingDateTime()                     { return meetingDateTime; }
        public void   setMeetingDateTime(String meetingDateTime) { this.meetingDateTime = meetingDateTime; }

        public String getAgenda()                  { return agenda; }
        public void   setAgenda(String agenda)     { this.agenda = agenda; }

        public String getDecisionsPolls()                      { return decisionsPolls; }
        public void   setDecisionsPolls(String decisionsPolls) { this.decisionsPolls = decisionsPolls; }

        public String getStatus()                  { return status; }
        public void   setStatus(String status)     { this.status = status; }

        public Long   getOwnerId()                 { return ownerId; }
        public void   setOwnerId(Long ownerId)     { this.ownerId = ownerId; }

        public Long   getProjectId()               { return projectId; }
        public void   setProjectId(Long projectId) { this.projectId = projectId; }

        public List<Long> getMemberIds()                   { return memberIds; }
        public void       setMemberIds(List<Long> memberIds) { this.memberIds = memberIds; }
    }

    // ── Member info embedded in response ──────────────────────────────────────
    public static class MemberInfo {
        private Long   id;
        private String username;
        private String email;

        public MemberInfo() {}
        public MemberInfo(Long id, String username, String email) {
            this.id       = id;
            this.username = username;
            this.email    = email;
        }

        public Long   getId()               { return id; }
        public void   setId(Long id)        { this.id = id; }
        public String getUsername()         { return username; }
        public void   setUsername(String u) { this.username = u; }
        public String getEmail()            { return email; }
        public void   setEmail(String e)    { this.email = e; }
    }

    // ── Full meeting response (enriched) ──────────────────────────────────────
    public static class MeetingResponse {

        private Long             id;
        private String           title;
        private String           meetingDateTime;
        private String           agenda;
        private String           decisionsPolls;
        private String           status;
        private Long             ownerId;
        private String           ownerName;
        private Long             projectId;
        private String           projectName;
        private List<Long>       memberIds;
        private List<MemberInfo> members;
        private List<String>     documentUrls;
        private List<String>     actionItems;
        private String           createdAt;
        private String           updatedAt;

        // ── Getters & Setters ──────────────────────────────────────────────────

        public Long   getId()                          { return id; }
        public void   setId(Long id)                   { this.id = id; }

        public String getTitle()                       { return title; }
        public void   setTitle(String t)               { this.title = t; }

        public String getMeetingDateTime()             { return meetingDateTime; }
        public void   setMeetingDateTime(String dt)    { this.meetingDateTime = dt; }

        public String getAgenda()                      { return agenda; }
        public void   setAgenda(String a)              { this.agenda = a; }

        public String getDecisionsPolls()              { return decisionsPolls; }
        public void   setDecisionsPolls(String dp)     { this.decisionsPolls = dp; }

        public String getStatus()                      { return status; }
        public void   setStatus(String s)              { this.status = s; }

        public Long   getOwnerId()                     { return ownerId; }
        public void   setOwnerId(Long o)               { this.ownerId = o; }

        public String getOwnerName()                   { return ownerName; }
        public void   setOwnerName(String n)           { this.ownerName = n; }

        public Long   getProjectId()                   { return projectId; }
        public void   setProjectId(Long p)             { this.projectId = p; }

        public String getProjectName()                 { return projectName; }
        public void   setProjectName(String n)         { this.projectName = n; }

        public List<Long>       getMemberIds()         { return memberIds; }
        public void             setMemberIds(List<Long> m) { this.memberIds = m; }

        public List<MemberInfo> getMembers()           { return members; }
        public void             setMembers(List<MemberInfo> m) { this.members = m; }

        public List<String>     getDocumentUrls()      { return documentUrls; }
        public void             setDocumentUrls(List<String> d) { this.documentUrls = d; }

        public List<String>     getActionItems()       { return actionItems; }
        public void             setActionItems(List<String> a) { this.actionItems = a; }

        public String getCreatedAt()                   { return createdAt; }
        public void   setCreatedAt(String c)           { this.createdAt = c; }

        public String getUpdatedAt()                   { return updatedAt; }
        public void   setUpdatedAt(String u)           { this.updatedAt = u; }
    }
}