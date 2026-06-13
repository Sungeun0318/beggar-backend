package com.beggar.api.repository;

import com.beggar.api.entity.RoomFreeComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface RoomFreeCommentRepository extends JpaRepository<RoomFreeComment, Long> {

    void deleteAllByAuthor_UserNo(Long userNo);

    @Modifying
    @Query("""
           DELETE FROM RoomFreeComment c
            WHERE c.post.postId IN (
                  SELECT p.postId
                    FROM RoomFreePost p
                   WHERE p.author.userNo = :userNo
            )
           """)
    void deleteAllByPostAuthorUserNo(@Param("userNo") Long userNo);

    long countByAuthor_UserNo(Long userNo);

    long countByPost_PostId(Long postId);

    List<RoomFreeComment> findByPost_PostIdOrderByCreatedAtAsc(Long postId);

    Page<RoomFreeComment> findByContentContainingIgnoreCase(String content, Pageable pageable);

    Page<RoomFreeComment> findByPost_PostId(Long postId, Pageable pageable);

    Page<RoomFreeComment> findByPost_PostIdAndContentContainingIgnoreCase(Long postId, String content, Pageable pageable);

    @Modifying
    @Query("delete from RoomFreeComment c where c.post.postId = :postId")
    void deleteByPost_PostId(@Param("postId") Long postId);
}
