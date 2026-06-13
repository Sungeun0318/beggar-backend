package com.beggar.api.controller.admin;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.admin.DashboardStats;
import com.beggar.api.service.admin.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    public ApiResponse<Map<String, Object>> dashboard() {
        DashboardStats stats = dashboardService.getStats();

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "대시보드");
        data.put("pageDescription", "서비스 운영 지표를 한눈에 확인해.");
        data.put("activeMenu", "dashboard");
        data.put("stats", stats);
        data.put("recentUsers", dashboardService.getRecentUsers());
        data.put("recentRooms", dashboardService.getRecentRooms());
        data.put("recentPosts", dashboardService.getRecentPosts());

        return ApiResponse.success(data);
    }
}
