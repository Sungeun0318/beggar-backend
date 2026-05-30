package com.beggar.api.service;

import com.beggar.api.config.PasswordEncoderConfig;
import com.beggar.api.dto.user.UserRequest;
import com.beggar.api.entity.User;
import com.beggar.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    // private final PasswordEncoderConfig passwordEncoderConfig;

    @Transactional
    public void userSignup(UserRequest userRequest){
        // 1. 유저명 중복 검사
        if(userRepository.existsByUserName(userRequest.getUserName())){
            throw new IllegalArgumentException("이미 존재하는 유저명입니다.");
        }
        // 2. 이메일 중복 검사
        if(userRepository.existsByEmail(userRequest.getEmail())){
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        // 3. 비밀번호 암호화 (설정 시 주석 해제)
        // String encodedPassword = passwordEncoder.encoder(userRequest.getPassword());
        String encodedPassword = userRequest.getPassword(); // 임시 처리 (암호화 적용 권장)
        
        // 4. 유저 저장
        User user = User.signup(userRequest, encodedPassword);
        System.out.println("userRequest = " + userRequest);
        userRepository.save(user);
    }
    // TODO: getMyProfile(userNo) — 마이페이지 프로필 조회
}
