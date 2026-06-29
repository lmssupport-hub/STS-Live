package com.example.nexus.repository;

// ─────────────────────────────────────────────────────────────────────────────
// Your UserRepository already exists — NO changes needed.
// MeetingService only uses findById(Long), which is inherited from JpaRepository.
// This file is shown for reference only. Do NOT replace your existing file.
// ─────────────────────────────────────────────────────────────────────────────

import com.example.nexus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);
    Optional<User> findByEmailIgnoreCase(String email);
    // findById(Long) is already inherited from JpaRepository — used by MeetingService
}