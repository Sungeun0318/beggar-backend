package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.community.*;
import com.beggar.api.security.LoginUser;
import com.beggar.api.service.RoomFreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/community")
public class RoomFreeController {
    private final RoomFreeService roomFreeService;
    private final SimpMessagingTemplate messagingTemplate;

    // 1. 게시글 목록 조회 및 검색
    @GetMapping("/posts")
    public ApiResponse<List<RoomFreePostResponse>> getPosts(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(roomFreeService.getPosts(keyword));
    }

    // 인기 게시글 조회 (댓글 많은 순)
    @GetMapping("/posts/popular")
    public ApiResponse<List<RoomFreePostResponse>> getPopularPosts() {
        return ApiResponse.success(roomFreeService.getPopularPosts());
    }

    // 2. 게시글 상세 조회 (댓글 포함)
    @GetMapping("/posts/{postId}")
    public ApiResponse<RoomFreePostDetailResponse> getPostDetail(@PathVariable Long postId) {
        return ApiResponse.success(roomFreeService.getPostDetail(postId));
    }

    // 3. 게시글 작성
    @PostMapping("/posts")
    public ApiResponse<RoomFreePostResponse> createPost(
            @LoginUser Long userNo,
            @RequestBody RoomFreePostRequest request) {
        return ApiResponse.success(roomFreeService.createPost(userNo, request));
    }

    // 게시글 삭제
    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(
            @LoginUser Long userNo,
            @PathVariable Long postId) {
        roomFreeService.deletePost(userNo, postId);
        return ApiResponse.success();
    }

    // 4. 댓글 작성
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<RoomFreeCommentResponse> createComment(
            @LoginUser Long userNo,
            @PathVariable Long postId,
            @RequestBody RoomFreeCommentRequest request) {
        return ApiResponse.success(roomFreeService.createComment(userNo, postId, request.getContent()));
    }

    // 5. 전체 채팅 내역 조회
    @GetMapping("/chats")
    public ApiResponse<List<RoomFreeChatResponse>> getChatHistory() {
        return ApiResponse.success(roomFreeService.getChatHistory());
    }

    // 6. 채팅 메시지 전송
    @PostMapping("/chats")
    public ApiResponse<RoomFreeChatResponse> sendChat(
            @LoginUser Long userNo,
            @RequestBody RoomFreeChatRequest request) {
        RoomFreeChatResponse response = roomFreeService.sendChat(userNo, request.getContent());
        messagingTemplate.convertAndSend("/sub/chats", response);
        return ApiResponse.success(response);
    }

    // 임시 토큰 추출 메서드: JwtInterceptor 완성되면 아래 메서드 제거
//    private Long extractUserNo(String authHeader) {
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            String token = authHeader.substring(7);
//            if (jwtTokenProvider.isValid(token)) {
//                return jwtTokenProvider.parseUserNo(token);
//            }
//        }
//        // 토큰이 없거나 유효하지 않아도 테스트를 위해 기본 유저 ID를 반환 (실제 배포 시에는 반드시 제거하고 null을 반환해야 함)
//        return 1L;
//    }
}
