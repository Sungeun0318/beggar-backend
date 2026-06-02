package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.community.RoomFreePostDetailResponse;
import com.beggar.api.dto.community.RoomFreePostResponse;
import com.beggar.api.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    // 1. 게시글 목록 조회 및 검색
    @GetMapping("/posts")
    public ApiResponse<List<RoomFreePostResponse>> getPosts(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(communityService.getPosts(keyword));
    }

    // 2. 게시글 상세 조회 (댓글 포함)
    @GetMapping("/posts/{postId}")
    public ApiResponse<RoomFreePostDetailResponse> getPostDetail(@PathVariable Long postId) {
        return ApiResponse.success(communityService.getPostDetail(postId));
    }

}
