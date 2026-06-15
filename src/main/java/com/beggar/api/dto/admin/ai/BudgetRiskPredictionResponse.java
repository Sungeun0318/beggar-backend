package com.beggar.api.dto.admin.ai;

import java.util.List;

public record BudgetRiskPredictionResponse(
        String modelVersion,
        String generatedAt,
        List<BudgetRiskPredictionItem> items
) {
    public record BudgetRiskPredictionItem(
            Long roomNo,
            String roomName,
            String riskLevel,
            Double riskScore,
            Integer predictedFinalSpentAmount,
            Double predictedBudgetUsageRate,
            Integer recommendedNextSpendLimit,
            String reason
    ) {
    }
}
