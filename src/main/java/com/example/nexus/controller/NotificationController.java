package com.example.nexus.controller;

import com.example.nexus.dto.NotificationDtos.*;
import com.example.nexus.service.NotificationService;
import com.example.nexus.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin("*")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    public NotificationController(NotificationService notificationService, JwtUtil jwtUtil) {
        this.notificationService = notificationService;
        this.jwtUtil = jwtUtil;
    }

    private String extractEmail(String authHeader) {
        return jwtUtil.extractEmail(authHeader.replace("Bearer ", ""));
    }

    // GET /api/notifications  — list, latest first
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(notificationService.getMyNotifications(extractEmail(authHeader)));
    }

    // GET /api/notifications/unread-count  — badge count on the bell icon
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(notificationService.getUnreadCount(extractEmail(authHeader)));
    }

    // PUT /api/notifications/{id}/read  — mark one as read (on open)
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(notificationService.markAsRead(id, extractEmail(authHeader)));
    }

    // PUT /api/notifications/read-all
    @PutMapping("/read-all")
    public ResponseEntity<String> markAllAsRead(
            @RequestHeader("Authorization") String authHeader) {
        notificationService.markAllAsRead(extractEmail(authHeader));
        return ResponseEntity.ok("All notifications marked as read");
    }
}