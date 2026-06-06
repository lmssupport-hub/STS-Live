package com.example.nexus.repository;

import com.example.nexus.entity.SubTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubtaskRepository extends JpaRepository<SubTask, Long> {
}