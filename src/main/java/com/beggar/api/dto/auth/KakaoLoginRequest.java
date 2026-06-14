package com.beggar.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record KakaoLoginRequest(
        @NotBlank String kakaoAccessToken,
        @NotBlank @Email String email,
        @NotNull Integer gender,
        @NotNull @Min(0) @Max(120) Integer age
) {}
