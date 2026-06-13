package com.beggar.api.controller.admin;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.admin.AdminLoginRequest;
import com.beggar.api.dto.admin.AdminTokenResponse;
import com.beggar.api.service.admin.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("adminAuthController")
@RequiredArgsConstructor
public class AuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/admin/auth/login")
    public ApiResponse<AdminTokenResponse> login(@RequestBody AdminLoginRequest request) {
        return ApiResponse.success(adminAuthService.login(request));
    }
}
