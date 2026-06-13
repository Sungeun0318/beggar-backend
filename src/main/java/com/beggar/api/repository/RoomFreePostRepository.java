package com.beggar.api.repository;

import com.beggar.api.entity.RoomFreePost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface RoomFreePostRepository extends JpaRepository<RoomFreePost, Long> {

    @Query("SELECT p FROM RoomFreePost p JOIN FETCH p.author " +
            "WHERE (:keyword IS NULL OR p.title LIKE %:keyword% OR p.content LIKE %:keyword%) " +
            "ORDER BY p.createdAt DESC")
    List<RoomFreePost> findAllWithAuthorByKeyword(@Param("keyword") String keyword);

    @Query("SELECT p FROM RoomFreePost p JOIN FETCH p.author " +
            "LEFT JOIN p.comments c " +
            "GROUP BY p " +
            "ORDER BY COUNT(c) DESC, p.createdAt DESC")
    List<RoomFreePost> findPopularPosts();

    void deleteAllByAuthor_UserNo(Long userNo);

    List<RoomFreePost> findTop5ByOrderByCreatedAtDesc();

    long countByAuthor_UserNo(Long userNo);

    Page<RoomFreePost> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(
            String title,
            String content,
            Pageable pageable
    );

    Page<RoomFreePost> findByTagAndTitleContainingIgnoreCaseOrTagAndContentContainingIgnoreCase(
            String titleTag,
            String title,
            String contentTag,
            String content,
            Pageable pageable
    );
}
