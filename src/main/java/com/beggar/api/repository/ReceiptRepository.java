package com.beggar.api.repository;

import com.beggar.api.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    List<Receipt> findAllByRoom_RoomNoOrderByCreatedAtDesc(Long roomNo);

    @Query("""
           select coalesce(sum(r.amount), 0)
             from Receipt r
            where r.room.roomNo = :roomNo
           """)
    long sumAmountByRoomNo(Long roomNo);

    @Query("""
           select r
             from Receipt r
            where r.uploader.user.userNo = :userNo
            order by r.createdAt desc
           """)
    List<Receipt> findAllByUploaderUserNo(Long userNo);
}
