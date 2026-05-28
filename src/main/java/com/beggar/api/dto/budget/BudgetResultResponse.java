package com.beggar.api.dto.budget;

import com.beggar.api.entity.RoomBudgetResult;

import java.time.LocalDateTime;

public record BudgetResultResponse(
        Long roomNo,
        Integer minBudgetPerPerson,
        Integer memberCount,
        Integer totalBudget,
        LocalDateTime confirmedAt
) {
    public static BudgetResultResponse from(RoomBudgetResult r) {
        return new BudgetResultResponse(
                r.getRoom().getRoomNo(),
                r.getMinBudgetPerPerson(),
                r.getMemberCount(),
                r.getTotalBudget(),
                r.getConfirmedAt()
        );
    }
}
