package com.beggar.api.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter @NoArgsConstructor
@ToString
public class UserRequest {
    @Size(max = 15)
    private String userName;

    @NotBlank
    @Size(min = 4, max = 100)
    private String password;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 500)
    private String profileImageUrl;

    private Integer gender;

    @Size(max = 20)
    private String ageRange;
}
