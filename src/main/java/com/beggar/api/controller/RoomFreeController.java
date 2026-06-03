package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.community.*;
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

    // 1. 게시글 목록 조회 및 검색
    @GetMapping("/posts")
    public ApiResponse<List<RoomFreePostResponse>> getPosts(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(roomFreeService.getPosts(keyword));
    }

    // 2. 게시글 상세 조회 (댓글 포함)
    @GetMapping("/posts/{postId}")
    public ApiResponse<RoomFreePostDetailResponse> getPostDetail(@PathVariable Long postId) {
        return ApiResponse.success(roomFreeService.getPostDetail(postId));
    }

    // 3. 게시글 작성
    @PostMapping("/posts")
    public ApiResponse<Void> createPost(
            @LoginUser Long userNo,
            @RequestBody RoomFreePostRequest request) {
        roomFreeService.createPost(userNo, request);
        return ApiResponse.success();
    }

    // 4. 댓글 작성
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<Void> createComment(
            @LoginUser Long userNo,
            @PathVariable Long postId,
            @RequestBody RoomFreeCommentRequest request) {
        roomFreeService.createComment(userNo, postId, request.getContent());
        return ApiResponse.success();
    }

    // 5. 전체 채팅 내역 조회
    @GetMapping("/chats")
    public ApiResponse<List<RoomFreeChatResponse>> getChatHistory() {
        return ApiResponse.success(roomFreeService.getChatHistory());
    }

    // 6. 채팅 메시지 전송
    @PostMapping("/chats")
    public ApiResponse<Void> sendChat(
            @LoginUser Long userNo,
            @RequestBody RoomFreeChatRequest request) {
        roomFreeService.sendChat(userNo, request.getContent());
        return ApiResponse.success();
    }
}
