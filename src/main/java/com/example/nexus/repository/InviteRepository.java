package com.example.nexus.repository;

import com.example.nexus.entity.Invite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteRepository extends JpaRepository<Invite, Long> {
    Optional<Invite> findByTokenAndUsedFalse(String token);
    boolean existsByEmailIgnoreCaseAndUsedFalse(String email);
}