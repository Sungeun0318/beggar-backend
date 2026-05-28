package com.beggar.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank String kakaoAccessToken
) {}
