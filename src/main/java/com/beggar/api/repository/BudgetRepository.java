package com.beggar.api.repository;

import com.beggar.api.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByRoomNoAndUserNo(Long roomNo, Long userNo);

    List<Budget> findByRoomNo(Long roomNo);

    long countByRoomNo(Long roomNo);

    boolean existsByRoomNoAndUserNo(Long roomNo, Long userNo);

    void deleteAllByUserNo(Long userNo);

    void deleteAllByRoomNo(Long roomNo);
}
