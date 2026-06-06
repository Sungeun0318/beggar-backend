package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.user.UserRequest;
import com.beggar.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ApiResponse<Void> signup(@Valid @RequestBody UserRequest userRequest) {
        userService.userSignup(userRequest);
        return ApiResponse.success();
    }
}
