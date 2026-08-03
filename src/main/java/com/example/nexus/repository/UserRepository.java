package com.example.nexus.repository;
import com.example.nexus.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email); 
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);
    Optional<User> findByEmailIgnoreCase(String email);
    List<User> findByRole(String role);
    List<User> findByCreatedByAdminId(Long createdByAdminId);
}