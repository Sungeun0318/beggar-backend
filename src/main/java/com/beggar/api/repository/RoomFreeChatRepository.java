package com.beggar.api.repository;

import com.beggar.api.entity.RoomFreeChat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface RoomFreeChatRepository extends JpaRepository<RoomFreeChat, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<RoomFreeChat> findTop100ByOrderByCreatedAtDesc();

    @Modifying
    @Query("DELETE FROM RoomFreeChat r WHERE r.createdAt < :targetDate")
    void deleteOldChats(@Param("targetDate") LocalDateTime targetDate);

    void deleteAllByUser_UserNo(Long userNo);

    Page<RoomFreeChat> findByMessageContainingIgnoreCase(String message, Pageable pageable);

    Page<RoomFreeChat> findByUser_UserNo(Long userNo, Pageable pageable);

    Page<RoomFreeChat> findByUser_UserNoAndMessageContainingIgnoreCase(Long userNo, String message, Pageable pageable);
}
