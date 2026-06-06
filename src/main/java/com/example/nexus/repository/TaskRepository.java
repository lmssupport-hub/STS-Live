package com.example.nexus.repository;

import com.example.nexus.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
            SELECT DISTINCT t FROM Task t
            LEFT JOIN FETCH t.subTasks
            JOIN FETCH t.assignedUser
            JOIN FETCH t.project
            WHERE t.project.id = :projectId
            ORDER BY t.createdAt ASC
            """)
    List<Task> findByProjectId(@Param("projectId") Long projectId);
}