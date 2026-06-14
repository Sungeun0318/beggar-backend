package com.beggar.api.entity;

import com.beggar.api.common.BaseTimeEntity;
import com.beggar.api.dto.user.UserRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_user_name", columnNames = "user_name"),
                @UniqueConstraint(name = "uk_users_uemail",    columnNames = "uemail")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_no")
    private Long userNo;

    @Column(name = "user_name", length = 15, nullable = false)
    private String userName;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "uemail", length = 100, nullable = false)
    private String email;

    @Column(name = "role", length = 20, nullable = false)
    private String role;

    @Column(name = "gender")
    private Integer gender;

    @Column(name = "age_range", length = 20)
    private String ageRange;

    @Builder
    public User(String userName, String passwordHash, String profileImageUrl, String email,
                String role, Integer gender, String ageRange) {
        this.userName = userName;
        this.passwordHash = passwordHash;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
        this.role = (role == null) ? "USER" : role;
        this.gender = gender;
        this.ageRange = ageRange;
    }

    // 회원가입용 정적 팩토리 메서드 추가 (UserService)
    public static User signup(UserRequest requestDto, String encodedPassword) {
        return User.builder()
                .userName(requestDto.getUserName())
                .passwordHash(encodedPassword)
                .email(requestDto.getEmail())
                .profileImageUrl(requestDto.getProfileImageUrl())
                .gender(requestDto.getGender())
                .ageRange(requestDto.getAgeRange())
                .role("USER") // 기본 권한 세팅
                .build();
    }

    public void updateProfile(String userName, String profileImageUrl) {
        if (userName != null) this.userName = userName;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }

    public void updateKakaoLoginInfo(String userName, String profileImageUrl, Integer gender, String ageRange) {
        updateProfile(userName, profileImageUrl);
        if (gender != null) this.gender = gender;
        if (ageRange != null) this.ageRange = ageRange;
    }
}
