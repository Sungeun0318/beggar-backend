package com.beggar.api.entity;

import com.beggar.api.common.BaseTimeEntity;
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
    private int gender;

    @Column(name = "age")
    private int age;

    @Builder
    public User(String userName, String passwordHash, String profileImageUrl, String email, String role) {
        this.userName = userName;
        this.passwordHash = passwordHash;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
        this.role = (role == null) ? "USER" : role;
    }

    public void updateProfile(String userName, String profileImageUrl) {
        if (userName != null) this.userName = userName;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
    }
}