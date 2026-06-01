package com.beggar.api.service;

//import com.beggar.api.common.exception.CustomException;
//import com.beggar.api.common.exception.ErrorCode;
//import com.beggar.api.dto.auth.TokenResponse;
//import com.beggar.api.repository.UserRepository;
//import com.beggar.api.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.reactive.function.client.WebClient;
//
@Service
@Transactional(readOnly = true)
public class AuthService {
//    private final UserRepository userRepository;
//    private final JwtTokenProvider jwtTokenProvider;
//
//    @Qualifier("kakaoWebClient")
//    private final WebClient kakaoWebClient;
//
//    // * loginWithKakao — 카카오 액세스 토큰 → 카카오 계정 이메일/성별/연령대 조회 → 자체 JWT 발급
//    // * 신규 회원인 경우 users 테이블만 INSERT 수행 (uemail 기준 식별, 전역 거지력 행 미생성)
//
//    // TODO: loginWithKakao — 카카오 액세스 토큰 → 카카오 계정 이메일/성별/연령대 조회 → 자체 JWT 발급
//    @Transactional
//    public TokenResponse loginWithKakao(String kakaoToken){
//        // 1. 카카오 WebClient를 사용하여 유저프로필 및 계정 정보 조회
//        KakaoUserInfoResponse kakaoResponse = kakaoWebClient.get()
//                .uri("/v2/user/me")
//                .header("Authorization", "Bearer "+ kakaoToken)
//                .retrieve()
//                .bodyToMono(KakaoUserInfoResponse.class)
//                .onErrorMap(e -> new CustomException(ErrorCode.KAKAO_LOGIN_FAILED, "카카오 서버 통신 중 오류가 발생했습니다."))
//                .block();
//        if(kakaoResponse == null || kakaoResponse.kakao_account() == null || ? kakaoResponse.kakao_account().email() == null){
//
//        }
//    }
//    //                       신규면 users만 INSERT. 카카오 식별은 uemail 기준, 전역 거지력 행은 만들지 않음
//    // TODO: refresh        — 리프레시 토큰 검증 → 액세스 토큰 재발급
//    // TODO: signOut        — (필요 시) 리프레시 블랙리스트
//    // TODO: withdraw       — 회원 탈퇴
}
