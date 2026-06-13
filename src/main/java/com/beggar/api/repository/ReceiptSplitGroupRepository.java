package com.beggar.api.repository;

import com.beggar.api.entity.ReceiptSplitGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptSplitGroupRepository extends JpaRepository<ReceiptSplitGroup, Long> {

    List<ReceiptSplitGroup> findAllByRoom_RoomNoOrderByCreatedAtDesc(Long roomNo);

    List<ReceiptSplitGroup> findAllByRoom_RoomNoAndStatus(
            Long roomNo,
            ReceiptSplitGroup.SplitGroupStatus status
    );
}
