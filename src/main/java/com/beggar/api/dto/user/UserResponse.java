package com.beggar.api.dto.user;

import com.beggar.api.entity.User;

public record UserResponse(
        Long userNo,
        String userName,
        String email,
        String profileImageUrl,
        String role,
        Integer gender,
        Integer age
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserNo(),
                user.getUserName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getGender(),
                user.getAgeRange()
        );
    }
}
