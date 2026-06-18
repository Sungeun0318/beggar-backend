package com.beggar.api.dto.admin.ai;

import java.time.LocalDateTime;
import java.util.List;

public record SpendingInsightRequest(
        List<RoomInsightItem> rooms,
        List<ReceiptInsightItem> receipts,
        List<RecommendationInteractionItem> recommendationInteractions
) {
    public record RoomInsightItem(
            Long roomNo,
            String roomName,
            String location,
            String tag,
            Integer totalBudget,
            Integer memberCount,
            String status
    ) {
    }

    public record ReceiptInsightItem(
            Long receiptId,
            Long roomNo,
            Integer amount,
            String storeName,
            String receiptType,
            Boolean goodPriceMatched,
            LocalDateTime receiptIssuedAt
    ) {
    }

    public record RecommendationInteractionItem(
            Long roomNo,
            String requestedTag,
            String action,
            Integer expectedPrice,
            LocalDateTime createdAt
    ) {
    }
}
