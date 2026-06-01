package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.auth.TokenResponse;
import com.beggar.api.entity.User;
import com.beggar.api.repository.UserRepository;
import com.beggar.api.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClient;

@Service
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Qualifier("kakaoWebClient")
    private final WebClient kakaoWebClient;

    // * loginWithKakao — 카카오 액세스 토큰 → 카카오 계정 이메일/성별/연령대 조회 → 자체 JWT 발급
    // * 신규 회원인 경우 users 테이블만 INSERT 수행 (email 기준 식별, 전역 거지력 행 미생성)

    // TODO: loginWithKakao — 카카오 액세스 토큰 → 카카오 계정 이메일/성별/연령대 조회 → 자체 JWT 발급
    @Transactional
    public TokenResponse loginWithKakao(String kakaoToken){
        // 1. 카카오 WebClient를 사용하여 유저프로필 및 계정 정보 조회
        KakaoUserInfoResponse kakaoResponse = kakaoWebClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer "+ kakaoToken)
                .retrieve()
                .bodyToMono(KakaoUserInfoResponse.class)
                .onErrorMap(e -> new CustomException(ErrorCode.KAKAO_LOGIN_FAILED, "카카오 서버 통신 중 오류가 발생했습니다."))
                .block();
            if(kakaoResponse == null || kakaoResponse.kakao_account() == null || kakaoResponse.kakao_account().email() == null){
                throw new CustomException(ErrorCode.KAKAO_LOGIN_FAILED, "카카오 이메일 정보 획득에 실패했거나 사용자 동의가 필요합니다. ");
            }
            String email = kakaoResponse.kakao_account().email();
            String nickname = (kakaoResponse.kakao_account().profile() != null) ? kakaoResponse.kakao_account().profile().nickname(): "거지방주민";
            String profileImageUrl = (kakaoResponse.kakao_account().profile() != null ) ? kakao_account().profile().profile_image_url() : null;
        // 2. email 기준으로 기존 유저 조회 및 자동 가입 처리
        User user = userRepository.findByEmail(email).orElseGet(() -> {
           // Unique 제약 조건 충돌 방지를 위한 유동적 userName 처리 기법
            String uniqueUserName = nickname;
            if(userRepository.existsByUserName(uniqueUserName)){
                uniqueUserName = nickname + "_" + (System.currentTimeMillis() % 10000);
            }
            User newUser = User.builder()
                    .email(email)
                    .userName(uniqueUserName)
                    .profileImageUrl(profileImageUrl)
                    .role("USER")
                    .build();
            // 전역 거지력 스코어 행을 추가 생성하지 않고 오직 users만 insert
            return userRepository.save(newUser);
        });
        // 3. 자체 인가 아키텍쳐에 맞추어 JWT 발급 후 Response 조립
        String accessToken = jwtTokenProvider.createToken(user.getUserNo());
        String refreshToken = jwtTokenProvider.createToken(user.getUserNo());
        return new TokenResponse(accessToken, refreshToken, user.getUserNo(), user.getUserName());
    }
    // TODO: refresh        — 리프레시 토큰 검증 → 액세스 토큰 재발급
    @Transactional
    public TokenResponse refresh(String refreshToken){
        // 1. 전달받은 리프레시 토큰의 서명 및 만료 체크
        if(refreshToken == null || !jwtTokenProvider.isValid(refreshToken)){
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

    // TODO: signOut        — (필요 시) 리프레시 블랙리스트
    public void signOut(String token) {// 토큰의 남은 유효시간만큼 블랙리스트 세팅을 처리
    }

    // TODO: withdraw       — 회원 탈퇴
    @Transactional
    public void withdraw(Long userNo){
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }
    // ─── 카카오 API Jackson 바인딩용 내부 매핑 Record 정의 ───
    private record KakaoUserInfoResponse(
            Long id,
            KakaoAccount kakao_account
    ){
        private record KakaoAccount(
                Profile profile,
                String email,
                String gender,
                String age_range
        ){
            private record Profile(
                    String nickname,
                    String profile_image_url
            ){}
        }
    }
}
