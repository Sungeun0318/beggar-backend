package com.beggar.api.security;

import lombok.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    // TODO: createAccessToken(userNo)   — userNo 를 subject 로 액세스 토큰 발급
    // TODO: createRefreshToken(userNo)  — 동일하게 리프레시 토큰 발급
    // TODO: parseUserNo(token)          — 토큰 → userNo 추출
    // TODO: isValid(token)              — 서명 검증 + 만료 체크


    // application.properties:
    //   jwt.secret, jwt.access-token-validity-ms, jwt.refresh-token-validity-ms
}
