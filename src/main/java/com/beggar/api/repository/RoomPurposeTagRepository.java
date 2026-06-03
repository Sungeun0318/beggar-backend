package com.beggar.api.repository;

import com.beggar.api.entity.RoomPurposeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomPurposeTagRepository extends JpaRepository<RoomPurposeTag, Integer> {
    List<RoomPurposeTag> findAllByRoom_RoomNo(Long roomNo);
}
