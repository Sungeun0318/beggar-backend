package com.beggar.api.repository;

import com.beggar.api.entity.RoomFreeChat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomFreeChatRepository extends JpaRepository<RoomFreeChat, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<RoomFreeChat> findTop100ByOrderByCreatedAtDesc();
}
