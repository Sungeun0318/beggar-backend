package com.beggar.api.dto.admin.ai;

import java.time.LocalDateTime;
import java.util.List;

public record BudgetRiskPredictionRequest(
        List<RoomRiskItem> rooms,
        List<ReceiptRiskItem> receipts,
        List<BudgetRiskItem> budgets
) {
    public record RoomRiskItem(
            Long roomNo,
            String roomName,
            Integer totalBudget,
            String location,
            String tag,
            Integer memberCount,
            String status,
            LocalDateTime roomCreated,
            LocalDateTime endedAt
    ) {
    }

    public record ReceiptRiskItem(
            Long roomNo,
            Integer amount,
            String receiptType,
            Boolean goodPriceMatched,
            LocalDateTime receiptIssuedAt
    ) {
    }

    public record BudgetRiskItem(
            Long roomNo,
            Integer budgetAmount,
            LocalDateTime submittedAt
    ) {
    }
}
