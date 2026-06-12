package com.beggar.api.repository;

import com.beggar.api.entity.Receipt;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    List<Receipt> findAllByRoom_RoomNoOrderByCreatedAtDesc(Long roomNo);

    @EntityGraph(attributePaths = {"room"})
    List<Receipt> findAllByRoom_RoomNoInAndReceiptTypeInOrderByCreatedAtDesc(
            Collection<Long> roomNos,
            Collection<Receipt.ReceiptType> receiptTypes
    );

    @Query("""
           select coalesce(sum(r.amount), 0)
             from Receipt r
            where r.room.roomNo = :roomNo
           """)
    long sumAmountByRoomNo(Long roomNo);

    @Query("""
           select count(r)
             from Receipt r
            where r.room.roomNo = :roomNo
              and r.goodPriceMatched = true
           """)
    long countGoodPriceMatchedByRoomNo(Long roomNo);

    @Query("""
           select r
             from Receipt r
            where r.uploader.user.userNo = :userNo
            order by r.createdAt desc
           """)
    List<Receipt> findAllByUploaderUserNo(Long userNo);

    void deleteAllByUploader_RoomMemberId(Long roomMemberId);

    void deleteAllByRoom_RoomNo(Long roomNo);
}
