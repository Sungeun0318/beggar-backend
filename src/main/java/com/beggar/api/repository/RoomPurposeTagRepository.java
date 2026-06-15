package com.beggar.api.repository;

import com.beggar.api.entity.RoomPurposeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomPurposeTagRepository extends JpaRepository<RoomPurposeTag, Integer> {
    List<RoomPurposeTag> findByRoom_RoomNo(Long roomNo);
    List<RoomPurposeTag> findAllByRoom_RoomNo(Long roomNo);

    @Query("select t from RoomPurposeTag t join fetch t.room")
    List<RoomPurposeTag> findAllWithRoom();

    void deleteAllByRoom_RoomNo(Long roomNo);
}
