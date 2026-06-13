package com.beggar.api.dto.admin;

public record AdminLoginRequest(
        String username,
        String password
) {
}
