package com.beggar.api.repository;

import com.beggar.api.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);

    @Query("""
           select distinct r
             from Room r
             join RoomMember rm on rm.room = r
            where rm.user.userNo = :userNo and rm.status = com.beggar.api.entity.RoomMember.Status.ACTIVE
            order by r.roomCreated desc
           """)
    List<Room> findActiveRoomsByUserNo(Long userNo);
}
