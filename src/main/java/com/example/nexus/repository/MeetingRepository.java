package com.example.nexus.repository;
import com.example.nexus.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByTeamAdminId(Long teamAdminId);

    List<Meeting> findByStatusAndTeamAdminId(String status, Long teamAdminId);

    List<Meeting> findByOwnerIdAndTeamAdminId(Long ownerId, Long teamAdminId);

    List<Meeting> findByStatusAndOwnerIdAndTeamAdminId(String status, Long ownerId, Long teamAdminId);

    @Query("SELECT m FROM Meeting m WHERE " +
           "(LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(m.agenda) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND m.teamAdminId = :teamAdminId")
    List<Meeting> searchByKeyword(@Param("keyword") String keyword,
                                   @Param("teamAdminId") Long teamAdminId);

    @Query("SELECT m FROM Meeting m WHERE " +
           "(LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(m.agenda) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND m.status = :status AND m.teamAdminId = :teamAdminId")
    List<Meeting> searchByKeywordAndStatus(@Param("keyword") String keyword,
                                           @Param("status") String status,
                                           @Param("teamAdminId") Long teamAdminId);

    @Query("SELECT m FROM Meeting m WHERE " +
           "(LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(m.agenda) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND m.ownerId = :ownerId AND m.teamAdminId = :teamAdminId")
    List<Meeting> searchByKeywordAndOwner(@Param("keyword") String keyword,
                                          @Param("ownerId") Long ownerId,
                                          @Param("teamAdminId") Long teamAdminId);

    @Query("SELECT m FROM Meeting m WHERE " +
           "(LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(m.agenda) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND m.status = :status AND m.ownerId = :ownerId AND m.teamAdminId = :teamAdminId")
    List<Meeting> searchByKeywordStatusAndOwner(@Param("keyword") String keyword,
                                                @Param("status") String status,
                                                @Param("ownerId") Long ownerId,
                                                @Param("teamAdminId") Long teamAdminId);
}