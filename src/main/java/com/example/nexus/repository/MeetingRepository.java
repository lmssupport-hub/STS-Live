package com.example.nexus.repository;

import com.example.nexus.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    // Filter by status
    List<Meeting> findByStatus(String status);

    // Filter by owner
    List<Meeting> findByOwnerId(Long ownerId);

    // Filter by status + owner
    List<Meeting> findByStatusAndOwnerId(String status, Long ownerId);

    // Full-text search on title or agenda (case-insensitive)
    @Query("SELECT m FROM Meeting m WHERE " +
           "LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.agenda) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Meeting> searchByKeyword(@Param("keyword") String keyword);

    // Search + status filter
    @Query("SELECT m FROM Meeting m WHERE " +
           "(LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(m.agenda) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND m.status = :status")
    List<Meeting> searchByKeywordAndStatus(@Param("keyword") String keyword,
                                           @Param("status") String status);

    // Search + owner filter
    @Query("SELECT m FROM Meeting m WHERE " +
           "(LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(m.agenda) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND m.ownerId = :ownerId")
    List<Meeting> searchByKeywordAndOwner(@Param("keyword") String keyword,
                                          @Param("ownerId") Long ownerId);

    // Search + status + owner filter
    @Query("SELECT m FROM Meeting m WHERE " +
           "(LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(m.agenda) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND m.status = :status AND m.ownerId = :ownerId")
    List<Meeting> searchByKeywordStatusAndOwner(@Param("keyword") String keyword,
                                                @Param("status") String status,
                                                @Param("ownerId") Long ownerId);
}