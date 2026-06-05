package com.beggar.api.repository;

import com.beggar.api.entity.RoomMember; // 소영님의 실제 룸멤버 엔티티 경로 확인!
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    // 👥 특정 방에 있는 ACTIVE(참여중) 상태의 멤버 수를 세어주는 기능!
    long countByRoomNoAndStatus(Long roomNo, String status);
    Optional<RoomMember> findByRoom_RoomNoAndUser_UserNo(Long roomNo, Long userNo);
}