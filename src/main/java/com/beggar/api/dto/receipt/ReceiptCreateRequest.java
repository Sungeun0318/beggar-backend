package com.beggar.api.dto.receipt;

import com.beggar.api.entity.Receipt;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ReceiptCreateRequest(
        Long roomNo,
        @NotNull Long uploaderUserNo,
        @NotNull Receipt.ReceiptType receiptType,
        @NotNull Receipt.InputMethod inputMethod,
        String imageUrl,
        String storeName,
        Integer totalAmount,
        String address,
        BigDecimal centerLat,
        BigDecimal centerLng,
        Long splitGroupId,
        @Min(0) Integer amount,
        List<SplitItem> splits
) {
    public record SplitItem(
            @NotNull Long roomMemberId,
            @NotNull @Min(0) Integer amount
    ) {}
}
