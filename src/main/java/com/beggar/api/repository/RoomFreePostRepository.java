package com.beggar.api.repository;

import com.beggar.api.entity.RoomFreePost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomFreePostRepository extends JpaRepository<RoomFreePost, Long> {

    @Query("SELECT p FROM RoomFreePost p JOIN FETCH p.author " +
            "WHERE (:keyword IS NULL OR p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
            "ORDER BY p.createdAt DESC")
    List<RoomFreePost> findAllWithAuthorByKeyword(@Param("keyword") String keyword);
}
