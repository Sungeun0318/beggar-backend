package com.beggar.api.repository;

import com.beggar.api.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    long countByRoom_RoomNoAndStatus(Long roomNo, RoomMember.Status status);
    Optional<RoomMember> findByRoom_RoomNoAndUser_UserNo(Long roomNo, Long userNo);
}
