package com.example.nexus.service;

import com.example.nexus.dto.NotificationDtos.*;
import com.example.nexus.entity.Notification;
import com.example.nexus.entity.User;
import com.example.nexus.repository.NotificationRepository;
import com.example.nexus.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                                UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // ════════════════════════════════════════════════════════════════════
    //  Creation — called from ProjectService / TaskService on assignment
    // ════════════════════════════════════════════════════════════════════

    /**
     * Creates a notification for a known recipient (by internal user id).
     * Used when we already have the User (e.g. Task.assignedUser).
     */
    @Transactional
    public void notifyUser(Long recipientUserId, String title, String message,
                            String type, String relatedType, Long relatedId) {
        if (recipientUserId == null) return;

        Notification n = new Notification();
        n.setRecipientUserId(recipientUserId);
        n.setTitle(truncate(title, 200));
        n.setMessage(truncate(message, 1000));
        n.setType(type);
        n.setStatus("Unread");
        n.setRelatedType(relatedType);
        n.setRelatedId(relatedId);

        notificationRepository.save(n);
    }

    /**
     * Creates a notification by resolving the recipient's email to a user id.
     * Used when we only have an email (e.g. Project.assignedUsers is List<String>).
     * Silently skips unknown emails instead of failing the whole assignment.
     */
    @Transactional
    public void notifyUserByEmail(String email, String title, String message,
                                   String type, String relatedType, Long relatedId) {
        if (email == null || email.isBlank()) return;
        userRepository.findByEmail(email.trim())
                .ifPresentOrElse(
                        u -> notifyUser(u.getId(), title, message, type, relatedType, relatedId),
                        () -> System.out.println("NOTIFY SKIP — no user found for email: [" + email.trim() + "]")
                );
    }

    // ════════════════════════════════════════════════════════════════════
    //  Read side — used by NotificationController
    // ════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String requesterEmail) {
        Long userId = resolveUserId(requesterEmail);
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(String requesterEmail) {
        Long userId = resolveUserId(requesterEmail);
        long count = notificationRepository.countByRecipientUserIdAndStatus(userId, "Unread");
        return new UnreadCountResponse(count);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, String requesterEmail) {
        Long userId = resolveUserId(requesterEmail);
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!n.getRecipientUserId().equals(userId)) {
            throw new RuntimeException("You don't have access to this notification");
        }

        // Idempotent — re-marking an already-read notification is a no-op
        if (!"Read".equals(n.getStatus())) {
            n.setStatus("Read");
            notificationRepository.save(n);
        }
        return toResponse(n);
    }

    @Transactional
    public void markAllAsRead(String requesterEmail) {
        Long userId = resolveUserId(requesterEmail);
        notificationRepository.markAllAsRead(userId);
    }

    // ════════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════════

    private Long resolveUserId(String requesterEmail) {
        User user = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("Invalid session"));
        return user.getId();
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.getStatus(),
                n.getRelatedType(),
                n.getRelatedId(),
                n.getCreatedAt() != null ? n.getCreatedAt().toString() : null
        );
    }
}