package com.beggar.api.dto.receipt;

import com.beggar.api.entity.Receipt;

import java.time.LocalDateTime;
import java.util.List;

public record ReceiptDuplicateCheckResponse(
        boolean hasDuplicate,
        List<Candidate> candidates
) {
    public record Candidate(
            Long receiptId,
            Long roomNo,
            Long uploaderUserNo,
            String receiptType,
            String inputMethod,
            String storeName,
            String address,
            Integer amount,
            Long splitGroupId,
            LocalDateTime receiptIssuedAt,
            LocalDateTime createdAt
    ) {
        public static Candidate from(Receipt receipt) {
            return new Candidate(
                    receipt.getReceiptId(),
                    receipt.getRoom().getRoomNo(),
                    receipt.getUploader().getUser().getUserNo(),
                    receipt.getReceiptType().name(),
                    receipt.getInputMethod().name(),
                    receipt.getStoreName(),
                    receipt.getAddress(),
                    receipt.getAmount(),
                    receipt.getSplitGroup() == null ? null : receipt.getSplitGroup().getSplitGroupId(),
                    receipt.getReceiptIssuedAt(),
                    receipt.getCreatedAt()
            );
        }
    }

    public static ReceiptDuplicateCheckResponse empty() {
        return new ReceiptDuplicateCheckResponse(false, List.of());
    }

    public static ReceiptDuplicateCheckResponse from(List<Receipt> receipts) {
        List<Candidate> candidates = receipts.stream()
                .map(Candidate::from)
                .toList();
        return new ReceiptDuplicateCheckResponse(!candidates.isEmpty(), candidates);
    }
}
