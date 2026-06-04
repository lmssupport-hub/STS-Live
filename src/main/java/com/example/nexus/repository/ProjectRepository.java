package com.example.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.nexus.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByProjectNameIgnoreCase(String projectName);

    
    boolean existsByProjectNameIgnoreCaseAndIdNot(String projectName, Long id);
}