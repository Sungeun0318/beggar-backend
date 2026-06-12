package com.beggar.api.dto.auth;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        Long userNo,
        String userName,
        String email,
        String profileImageUrl
) {}
