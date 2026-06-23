package com.beggar.api.repository;

import com.beggar.api.entity.RoomRouletteResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRouletteResultRepository extends JpaRepository<RoomRouletteResult, Long> {
    Optional<RoomRouletteResult> findByRoom_RoomNo(Long roomNo);

    void deleteByRoom_RoomNo(Long roomNo);
}
