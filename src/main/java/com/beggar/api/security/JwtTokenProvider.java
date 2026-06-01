package com.beggar.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secretString;

    @Value("${jwt.acess-token-validity-ms}")
    private long accessTokenValidityMs;

    @Value("${jwt.refresh-token-validity-ms}")
    private long refreshTokenValidityMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }
    public String createAccessToken(Long userNo) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .subject(String.valueOf(userNo))
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }
    // token -> userNo 추출
    public Long parseUserNo(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }
    // 서명 검증 + 만료 체크
    public boolean isValid(String token){
        try{
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch( JwtException | IllegalArgumentException e){
            return false;
        }
    }
    
    // TODO: createAccessToken(userNo)   — userNo 를 subject 로 액세스 토큰 발급
    // TODO: createRefreshToken(userNo)  — 동일하게 리프레시 토큰 발급
    // TODO: parseUserNo(token)          — 토큰 → userNo 추출
    // TODO: isValid(token)              — 서명 검증 + 만료 체크


    // application.properties:
    //   jwt.secret, jwt.access-token-validity-ms, jwt.refresh-token-validity-ms
}
