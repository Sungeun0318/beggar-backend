package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.receipt.MyReceiptHistoryResponse;
import com.beggar.api.dto.user.UserRequest;
import com.beggar.api.dto.user.UserResponse;
import com.beggar.api.security.LoginUser;
import com.beggar.api.service.AuthService;
import com.beggar.api.service.ReceiptService;
import com.beggar.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final ReceiptService receiptService;

    @PostMapping("/signup")
    public ApiResponse<Void> signup(@Valid @RequestBody UserRequest userRequest) {
        userService.userSignup(userRequest);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile(@LoginUser Long userNo) {
        return ApiResponse.success(userService.getProfile(userNo));
    }

    @PatchMapping("/me") // 이미지,닉네임 수정
    public ApiResponse<Void> updateProfile(@LoginUser Long userNo, @RequestBody UserRequest userRequest) {
        userService.updateProfile(userNo, userRequest);
        return ApiResponse.success();
    }

    @GetMapping("/me/receipts")
    public ApiResponse<MyReceiptHistoryResponse> getMyReceipts(@LoginUser Long userNo) {
        return ApiResponse.success(receiptService.readMyReceiptHistory(userNo));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@LoginUser Long userNo) {
        authService.withdraw(userNo);
        return ApiResponse.success();
    }

    @GetMapping("/me/presigned-url")
    public ApiResponse<String> getProfilePresignedUrl(@LoginUser Long userNo, @RequestParam String fileName) {
        return ApiResponse.success(userService.getProfileUploadUrl(fileName));
    }
}
