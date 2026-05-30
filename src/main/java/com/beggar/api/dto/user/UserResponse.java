package com.beggar.api.dto.user;

import com.beggar.api.entity.User;

public record UserResponse(
        Long userNo,
        String userName,
        String email,
        String profileImageUrl,
        String gender,
        String ageRange,
        String role
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserNo(),
                user.getUserName(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getGender(),
                user.getAgeRange(),
                user.getRole()
        );
    }
}
