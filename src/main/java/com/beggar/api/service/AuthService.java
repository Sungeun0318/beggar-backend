    package com.beggar.api.service;

    import com.fasterxml.jackson.databind.JsonNode;
    import com.beggar.api.common.exception.CustomException;
    import com.beggar.api.common.exception.ErrorCode;
    import com.beggar.api.dto.auth.TokenResponse;
    import com.beggar.api.dto.user.UserRequest;
    import com.beggar.api.entity.Room;
    import com.beggar.api.entity.RoomMember;
    import com.beggar.api.entity.User;
    import com.beggar.api.repository.BudgetRepository;
    import com.beggar.api.repository.ReceiptRepository;
    import com.beggar.api.repository.ReceiptSplitRepository;
    import com.beggar.api.repository.RoomBeggarScoreRepository;
    import com.beggar.api.repository.RoomBudgetResultRepository;
    import com.beggar.api.repository.RoomFreeChatRepository;
    import com.beggar.api.repository.RoomFreeCommentRepository;
    import com.beggar.api.repository.RoomFreePostRepository;
    import com.beggar.api.repository.RoomMemberRepository;
    import com.beggar.api.repository.RoomPurposeTagRepository;
    import com.beggar.api.repository.RoomRepository;
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

    import java.util.List;

    @Service
    @Transactional(readOnly = true)
    @RequiredArgsConstructor
    @Slf4j
    public class AuthService {
        private final UserRepository userRepository;
        private final RoomRepository roomRepository;
        private final RoomMemberRepository roomMemberRepository;
        private final ReceiptRepository receiptRepository;
        private final ReceiptSplitRepository receiptSplitRepository;
        private final BudgetRepository budgetRepository;
        private final RoomFreePostRepository roomFreePostRepository;
        private final RoomFreeCommentRepository roomFreeCommentRepository;
        private final RoomFreeChatRepository roomFreeChatRepository;
        private final RoomPurposeTagRepository roomPurposeTagRepository;
        private final RoomBudgetResultRepository roomBudgetResultRepository;
        private final RoomBeggarScoreRepository roomBeggarScoreRepository;
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
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserNo());

            return tokenResponse(accessToken, refreshToken, user);


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
            String email = kakaoEmail(kakaoId, account);

            // 1. profile 객체 안전하게 추출
            JsonNode profile = account.path("profile");

            // 2. 카카오 실제 반환 Key 매핑 (동의하지 않았다면 null이 반환됨)
            String kakaoNicknameValue = kakaoText(profile, "nickname");
            System.out.println("kakaoNicknameValue = " + kakaoNicknameValue);

            String profile_nickname = kakaoText(profile, "profile_nickname");
            System.out.println("profile_nickname = " + profile_nickname);


            String profileImageUrl = kakaoText(profile, "profile_image_url");

            // 3. [핵심 방어] 닉네임 미동의(null) 시 자동 생성 규칙 적용
            if (!StringUtils.hasText(kakaoNicknameValue)) {
                kakaoNicknameValue = "kakao_" + kakaoId;
            }
            kakaoNicknameValue = trimToUserNameLimit(kakaoNicknameValue);

            // 4. [핵심 방어] 프로필 이미지 미동의(null) 시 기본 이미지 처리
            if (!StringUtils.hasText(profileImageUrl)) {
                profileImageUrl = "default_profile_url"; // 빈 문자열("") 혹은 프로젝트 내부의 기본 이미지 URL 상수를 넣어주세요.
            }

            // 5. 변수 effectively final 처리 (람다식 내부 사용용)
            String finalNickname = kakaoNicknameValue;
            String finalProfileImageUrl = profileImageUrl;

            // 6. 회원가입 (이메일, 닉네임, 프로필 사진만 저장)
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        String availableNickname = availableKakaoNickname(finalNickname, kakaoId, null);
                        User newUser = User.builder()
                                .userName(availableNickname)
                                .email(email)
                                .profileImageUrl(finalProfileImageUrl)
                                .role("USER")
                                .build();
                        return userRepository.save(newUser);
                    });

            // 7. 로그인 시마다 프로필 정보 갱신 (중복 방지 닉네임 적용)
            user.updateProfile(
                    availableKakaoNickname(kakaoNicknameValue, kakaoId, user.getUserNo()),
                    profileImageUrl
            );

            String accessToken = jwtTokenProvider.createToken(user.getUserNo());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserNo());

            return tokenResponse(accessToken, refreshToken, user);
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

        private String trimToUserNameLimit(String userName) {
            String normalized = userName == null ? "" : userName.trim();
            if (normalized.length() <= 15) {
                return normalized;
            }
            return normalized.substring(0, 15);
        }

        private String availableKakaoNickname(String nickname, Long kakaoId, Long currentUserNo) {
            if (!isDuplicateUserName(nickname, currentUserNo)) {
                return nickname;
            }

            String suffix = "_" + Long.toString(kakaoId).substring(Math.max(0, Long.toString(kakaoId).length() - 4));
            int baseLength = Math.max(1, 15 - suffix.length());
            String base = nickname.length() > baseLength ? nickname.substring(0, baseLength) : nickname;
            String candidate = base + suffix;

            if (!isDuplicateUserName(candidate, currentUserNo)) {
                return candidate;
            }

            return trimToUserNameLimit("kakao_" + kakaoId);
        }

        private boolean isDuplicateUserName(String nickname, Long currentUserNo) {
            if (currentUserNo == null) {
                return userRepository.existsByUserName(nickname);
            }
            return userRepository.existsByUserNameAndUserNoNot(nickname, currentUserNo);
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
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getUserNo());

        return tokenResponse(newAccessToken, newRefreshToken, user);
    }

    private TokenResponse tokenResponse(String accessToken, String refreshToken, User user) {
        return new TokenResponse(
                accessToken,
                refreshToken,
                user.getUserNo(),
                user.getUserName(),
                user.getEmail(),
                user.getProfileImageUrl()
        );
    }

    // TODO: signOut        — 로그아웃
    public void signOut(String token) {// 토큰의 남은 유효시간만큼 블랙리스트 세팅을 처리
    }

    // TODO: withdraw       — 회원 탈퇴
    @Transactional
    public void withdraw(Long userNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Room> ownedRooms = roomRepository.findByOwnerUserNo(userNo);
        List<RoomMember> memberships = roomMemberRepository.findByUser_UserNo(userNo);

        budgetRepository.deleteAllByUserNo(userNo);
        deleteCommunityData(userNo);

        for (RoomMember membership : memberships) {
            Long roomMemberId = membership.getRoomMemberId();
            receiptSplitRepository.deleteAllByReceipt_Uploader_RoomMemberId(roomMemberId);
            receiptRepository.deleteAllByUploader_RoomMemberId(roomMemberId);
            receiptSplitRepository.deleteAllByRoomMember_RoomMemberId(roomMemberId);
        }

        roomMemberRepository.deleteAll(memberships);

        for (Room room : ownedRooms) {
            transferOwnerOrDeleteRoom(room, userNo);
        }

        userRepository.delete(user);
    }

    private void deleteCommunityData(Long userNo) {
        roomFreeChatRepository.deleteAllByUser_UserNo(userNo);
        roomFreeCommentRepository.deleteAllByPostAuthorUserNo(userNo);
        roomFreeCommentRepository.deleteAllByAuthor_UserNo(userNo);
        roomFreePostRepository.deleteAllByAuthor_UserNo(userNo);
    }

    private void transferOwnerOrDeleteRoom(Room room, Long withdrawnUserNo) {
        roomMemberRepository
                .findFirstByRoom_RoomNoAndUser_UserNoNotAndStatusOrderByJoinedAtAsc(
                        room.getRoomNo(),
                        withdrawnUserNo,
                        RoomMember.Status.ACTIVE
                )
                .ifPresentOrElse(
                        nextOwner -> room.changeOwner(nextOwner.getUser().getUserNo()),
                        () -> deleteRoomData(room)
                );
    }

    private void deleteRoomData(Room room) {
        Long roomNo = room.getRoomNo();

        receiptSplitRepository.deleteAllByReceipt_Room_RoomNo(roomNo);
        receiptSplitRepository.deleteAllByRoomMember_Room_RoomNo(roomNo);
        receiptRepository.deleteAllByRoom_RoomNo(roomNo);
        budgetRepository.deleteAllByRoomNo(roomNo);
        roomPurposeTagRepository.deleteAllByRoom_RoomNo(roomNo);
        roomBudgetResultRepository.deleteByRoom_RoomNo(roomNo);
        roomBeggarScoreRepository.deleteByRoom_RoomNo(roomNo);
        roomMemberRepository.deleteAllByRoom_RoomNo(roomNo);
        roomRepository.delete(room);
        }
    }
