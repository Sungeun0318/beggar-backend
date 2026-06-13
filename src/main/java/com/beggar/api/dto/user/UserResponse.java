package com.beggar.api.dto.user;

import com.beggar.api.entity.User;

public record UserResponse(
        Long userNo,
        String userName,
        String email,
        String profileImageUrl,
        String role,
        Integer gender,
        String ageRange,
        Integer score,
        String title
) {
    public static UserResponse from(User user, Integer score, String title) {
        return new UserResponse(
                user.getUserNo(),
                user.getUserName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getGender(),
                user.getAgeRange(),
                score,
                title
        );
    }
}
