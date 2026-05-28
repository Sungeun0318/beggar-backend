package com.beggar.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    // TODO: GET    /users/me               — 내 프로필 조회
    // TODO: GET    /users/me/beggar-score  — 내 거지력 점수/칭호
    // TODO: DELETE /users/me               — 회원 탈퇴
}
