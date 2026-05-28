package com.beggar.api.repository;

import com.beggar.api.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByRoomMember_RoomMemberId(Long roomMemberId);

    @Query("""
           select b
             from Budget b
            where b.roomMember.room.roomNo = :roomNo
           """)
    List<Budget> findAllByRoomNo(Long roomNo);

    @Query("""
           select count(b)
             from Budget b
            where b.roomMember.room.roomNo = :roomNo
           """)
    long countByRoomNo(Long roomNo);

    @Query("""
           select coalesce(min(b.budgetAmount), 0)
             from Budget b
            where b.roomMember.room.roomNo = :roomNo
           """)
    int findMinBudgetByRoomNo(Long roomNo);
}
