package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.community.RoomFreeCommentResponse;
import com.beggar.api.dto.community.RoomFreePostDetailResponse;
import com.beggar.api.dto.community.RoomFreePostResponse;
import com.beggar.api.entity.RoomFreePost;
import com.beggar.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    private final RoomFreePostRepository postRepository;
    private final UserRepository userRepository;

    // 1. 게시글 목록 조회 및 검색
    public List<RoomFreePostResponse> getPosts(String keyword) {
        return postRepository.findAllWithAuthorByKeyword(keyword).stream()
                .map(post -> RoomFreePostResponse.builder()
                        .postId(post.getPostId())
                        .title(post.getTitle())
                        .authorName(post.getAuthor().getUserName())
                        .contentPreview(post.getContent().substring(0, Math.min(post.getContent().length(), 50)))
                        .createdAt(post.getCreatedAt())
                        .commentCount(post.getComments().size())
                        .build())
                .collect(Collectors.toList());
    }

    // 2. 게시글 상세 조회
    public RoomFreePostDetailResponse getPostDetail(Long postId) {
        RoomFreePost post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        List<RoomFreeCommentResponse> comments = post.getComments().stream()
                .map(comment -> RoomFreeCommentResponse.builder()
                        .commentId(comment.getCommentId())
                        .authorName(comment.getAuthor().getUserName())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return RoomFreePostDetailResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .authorName(post.getAuthor().getUserName())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .comments(comments)
                .build();
    }


}
