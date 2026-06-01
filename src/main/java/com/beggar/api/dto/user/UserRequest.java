package com.beggar.api.dto.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @NoArgsConstructor
@ToString
public class UserRequest {
    private String userName;
    private String password;
    private String email;
    private String profileImageUrl;
    private Integer gender;
    private Integer age;
}
