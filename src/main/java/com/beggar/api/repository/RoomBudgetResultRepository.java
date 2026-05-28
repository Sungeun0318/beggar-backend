package com.beggar.api.repository;

import com.beggar.api.entity.RoomBudgetResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomBudgetResultRepository extends JpaRepository<RoomBudgetResult, Long> {

    Optional<RoomBudgetResult> findByRoom_RoomNo(Long roomNo);

    boolean existsByRoom_RoomNo(Long roomNo);
}
