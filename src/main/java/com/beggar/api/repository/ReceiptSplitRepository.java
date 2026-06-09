package com.beggar.api.repository;

import com.beggar.api.entity.ReceiptSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceiptSplitRepository extends JpaRepository<ReceiptSplit, Long> {

    List<ReceiptSplit> findAllByReceipt_ReceiptId(Long receiptId);

    List<ReceiptSplit> findAllByRoomMember_RoomMemberId(Long roomMemberId);

    void deleteAllByReceipt_ReceiptId(Long receiptId);
}
