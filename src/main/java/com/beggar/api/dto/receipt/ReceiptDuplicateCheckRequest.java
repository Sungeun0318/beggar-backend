package com.beggar.api.dto.receipt;

import com.beggar.api.entity.Receipt;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReceiptDuplicateCheckRequest(
        @NotNull Receipt.ReceiptType receiptType,
        String storeName,
        String address,
        @NotNull @Min(0) Integer amount,
        Long splitGroupId,
        LocalDateTime receiptIssuedAt,
        Long excludeReceiptId
) {
}
