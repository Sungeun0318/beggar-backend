package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.user.UserRequest;
import com.beggar.api.security.JwtTokenProvider;
import com.beggar.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    // ToDO : Post /users/signup            - 회원가입
    @PostMapping("/signup")
    public ApiResponse<Void> signup(@RequestBody UserRequest userRequest){
        userService.userSignup(userRequest);
        System.out.println("userRequest = " + userRequest);
        return ApiResponse.success();
    }

//     TODO: GET    /users/me               — 내 프로필 조회
//     TODO: GET    /users/me/beggar-score  — 내 거지력 점수/칭호
//     TODO: DELETE /users/me               — 회원 탈퇴
}
