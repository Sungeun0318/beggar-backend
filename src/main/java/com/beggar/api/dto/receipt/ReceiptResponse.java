package com.beggar.api.dto.receipt;

import com.beggar.api.entity.Receipt;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReceiptResponse(
        Long receiptId,
        Long roomNo,
        Long uploaderUserNo,
        String receiptType,
        String inputMethod,
        String imageUrl,
        String ocrStatus,
        String storeName,
        Integer totalAmount,
        Integer amount,
        String address,
        BigDecimal centerLat,
        BigDecimal centerLng,
        Long splitGroupId,
        Boolean goodPriceMatched,
        String goodPriceStoreId,
        String goodPriceStoreName,
        String goodPriceStoreAddress,
        LocalDateTime goodPriceVerifiedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<SplitResponse> splits
) {
    public record SplitResponse(
            Long roomMemberId,
            String userName,
            Integer amount
    ) {}

    public static ReceiptResponse from(Receipt r) {
        return new ReceiptResponse(
                r.getReceiptId(),
                r.getRoom().getRoomNo(),
                r.getUploader().getUser().getUserNo(),
                r.getReceiptType().name(),
                r.getInputMethod().name(),
                r.getImageUrl(),
                r.getOcrStatus().name(),
                r.getStoreName(),
                r.getTotalAmount(),
                r.getAmount(),
                r.getAddress(),
                r.getCenterLat(),
                r.getCenterLng(),
                r.getSplitGroup() == null ? null : r.getSplitGroup().getSplitGroupId(),
                r.getGoodPriceMatched(),
                r.getGoodPriceStoreId(),
                r.getGoodPriceStoreName(),
                r.getGoodPriceStoreAddress(),
                r.getGoodPriceVerifiedAt(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                r.getSplits().stream()
                        .map(s -> new SplitResponse(
                                s.getRoomMember().getRoomMemberId(),
                                s.getRoomMember().getUser().getUserName(),
                                s.getAmount()))
                        .toList()
        );
    }
}
