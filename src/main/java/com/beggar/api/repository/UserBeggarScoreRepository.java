package com.beggar.api.repository;

import com.beggar.api.entity.UserBeggarScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserBeggarScoreRepository extends JpaRepository<UserBeggarScore, Long> {

    @Query("""
           select s
             from UserBeggarScore s
             join fetch s.user u
            order by s.score desc, u.userName asc
           """)
    List<UserBeggarScore> findTopRanking(Pageable pageable);
}
