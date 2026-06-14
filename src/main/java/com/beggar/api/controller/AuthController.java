package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.auth.KakaoCodeLoginRequest;
import com.beggar.api.dto.auth.KakaoLoginRequest;
import com.beggar.api.dto.auth.RefreshTokenRequest;
import com.beggar.api.dto.auth.TokenResponse;
import com.beggar.api.dto.user.UserRequest;
import com.beggar.api.security.LoginUser;
import com.beggar.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> loginWithEmail(@Valid @RequestBody UserRequest request) {
        TokenResponse response = authService.loginWithEmail(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/kakao")
    public ApiResponse<TokenResponse> loginWithKakao(@Valid @RequestBody KakaoLoginRequest request) {
        TokenResponse tokenResponse = authService.loginWithKakao(
                request.kakaoAccessToken(),
                request.email(),
                request.gender(),
                request.age()
        );
        return ApiResponse.success(tokenResponse);
    }

    @PostMapping("/kakao/code")
    public ApiResponse<TokenResponse> loginWithKakaoCode(@Valid @RequestBody KakaoCodeLoginRequest request) {
        TokenResponse tokenResponse = authService.loginWithKakaoCode(
                request.code(),
                request.redirectUri(),
                request.email(),
                request.gender(),
                request.age()
        );
        return ApiResponse.success(tokenResponse);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = authService.refresh(request.refreshToken());
        return ApiResponse.success(tokenResponse);
    }

    @PostMapping("/signout")
    public ApiResponse<Void> signOut(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.signOut(token);
        }
        return ApiResponse.success();
    }

    @DeleteMapping("/withdraw")
    public ApiResponse<Void> withdraw(@LoginUser Long userNo) {
        authService.withdraw(userNo);
        return ApiResponse.success();
    }
}
