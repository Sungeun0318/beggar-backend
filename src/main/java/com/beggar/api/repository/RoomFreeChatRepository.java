package com.beggar.api.repository;

import com.beggar.api.entity.RoomFreeChat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomFreeChatRepository extends JpaRepository<RoomFreeChat, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<RoomFreeChat> findTop100ByOrderByCreatedAtDesc();

    @Modifying
    @Query("DELETE FROM RoomFreeChat r WHERE r.createdAt < :targetDate")
    void deleteOldChats(@Param("targetDate") LocalDateTime targetDate);
}
