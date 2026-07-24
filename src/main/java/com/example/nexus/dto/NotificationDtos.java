package com.example.nexus.dto;

public class NotificationDtos {

    // ── Response shown in the notification panel ─────────────────────────
    public record NotificationResponse(
        Long   id,
        String title,
        String message,
        String type,          // "New Instruction" | "Reminder Notification"
        String status,        // "Unread" | "Read"
        String relatedType,   // "PROJECT" | "TASK"
        Long   relatedId,
        String createdAt
    ) {}

    // ── Response for the bell icon badge ──────────────────────────────────
    public record UnreadCountResponse(long unreadCount) {}
}