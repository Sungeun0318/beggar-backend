package com.beggar.api.repository;

import com.beggar.api.entity.RoomFreeComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomFreeCommentRepository extends JpaRepository<RoomFreeComment, Long> {
}
