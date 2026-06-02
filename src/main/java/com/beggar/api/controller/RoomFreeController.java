package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.community.RoomFreeChatResponse;
import com.beggar.api.dto.community.RoomFreeCommentRequest;
import com.beggar.api.security.LoginUser;
import com.beggar.api.service.RoomFreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/freerooms")
public class RoomFreeController {
    private final RoomFreeService roomFreeService;

    // 1. 댓글 작성
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<Void> createComment(
            @LoginUser Long userNo,
            @PathVariable Long postId,
            @RequestBody RoomFreeCommentRequest request) {
        roomFreeService.createComment(userNo, postId, request.getContent());
        return ApiResponse.success();
    }

    // 2. 전체 채팅 내역 조회
    @GetMapping("/chats")
    public ApiResponse<List<RoomFreeChatResponse>> getChatHistory() {
        return ApiResponse.success(roomFreeService.getChatHistory());
    }

    // 3. 채팅 메시지 전송
    @PostMapping("/chats")
    public ApiResponse<Void> sendChat(@LoginUser Long userNo, @RequestBody String message) {
        roomFreeService.sendChat(userNo, message);
        return ApiResponse.success();
    }
}
