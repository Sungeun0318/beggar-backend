package com.beggar.api.dto.receipt;

import com.beggar.api.entity.Receipt;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomMember;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceiptCreateRequest(
        Long receiptId,
        Long roomNo,
        Long uploaderUserNo,
        String imageUrl,
        String ocrStatus,
        String storeName,
        Integer totalAmount,
        Integer amount,
        String address,
        BigDecimal centerLat,
        BigDecimal centerLng,
        LocalDateTime createdAt,
        LocalDateTime updatedAt      // 수동 입력값 (OCR 전 가입력)
) {
    public static ReceiptCreateRequest from(Receipt receipt) {
        return new ReceiptCreateRequest(
                receipt.getReceiptId(),
                receipt.getRoom().getRoomNo(), // Room 엔티티에서 ID만 추출
                receipt.getUploader().getRoomMemberId(), // RoomMember 엔티티에서 ID만 추출
                receipt.getImageUrl(),
                receipt.getOcrStatus().name(), // Enum을 String으로 변환
                receipt.getStoreName(),
                receipt.getTotalAmount(),
                receipt.getAmount(),
                receipt.getAddress(),
                receipt.getCenterLat(),
                receipt.getCenterLng(),
                receipt.getCreatedAt(),
                receipt.getUpdatedAt()
        );
    }

    public Receipt toEntity(Room room, RoomMember uploader) {
        return Receipt.builder()
            .room(room)
            .uploader(uploader)
            .imageUrl(this.imageUrl)
            .amount(this.amount)
            .build();
}}
