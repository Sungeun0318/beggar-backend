package com.beggar.api.repository;

import com.beggar.api.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    long countByRoom_RoomNoAndStatus(Long roomNo, RoomMember.Status status);
    Optional<RoomMember> findByRoom_RoomNoAndUser_UserNo(Long roomNo, Long userNo);
    List<RoomMember> findByRoom_RoomNoOrderByJoinedAtAsc(Long roomNo);
    List<RoomMember> findByUser_UserNoAndStatus(Long userNo, RoomMember.Status status);
    List<RoomMember> findByUser_UserNoAndStatusAndIsHiddenFalse(Long userNo, RoomMember.Status status);
    List<RoomMember> findByUser_UserNo(Long userNo);
    Optional<RoomMember> findFirstByRoom_RoomNoAndUser_UserNoNotAndStatusOrderByJoinedAtAsc(
            Long roomNo,
            Long userNo,
            RoomMember.Status status
    );
    void deleteAllByRoom_RoomNo(Long roomNo);

    long countByUser_UserNo(Long userNo);

    long countByRoom_RoomNo(Long roomNo);

    long countByRoom_RoomNoAndStatus(Long roomNo, String status);
}
