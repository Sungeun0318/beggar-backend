package com.beggar.api.repository;

import com.beggar.api.entity.RoomBeggarScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomBeggarScoreRepository extends JpaRepository<RoomBeggarScore, Long> {

    Optional<RoomBeggarScore> findByRoom_RoomNo(Long roomNo);

    boolean existsByRoom_RoomNo(Long roomNo);

    @Query("""
           select s
             from RoomBeggarScore s
             join fetch s.room r
            order by s.score desc, r.roomName asc
           """)
    List<RoomBeggarScore> findTopRoomScores(Pageable pageable);

    void deleteByRoom_RoomNo(Long roomNo);
}
