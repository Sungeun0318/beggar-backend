package com.beggar.api.dto.admin.ai;

import java.util.List;

public record SpendingInsightResponse(
        SpendingSummary summary,
        List<RegionSpending> topRegions,
        List<TagClickCount> tagClicks,
        List<HighBudgetUsageRoom> highBudgetUsageRooms
) {
    public record SpendingSummary(
            Integer totalSpentAmount,
            Integer averageReceiptAmount,
            Double budgetOverRoomRate,
            Double goodPriceUsageRate
    ) {
    }

    public record RegionSpending(
            String region,
            Integer spentAmount
    ) {
    }

    public record TagClickCount(
            String tag,
            Integer clickCount
    ) {
    }

    public record HighBudgetUsageRoom(
            Long roomNo,
            String roomName,
            Integer totalBudget,
            Integer spentAmount,
            Double usageRate
    ) {
    }
}
