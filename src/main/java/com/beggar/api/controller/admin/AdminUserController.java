package com.beggar.api.controller.admin;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.admin.AdminUserListItem;
import com.beggar.api.dto.admin.UserDetail;
import com.beggar.api.service.admin.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/admin/users")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<AdminUserListItem> users = adminUserService.getUsers(keyword, page);

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "회원 관리");
        data.put("pageDescription", "회원 정보를 검색하고 상세 활동을 확인해.");
        data.put("activeMenu", "users");
        data.put("users", users);
        data.put("keyword", keyword);

        return ApiResponse.success(data);
    }

    @GetMapping("/admin/users/{userNo}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long userNo) {
        UserDetail user = adminUserService.getUserDetail(userNo);

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "회원 상세");
        data.put("pageDescription", "회원 기본 정보와 활동 요약을 확인해.");
        data.put("activeMenu", "users");
        data.put("user", user);

        return ApiResponse.success(data);
    }
}
