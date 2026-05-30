package com.beggar.api.dto.ranking;

import com.beggar.api.entity.RoomBeggarScore;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BeggarScoreResponse(
        Long roomNo,
        Integer score,
        String title,
        Long totalSpentAmount,
        Long totalSavedAmount,
        Integer goodPriceVerifiedCount,
        BigDecimal budgetComplianceRate,
        BigDecimal avgSavingsRatio,
        LocalDateTime lastCalculatedAt
) {
    public static BeggarScoreResponse from(RoomBeggarScore s) {
        return new BeggarScoreResponse(
                s.getRoom().getRoomNo(),
                s.getScore(),
                s.getTitle(),
                s.getTotalSpentAmount(),
                s.getTotalSavedAmount(),
                s.getGoodPriceVerifiedCount(),
                s.getBudgetComplianceRate(),
                s.getAvgSavingsRatio(),
                s.getLastCalculatedAt()
        );
    }
}
