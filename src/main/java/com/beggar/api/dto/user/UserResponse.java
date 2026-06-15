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
        return from(user, score, title, user.getProfileImageUrl());
    }

    public static UserResponse from(User user, Integer score, String title, String profileImageUrl) {
        return new UserResponse(
                user.getUserNo(),
                user.getUserName(),
                user.getEmail(),
                profileImageUrl,
                user.getRole(),
                user.getGender(),
                user.getAgeRange(),
                score,
                title
        );
    }
}
