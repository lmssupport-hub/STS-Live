package com.example.nexus.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.nexus.entity.Instruction;
import com.example.nexus.entity.Instruction.InstructionDocument;
import com.example.nexus.entity.Instruction.InstructionAcknowledgement;

@Repository
public interface InstructionRepository extends JpaRepository<Instruction, Long> {

    List<Instruction> findByTeamAdminId(Long teamAdminId);

    @Query("""
            SELECT DISTINCT i FROM Instruction i
            LEFT JOIN i.targetUsersOrTeams t
            WHERE i.teamAdminId = :teamAdminId
              AND (:keyword IS NULL OR :keyword = '' OR
                   LOWER(i.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(i.category) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(i.priority) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(t) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR :status = '' OR i.status = :status)
              AND (:category IS NULL OR :category = '' OR i.category = :category)
              AND (:priority IS NULL OR :priority = '' OR i.priority = :priority)
            ORDER BY i.createdAt DESC
            """)
    List<Instruction> searchAndFilter(
            @Param("teamAdminId") Long teamAdminId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("category") String category,
            @Param("priority") String priority);

    interface InstructionAcknowledgementRepository extends JpaRepository<InstructionAcknowledgement, Long> {
        Optional<InstructionAcknowledgement> findByInstructionIdAndUserEmailIgnoreCase(Long instructionId, String userEmail);
        List<InstructionAcknowledgement> findByInstructionIdAndStatus(Long instructionId, String status);
        List<InstructionAcknowledgement> findByInstructionId(Long instructionId);
    }

    interface InstructionDocumentRepository extends JpaRepository<InstructionDocument, Long> {
    }
}