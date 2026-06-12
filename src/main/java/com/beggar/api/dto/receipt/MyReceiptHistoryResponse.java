package com.beggar.api.dto.receipt;

import com.beggar.api.entity.Receipt;

import java.time.LocalDateTime;
import java.util.List;

public record MyReceiptHistoryResponse(
        List<Item> receipts,
        long totalAmount
) {
    public record Item(
            Long receiptId,
            Long roomNo,
            String roomName,
            String receiptType,
            Integer amount,
            LocalDateTime createdAt
    ) {
        public static Item from(Receipt receipt) {
            return new Item(
                    receipt.getReceiptId(),
                    receipt.getRoom().getRoomNo(),
                    receipt.getRoom().getRoomName(),
                    receipt.getReceiptType().name(),
                    receipt.getAmount(),
                    receipt.getCreatedAt()
            );
        }
    }

    public static MyReceiptHistoryResponse from(List<Receipt> receipts) {
        long totalAmount = receipts.stream()
                .mapToLong(receipt -> receipt.getAmount() == null ? 0L : receipt.getAmount())
                .sum();

        return new MyReceiptHistoryResponse(
                receipts.stream()
                        .map(Item::from)
                        .toList(),
                totalAmount
        );
    }

    public static MyReceiptHistoryResponse empty() {
        return new MyReceiptHistoryResponse(List.of(), 0L);
    }
}
