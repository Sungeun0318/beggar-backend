package com.beggar.api.repository;

import com.beggar.api.entity.RoomFreeComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
