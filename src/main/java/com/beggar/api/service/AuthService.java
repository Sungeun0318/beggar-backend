package com.beggar.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    // TODO: loginWithKakao — 카카오 액세스 토큰 → 카카오 계정 이메일/성별/연령대 조회 → 자체 JWT 발급
    //                       신규면 users만 INSERT. 카카오 식별은 uemail 기준, 전역 거지력 행은 만들지 않음
    // TODO: refresh        — 리프레시 토큰 검증 → 액세스 토큰 재발급
    // TODO: signOut        — (필요 시) 리프레시 블랙리스트
    // TODO: withdraw       — 회원 탈퇴
}
