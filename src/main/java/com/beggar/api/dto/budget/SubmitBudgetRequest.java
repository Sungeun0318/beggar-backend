package com.beggar.api.dto.budget;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitBudgetRequest(
        @NotNull @Min(0) Integer budgetAmount
) {}
