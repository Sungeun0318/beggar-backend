package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.user.UserRequest;
import com.beggar.api.dto.user.UserResponse;
import com.beggar.api.entity.User;
import com.beggar.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final BeggarScoreService beggarScoreService;

    @Transactional
    public void userSignup(UserRequest userRequest) {
        if (!StringUtils.hasText(userRequest.getUserName())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "닉네임은 필수입니다.");
        }

        if (userRepository.existsByUserName(userRequest.getUserName())) {
            throw new CustomException(ErrorCode.DUPLICATE_USER_NAME, "이미 존재하는 닉네임입니다.");
        }

        if (userRepository.existsByEmail(userRequest.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL, "이미 사용 중인 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(userRequest.getPassword());
        User user = User.signup(userRequest, encodedPassword);
        userRepository.save(user);
    }

    @Transactional
    public UserResponse getProfile(Long userNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        
        String profileUrl = user.getProfileImageUrl();
        if (StringUtils.hasText(profileUrl) && !profileUrl.startsWith("http")) {
            profileUrl = s3Service.generateProfilePresignedGetUrl(profileUrl);
        }

        var scoreResult = beggarScoreService.getUserScore(userNo);

        return UserResponse.from(user, scoreResult.score(), scoreResult.title());
    }

    @Transactional
    public void updateProfile(Long userNo, UserRequest request) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (StringUtils.hasText(request.getUserName()) && 
            !request.getUserName().equals(user.getUserName()) &&
            userRepository.existsByUserName(request.getUserName())) {
            throw new CustomException(ErrorCode.DUPLICATE_USER_NAME);
        }

        user.updateProfile(request.getUserName(), request.getProfileImageUrl());
    }

    public String getProfileUploadUrl(String fileName) {
        return s3Service.generateProfilePresignedUrl(fileName);
    }
}
