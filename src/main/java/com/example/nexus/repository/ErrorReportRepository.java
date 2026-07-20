package com.example.nexus.repository;
import com.example.nexus.entity.ErrorReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ErrorReportRepository extends JpaRepository<ErrorReport, Long> {
    @Query("""
            SELECT DISTINCT e FROM ErrorReport e
            JOIN FETCH e.project p
            JOIN FETCH e.task t
            JOIN FETCH e.assignedUser u
            WHERE p.teamAdminId = :teamAdminId
              AND (:projectId IS NULL OR p.id = :projectId)
              AND (:status IS NULL OR e.status = :status)
              AND (:priority IS NULL OR e.priority = :priority)
              AND (:assignedUserId IS NULL OR u.id = :assignedUserId)
              AND (
                    CAST(:keyword AS string) IS NULL
                    OR LOWER(e.pageTitle) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                    OR LOWER(e.errorDescription) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                    OR LOWER(e.expectedResult) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                    OR LOWER(t.taskName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                    OR LOWER(p.projectName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
                  )
            ORDER BY e.createdAt DESC
            """)
    List<ErrorReport> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("status") ErrorReport.ErrorStatus status,
            @Param("priority") ErrorReport.Priority priority,
            @Param("assignedUserId") Long assignedUserId,
            @Param("projectId") Long projectId,
            @Param("teamAdminId") Long teamAdminId
    );

    @Query("""
            SELECT e FROM ErrorReport e
            JOIN FETCH e.project
            JOIN FETCH e.task
            JOIN FETCH e.assignedUser
            WHERE e.task.id = :taskId
            ORDER BY e.createdAt DESC
            """)
    List<ErrorReport> findByTaskId(@Param("taskId") Long taskId);
}