package com.beggar.api.repository;

import com.beggar.api.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);

    @Query("SELECT rm.room FROM RoomMember rm " +
           "WHERE rm.user.userNo = :userNo AND rm.status = com.beggar.api.entity.RoomMember.Status.ACTIVE " +
           "ORDER BY rm.room.roomCreated DESC")
    java.util.List<Room> findActiveRoomsByUserNo(Long userNo);
}