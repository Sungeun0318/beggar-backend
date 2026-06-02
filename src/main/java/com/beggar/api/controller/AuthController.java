package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.auth.KakaoLoginRequest;
import com.beggar.api.dto.auth.RefreshTokenRequest;
import com.beggar.api.dto.auth.TokenResponse;
import com.beggar.api.dto.user.UserRequest;
import com.beggar.api.security.JwtTokenProvider;
import com.beggar.api.service.AuthService;
import com.beggar.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor // AuthService 생성을 위한 생성자 주입 자동화
public class AuthController {
    private final AuthService authService;

    // TODO: POST /auth/login      — 일반회원 로그인
    @PostMapping("/login")
    public ApiResponse<TokenResponse> loginWithEmail(@RequestBody UserRequest request){
        TokenResponse response = authService.loginWithEmail(request);
        return ApiResponse.success(response);
    }

//    // TODO: POST /auth/kakao    — 카카오 로그인
//    @PostMapping("/kakao")
//    public ApiResponse<TokenResponse> loginWithKakao(@Valid @RequestBody KakaoLoginRequest request){
//        TokenResponse tokenResponse = authService.loginWithKakao(request.kakaoAccessToken());
//        return ApiResponse.success(tokenResponse);
//    }
    // TODO: POST /auth/refresh  — 토큰 재발급
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request){
        TokenResponse tokenResponse = authService.refresh(request.refreshToken());
        return ApiResponse.success(tokenResponse);
    }
    // TODO: POST /auth/signout  — 로그아웃
    @PostMapping("/signout")
    public ApiResponse<Void> signOut(HttpServletRequest request){
        // Http 요청 헤더에서 Authorization 값을 가져옵니다.
        String authHeader = request.getHeader("Authorization");

        if( authHeader != null && authHeader.startsWith("Bearer ")){
            String token = authHeader.substring(7);
            authService.signOut(token); // 토큰 블랙리스트 처리를 위해 서비스단으로 넘겨줍니다.
    }
    return ApiResponse.success();// 데이터 필드 없는 공통 성공응답 반환
}
}
