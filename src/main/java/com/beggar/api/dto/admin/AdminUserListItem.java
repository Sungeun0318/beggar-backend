package com.beggar.api.dto.admin;

import java.time.LocalDateTime;

public record AdminUserListItem(
        Long userNo,
        String userName,
        String profileImageUrl,
        String email,
        String role,
        Integer gender,
        String ageRange,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
