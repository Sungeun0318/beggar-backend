package com.beggar.api.repository;

import com.beggar.api.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByRoomNoAndUserNo(Long roomNo, Long userNo);

    List<Budget> findByRoomNo(Long roomNo);

    long countByRoomNo(Long roomNo);

    boolean existsByRoomNoAndUserNo(Long roomNo, Long userNo);
}
