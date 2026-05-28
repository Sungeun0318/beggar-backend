package com.beggar.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    // TODO: loginWithKakao — 카카오 액세스 토큰 → 자체 JWT 발급
    //                       신규면 users + user_beggar_scores INSERT (동일 트랜잭션)
    // TODO: refresh        — 리프레시 토큰 검증 → 액세스 토큰 재발급
    // TODO: signOut        — (필요 시) 리프레시 블랙리스트
    // TODO: withdraw       — 회원 탈퇴
}
