package com.beggar.api.repository;

import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);

    boolean existsByRoomName(String roomName);

    @Query("SELECT rm.room FROM RoomMember rm " +
           "WHERE rm.user.userNo = :userNo " +
           "AND rm.status = com.beggar.api.entity.RoomMember.Status.ACTIVE " +
           "AND rm.isHidden = false " +
           "AND rm.room.status != com.beggar.api.entity.RoomStatus.DELETED " +
           "ORDER BY rm.room.roomCreated DESC")
    java.util.List<Room> findActiveRoomsByUserNo(Long userNo);

    List<Room> findByOwnerUserNo(Long ownerUserNo);

    long countByStatus(RoomStatus status);

    long countByStatusIn(Collection<RoomStatus> statuses);

    List<Room> findTop5ByOrderByRoomCreatedDesc();

    long countByOwnerUserNo(Long ownerUserNo);

    @Query("""
            SELECT r
              FROM Room r
             WHERE (:status = 'ALL' AND r.status NOT IN (com.beggar.api.entity.RoomStatus.DRAFT, com.beggar.api.entity.RoomStatus.DELETED) 
                    OR CAST(r.status AS string) = :status)
               AND (
                    :keyword = ''
                    OR LOWER(COALESCE(r.roomName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(r.location, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(r.roomCode, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               )
            """)
    Page<Room> searchRooms(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}
