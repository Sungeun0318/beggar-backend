package com.beggar.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    // TODO: GET    /users/me      — 내 프로필 조회 (gender/ageRange 포함)
    // TODO: DELETE /users/me      — 회원 탈퇴
    // 방별 거지평가는 /rooms/{roomNo}/beggar-score에서 조회한다.
}
