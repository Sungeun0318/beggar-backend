package com.beggar.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record KakaoCodeLoginRequest(
        @NotBlank String code,
        @NotBlank String redirectUri
) {}
