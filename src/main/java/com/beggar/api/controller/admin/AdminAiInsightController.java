package com.beggar.api.controller.admin;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.admin.ai.BudgetRiskPredictionResponse;
import com.beggar.api.dto.admin.ai.SpendingInsightResponse;
import com.beggar.api.service.admin.AdminAiInsightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAiInsightController {

    private final AdminAiInsightService adminAiInsightService;

    public AdminAiInsightController(AdminAiInsightService adminAiInsightService) {
        this.adminAiInsightService = adminAiInsightService;
    }

    @GetMapping("/admin/ai/insights/spending-summary")
    public ApiResponse<SpendingInsightResponse> spendingSummary() {
        return ApiResponse.success(adminAiInsightService.getSpendingSummary());
    }

    @GetMapping("/admin/ai/predictions/budget-risk")
    public ApiResponse<BudgetRiskPredictionResponse> budgetRiskPredictions() {
        return ApiResponse.success(adminAiInsightService.getBudgetRiskPredictions());
    }
}
