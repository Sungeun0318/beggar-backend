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
    private final com.beggar.api.security.JwtTokenProvider jwtTokenProvider;    // 제거

    /* JwtInterceptor가 구현된 후에는 @LoginUser 사용.
    현재는 JwtInterceptor가 미완 상태라 임시로 @RequestHeader를 사용해 직접 추출함 */

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
    public ApiResponse<Void> createPost(
            // @LoginUser Long userNo, // <-- 인터셉터 완성 후 이 주석을 풀고 아래 authHeader 관련 로직 제거
            @RequestHeader(value = "Authorization", required = false) String authHeader,    // 제거
            @RequestBody RoomFreePostRequest request) {
        Long userNo = extractUserNo(authHeader);                                            // 제거
        roomFreeService.createPost(userNo, request);
        return ApiResponse.success();
    }

    // 4. 댓글 작성
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<Void> createComment(
            // @LoginUser Long userNo,
            @RequestHeader(value = "Authorization", required = false) String authHeader,    // 제거
            @PathVariable Long postId,
            @RequestBody RoomFreeCommentRequest request) {
        Long userNo = extractUserNo(authHeader);                                            // 제거
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
            // @LoginUser Long userNo,
            @RequestHeader(value = "Authorization", required = false) String authHeader,    // 제거
            @RequestBody RoomFreeChatRequest request) {
        Long userNo = extractUserNo(authHeader);                                            // 제거
        roomFreeService.sendChat(userNo, request.getContent());
        return ApiResponse.success();
    }

    // 임시 토큰 추출 메서드: JwtInterceptor 완성되면 아래 메서드 제거
    private Long extractUserNo(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenProvider.isValid(token)) {
                return jwtTokenProvider.parseUserNo(token);
            }
        }
        return null;
    }
}
