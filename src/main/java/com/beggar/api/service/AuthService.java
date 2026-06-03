package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.config.PasswordEncoderConfig;
import com.beggar.api.dto.auth.KakaoLoginRequest;
import com.beggar.api.dto.auth.TokenResponse;
import com.beggar.api.dto.user.UserRequest;
import com.beggar.api.entity.User;
import com.beggar.api.repository.UserRepository;
import com.beggar.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder; // 비밀번호 해시 비교를 위한 의존성 주입

    // 카카오 통신용 WebClient
    @Qualifier("kakaoWebClient")
    private final WebClient kakaoWebClient;

    // 일반회원 로그인
    @Transactional
    public TokenResponse loginWithEmail(UserRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "이메일 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND, "이메일 또는 비밀번호가 일치하지 않습니다.");
        }
        String accessToken = jwtTokenProvider.createToken(user.getUserNo());
        String refreshToken = jwtTokenProvider.createToken(user.getUserNo());

        return new TokenResponse(accessToken, refreshToken, user.getUserNo(), user.getUserName());
    }

    // 카카오 유저 정보 매핑용 record
    public record KakaoUserInfoResponse(Long id, KakaoAccount kakao_account) {
        public record KakaoAccount(String email, String gender, String age_range, Profile profile) {
            public record Profile(String nickname, String thumbnail_image_url) {
            }
        }
    }

    // TODO: loginWithKakao — 카카오 액세스 토큰 → 카카오 계정 이메일/성별/연령대 조회 → 자체 JWT 발급
    @Transactional
    public TokenResponse loginWithKakao(String kakaoToken) {

        // 1. 카카오 WebClient를 사용하여 유저프로필 및 계정 정보 조회
        KakaoUserInfoResponse kakaoResponse = kakaoWebClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer " + kakaoToken)
                .retrieve().bodyToMono(KakaoUserInfoResponse.class)
                .onErrorMap(e -> new CustomException(ErrorCode.KAKAO_LOGIN_FAILED, "카카오 서버 통신 중 오류가 발생했습니다.")).block();

        // 2. 카카오 응답 데이터 전송
        if (kakaoResponse == null || kakaoResponse.kakao_account() == null || kakaoResponse.kakao_account().email() == null) {
            throw new CustomException(ErrorCode.KAKAO_LOGIN_FAILED, "카카오 계정 정보 또는 이메일을 불러올 수 업습니다. ");
        }
        String email = kakaoResponse.kakao_account().email();

        // 나이 가공
        String rawAgeRange = kakaoResponse.kakao_account().age_range(); // 카카오가 제공한 연령대 데이터, 예: "20~29"
        Integer parsedAge = null;
        if (rawAgeRange != null && rawAgeRange.contains("~")) {
            parsedAge = Integer.parseInt(rawAgeRange.split("~")[0]);
        }
        final Integer finalAge = parsedAge;

        // 성별 가공(male -> 0, female -> 1)
        Integer parsedGender = null;
        String gender = kakaoResponse.kakao_account().gender();
        if ("male".equalsIgnoreCase(gender)) parsedGender = 0;
        else if ("female".equalsIgnoreCase(gender)) parsedGender = 1;
        final Integer finalGender = parsedGender;

        // 3. DB에서 이메일로 유저 조회, 없으면 자동회원가입 진행
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            String nickname = "거지_" + UUID.randomUUID().toString().substring(0, 6);

            User newUser = User.builder()
                    .userName(nickname).email(email).profileImageUrl(kakaoResponse.kakao_account().profile() != null ?
                            kakaoResponse.kakao_account().profile().thumbnail_image_url() : null)
                    .gender(finalGender).ageRange(finalAge).role("USER")
                    .build();
            return userRepository.save(newUser); // 거지력 스코어 행을 추가 생성하지 않고 오직 users만 insert
        });
        String accessToken = jwtTokenProvider.createToken(user.getUserNo());
        String refreshToken = jwtTokenProvider.createToken(user.getUserNo());

        return new TokenResponse(accessToken, refreshToken, user.getUserNo(), user.getUserName());
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
        User user = userRepository.findById(userNo).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
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
        User user = userRepository.findById(userNo).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }
}





