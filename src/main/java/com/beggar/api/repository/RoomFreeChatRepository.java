package com.beggar.api.repository;

import com.beggar.api.entity.RoomFreeChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoomFreeChatRepository extends JpaRepository<RoomFreeChat, Long> {

    @Query("SELECT c FROM RoomFreeChat c JOIN FETCH c.user ORDER BY c.createdAt DESC")
    List<RoomFreeChat> findTop100ByOrderByCreatedAtDesc();
}
