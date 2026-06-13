package com.beggar.api.repository;

import com.beggar.api.entity.RoomBudgetResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface RoomBudgetResultRepository extends JpaRepository<RoomBudgetResult, Long> {

    // 📊 방 번호로 확정된 예산 결과 데이터를 찾아오는 기능!
    Optional<RoomBudgetResult> findByRoom_RoomNo(Long roomNo);

    void deleteByRoom_RoomNo(Long roomNo);
}
