package com.example.nexus.repository;

import com.example.nexus.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    List<RoleEntity> findByCreatedByAdminId(Long adminId);
    boolean existsByNameIgnoreCaseAndCreatedByAdminId(String name, Long adminId);
    boolean existsByNameIgnoreCaseAndCreatedByAdminIdAndIdNot(String name, Long adminId, Long id);
}