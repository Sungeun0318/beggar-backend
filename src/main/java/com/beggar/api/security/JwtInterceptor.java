package com.beggar.api.security;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty() || !jwtTokenProvider.isValid(token)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        request.setAttribute("userNo", jwtTokenProvider.parseUserNo(token));
        return true;
    }
}
