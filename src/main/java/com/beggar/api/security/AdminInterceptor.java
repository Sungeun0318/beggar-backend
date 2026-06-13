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
public class AdminInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_ROLE = "ADMIN";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new CustomException(ErrorCode.ADMIN_UNAUTHORIZED);
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty() || !jwtTokenProvider.isValid(token)) {
            throw new CustomException(ErrorCode.ADMIN_UNAUTHORIZED);
        }

        if (!ADMIN_ROLE.equals(jwtTokenProvider.parseRole(token))) {
            throw new CustomException(ErrorCode.ADMIN_FORBIDDEN);
        }

        request.setAttribute("adminId", jwtTokenProvider.parseUserNo(token));
        return true;
    }
}
