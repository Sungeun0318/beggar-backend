package com.beggar.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.auth.TokenResponse;
import com.beggar.api.dto.user.UserRequest;
import com.beggar.api.entity.User;
import com.beggar.api.repository.UserRepository;
import com.beggar.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder; // 비밀번호 해시 비교를 위한 의존성 주입
    @Qualifier("kakaoWebClient")
    private final WebClient kakaoWebClient;
    @Qualifier("kakaoAuthWebClient")
    private final WebClient kakaoAuthWebClient;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    @Value("${kakao.client-secret:}")
    private String kakaoClientSecret;

    // 일반회원 로그인
    @Transactional
    public TokenResponse loginWithEmail(UserRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND, "이메일 또는 비밀번호가 일치하지 않습니다.");
        }
        String accessToken = jwtTokenProvider.createToken(user.getUserNo());
        String refreshToken = jwtTokenProvider.createToken(user.getUserNo());

        return new TokenResponse(accessToken, refreshToken, user.getUserNo(), user.getUserName());


    }

    @Transactional
    public TokenResponse loginWithKakao(String kakaoToken) {
        JsonNode kakaoResponse = kakaoWebClient.get()
                .uri("/v2/user/me")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + kakaoToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorMap(e -> new CustomException(ErrorCode.KAKAO_LOGIN_FAILED, "카카오 서버 통신 중 오류가 발생했습니다."))
                .block();
        Long kakaoId = kakaoLong(kakaoResponse, "id");
        if (kakaoResponse == null || kakaoId == null) {
            log.warn("Kakao user response missing id: {}", kakaoResponse);
            throw new CustomException(ErrorCode.KAKAO_LOGIN_FAILED, "카카오 계정 정보를 불러올 수 없습니다.");
        }
        JsonNode account = kakaoResponse.path("kakao_account");
        JsonNode profile = account.path("profile");
        String email = kakaoEmail(kakaoId, account);
        String rawAgeRange = kakaoText(account, "age_range");
        String profileImageUrl = kakaoText(profile, "thumbnail_image_url");

        Integer parsedGender = parseKakaoGender(kakaoText(account, "gender"));

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    String nickname = "거지_" + UUID.randomUUID().toString().substring(0, 6);

                    User newUser = User.builder()
                            .userName(nickname)
                            .email(email)
                            .profileImageUrl(profileImageUrl)
                            .gender(parsedGender)
                            .ageRange(rawAgeRange)
                            .role("USER")
                            .build();
                    return userRepository.save(newUser);
                });
        String accessToken = jwtTokenProvider.createToken(user.getUserNo());
        String refreshToken = jwtTokenProvider.createToken(user.getUserNo());

        return new TokenResponse(accessToken, refreshToken, user.getUserNo(), user.getUserName());

    }

    @Transactional
    public TokenResponse loginWithKakaoCode(String code, String redirectUri) {
        String accessToken = requestKakaoAccessToken(code, redirectUri);
        return loginWithKakao(accessToken);
    }

    private String requestKakaoAccessToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakaoRestApiKey);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (StringUtils.hasText(kakaoClientSecret)) {
            form.add("client_secret", kakaoClientSecret);
        }

        JsonNode tokenResponse = kakaoAuthWebClient.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(form)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorMap(WebClientResponseException.class, e -> {
                    String responseBody = e.getResponseBodyAsString();
                    log.warn("Kakao token exchange failed. status={}, body={}", e.getStatusCode(), responseBody);
                    return new CustomException(
                            ErrorCode.KAKAO_LOGIN_FAILED,
                            "카카오 토큰 교환에 실패했습니다: " + responseBody
                    );
                })
                .onErrorMap(e -> !(e instanceof CustomException),
                        e -> new CustomException(ErrorCode.KAKAO_LOGIN_FAILED, "카카오 토큰 교환에 실패했습니다."))
                .block();

        String accessToken = kakaoText(tokenResponse, "access_token");
        if (accessToken == null) {
            log.warn("Kakao token response missing access_token: {}", tokenResponse);
            throw new CustomException(ErrorCode.KAKAO_LOGIN_FAILED, "카카오 access token을 발급받지 못했습니다.");
        }

        return accessToken;
    }

    private Integer parseKakaoGender(String gender) {
        if ("male".equalsIgnoreCase(gender)) {
            return 0;
        }
        if ("female".equalsIgnoreCase(gender)) {
            return 1;
        }
        return null;
    }

    private String kakaoEmail(Long kakaoId, JsonNode account) {
        String email = kakaoText(account, "email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        return "kakao_" + kakaoId + "@kakao.local";
    }

    private Long kakaoLong(JsonNode node, String fieldName) {
        if (node == null || !node.hasNonNull(fieldName)) {
            return null;
        }
        return node.get(fieldName).asLong();
    }

    private String kakaoText(JsonNode node, String fieldName) {
        if (node == null || !node.hasNonNull(fieldName)) {
            return null;
        }
        String value = node.get(fieldName).asText();
        return value.isBlank() ? null : value;
    }

// TODO: refresh        — 리프레시 토큰 검증 → 액세스 토큰 재발급
@Transactional
public TokenResponse refresh(String refreshToken) {
    // 1. 전달받은 리프레시 토큰의 서명 및 만료 체크
    if (refreshToken == null || !jwtTokenProvider.isValid(refreshToken)) {
        throw new CustomException(ErrorCode.INVALID_TOKEN);
    }
    // 2. 토큰 claims 내부에서 userNo 추출
    Long userNo = jwtTokenProvider.parseUserNo(refreshToken);
    // 3. 실제 존재하는 유저인지 영속성 컨텍스트 검증
    User user = userRepository.findById(userNo)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    // 4. 새로운 토큰 체인 구성 및 응답 반환
    String newAccessToken = jwtTokenProvider.createToken(user.getUserNo());
    String newRefreshToken = jwtTokenProvider.createToken(user.getUserNo());

    return new TokenResponse(newAccessToken, newRefreshToken, user.getUserNo(), user.getUserName());
}

// TODO: signOut        — 로그아웃
public void signOut(String token) {// 토큰의 남은 유효시간만큼 블랙리스트 세팅을 처리
}

// TODO: withdraw       — 회원 탈퇴
@Transactional
public void withdraw(Long userNo) {
    User user = userRepository.findById(userNo)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    userRepository.delete(user);
}

/*
// ─── 카카오 API Jackson 바인딩용 내부 매핑 Record 정의 ───
private record KakaoUserInfoResponse(
        Long id,
        KakaoAccount kakao_account
) {
    private record KakaoAccount(
            Profile profile,
            String email,
            String gender,
            String age_range
    ) {
        private record Profile(
                String nickname,
                String profile_image_url
        ) {
        }
    }

 */
}
