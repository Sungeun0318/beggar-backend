package com.beggar.api.repository;

import com.beggar.api.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    List<RoomMember> findAllByRoom_RoomNo(Long roomNo);

    List<RoomMember> findAllByRoom_RoomNoAndStatus(Long roomNo, RoomMember.Status status);

    Optional<RoomMember> findByRoom_RoomNoAndUser_UserNo(Long roomNo, Long userNo);

    long countByRoom_RoomNoAndStatus(Long roomNo, RoomMember.Status status);

    List<RoomMember> findAllByUser_UserNo(Long userNo);
}
