package com.beggar.api.dto.ranking;

import com.beggar.api.entity.UserBeggarScore;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BeggarScoreResponse(
        Long userNo,
        Integer score,
        String title,
        Long totalSavedAmount,
        BigDecimal budgetComplianceRate,
        BigDecimal avgSavingsRatio,
        Integer participationCount,
        LocalDateTime lastCalculatedAt
) {
    public static BeggarScoreResponse from(UserBeggarScore s) {
        return new BeggarScoreResponse(
                s.getUser().getUserNo(),
                s.getScore(),
                s.getTitle(),
                s.getTotalSavedAmount(),
                s.getBudgetComplianceRate(),
                s.getAvgSavingsRatio(),
                s.getParticipationCount(),
                s.getLastCalculatedAt()
        );
    }
}
