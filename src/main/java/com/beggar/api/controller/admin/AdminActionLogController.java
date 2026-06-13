package com.beggar.api.controller.admin;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.admin.AdminActionLogListItem;
import com.beggar.api.service.admin.AdminActionLogService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AdminActionLogController {

    private final AdminActionLogService actionLogService;

    public AdminActionLogController(AdminActionLogService actionLogService) {
        this.actionLogService = actionLogService;
    }

    @GetMapping("/admin/logs")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String adminUsername,
            @RequestParam(defaultValue = "") String action,
            @RequestParam(defaultValue = "") String targetType,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<AdminActionLogListItem> logs = actionLogService.getLogs(
                adminUsername,
                action,
                targetType,
                keyword,
                page
        );

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "운영 로그");
        data.put("pageDescription", "관리자 변경 액션을 조회하고 감사 기록을 확인해.");
        data.put("activeMenu", "logs");
        data.put("logs", logs);
        data.put("adminUsername", adminUsername);
        data.put("action", action);
        data.put("targetType", targetType);
        data.put("keyword", keyword);

        return ApiResponse.success(data);
    }

    @GetMapping("/admin/logs/{logId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long logId) {
        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "운영 로그 상세");
        data.put("pageDescription", "관리자 액션의 대상과 내용을 확인해.");
        data.put("activeMenu", "logs");
        data.put("log", actionLogService.getLog(logId));

        return ApiResponse.success(data);
    }
}
