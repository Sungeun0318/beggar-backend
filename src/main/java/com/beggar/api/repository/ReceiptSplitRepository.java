package com.beggar.api.repository;

import com.beggar.api.entity.ReceiptSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ReceiptSplitRepository extends JpaRepository<ReceiptSplit, Long> {

    List<ReceiptSplit> findAllByReceipt_ReceiptId(Long receiptId);

    List<ReceiptSplit> findAllByRoomMember_RoomMemberId(Long roomMemberId);

    void deleteAllByReceipt_ReceiptId(Long receiptId);

    void deleteAllByReceipt_Uploader_RoomMemberId(Long roomMemberId);

    void deleteAllByRoomMember_RoomMemberId(Long roomMemberId);

    void deleteAllByReceipt_Room_RoomNo(Long roomNo);

    void deleteAllByRoomMember_Room_RoomNo(Long roomNo);
}
