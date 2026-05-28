package com.beggar.api.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    // TODO: preHandle()
    //   1) Authorization 헤더에서 "Bearer xxx" 추출
    //   2) JwtTokenProvider.isValid(token) 검증 (실패 시 CustomException(INVALID_TOKEN))
    //   3) userNo 를 request.setAttribute("userNo", ...) 로 저장
    //      → LoginUserArgumentResolver 가 꺼내 @LoginUser 에 주입
    //
    // 공개 엔드포인트(인터셉터 미적용)는 WebConfig.excludePathPatterns 로 제외:
    //   /auth/kakao, /auth/refresh, /error, /actuator/health
}
