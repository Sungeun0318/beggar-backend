package com.beggar.api.repository;

import com.beggar.api.entity.Budget;
import com.beggar.api.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByRoomNoAndUserNo(Long roomNo, Long userNo);

    List<Budget> findByRoomNo(Long roomNo);

    long countByRoomNo(Long roomNo);

    @Query("""
            select b
            from Budget b
            where b.roomNo = :roomNo
              and b.userNo in (
                  select rm.user.userNo
                  from RoomMember rm
                  where rm.room.roomNo = :roomNo
                    and rm.status = :status
              )
            """)
    List<Budget> findByRoomNoAndActiveMembers(
            @Param("roomNo") Long roomNo,
            @Param("status") RoomMember.Status status
    );

    @Query("""
            select count(distinct b.userNo)
            from Budget b
            where b.roomNo = :roomNo
              and b.userNo in (
                  select rm.user.userNo
                  from RoomMember rm
                  where rm.room.roomNo = :roomNo
                    and rm.status = :status
              )
            """)
    long countSubmittedActiveMembers(
            @Param("roomNo") Long roomNo,
            @Param("status") RoomMember.Status status
    );

    boolean existsByRoomNoAndUserNo(Long roomNo, Long userNo);

    void deleteAllByUserNo(Long userNo);

    void deleteAllByRoomNo(Long roomNo);
}
