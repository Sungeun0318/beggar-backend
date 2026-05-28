package com.beggar.api.repository;

import com.beggar.api.entity.RoomPurposeTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomPurposeTagRepository extends JpaRepository<RoomPurposeTag, Long> {

    List<RoomPurposeTag> findAllByRoom_RoomNo(Long roomNo);

    void deleteAllByRoom_RoomNo(Long roomNo);
}
