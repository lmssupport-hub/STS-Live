package com.example.nexus.repository;

import com.example.nexus.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Latest first — matches "descending order based on latest date/time" rule
    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    long countByRecipientUserIdAndStatus(Long recipientUserId, String status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'Read' " +
           "WHERE n.recipientUserId = :userId AND n.status = 'Unread'")
    void markAllAsRead(@Param("userId") Long userId);
}