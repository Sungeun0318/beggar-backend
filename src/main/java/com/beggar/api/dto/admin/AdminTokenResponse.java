package com.beggar.api.dto.admin;

public record AdminTokenResponse(
        String accessToken,
        String username,
        String role
) {
}
