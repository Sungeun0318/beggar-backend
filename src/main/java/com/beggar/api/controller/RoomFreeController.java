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
    //private final com.beggar.api.security.JwtTokenProvider jwtTokenProvider;    // 제거

    /* JwtInterceptor가 구현된 후 @LoginUser 사용으로 변경
    이전엔 JwtInterceptor가 미완 상태라 임시로 @RequestHeader를 사용해 직접 추출함 */

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
            @LoginUser Long userNo, // <-- 인터셉터 완성 후 이 주석을 풀고 아래 authHeader 관련 로직 제거
            //@RequestHeader(value = "Authorization", required = false) String authHeader,    // 제거
            @RequestBody RoomFreePostRequest request) {
        //Long userNo = extractUserNo(authHeader);                                            // 제거
        return ApiResponse.success(roomFreeService.createPost(userNo, request));
    }

    // 4. 댓글 작성
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<RoomFreeCommentResponse> createComment(
            @LoginUser Long userNo,
            //@RequestHeader(value = "Authorization", required = false) String authHeader,    // 제거
            @PathVariable Long postId,
            @RequestBody RoomFreeCommentRequest request) {
        //Long userNo = extractUserNo(authHeader);                                            // 제거
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
            //@RequestHeader(value = "Authorization", required = false) String authHeader,    // 제거
            @RequestBody RoomFreeChatRequest request) {
        //Long userNo = extractUserNo(authHeader);                                            // 제거
        return ApiResponse.success(roomFreeService.sendChat(userNo, request.getContent()));
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
