package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.community.*;
import com.beggar.api.entity.RoomFreeChat;
import com.beggar.api.entity.RoomFreeComment;
import com.beggar.api.entity.RoomFreePost;
import com.beggar.api.entity.User;
import com.beggar.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomFreeService {
    private final RoomFreePostRepository postRepository;
    private final RoomFreeCommentRepository commentRepository;
    private final RoomFreeChatRepository chatRepository;
    private final UserRepository userRepository;

    // 1. 게시글 목록 조회 및 검색
    public List<RoomFreePostResponse> getPosts(String keyword) {
        return postRepository.findAllWithAuthorByKeyword(keyword).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // 인기글 조회 (댓글 많은 순)
    public List<RoomFreePostResponse> getPopularPosts() {
        return postRepository.findPopularPosts().stream()
                .limit(10) // 상위 10개
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private RoomFreePostResponse convertToResponse(RoomFreePost post) {
        String contentSnippet = post.getContent();
        if (contentSnippet != null && contentSnippet.length() > 50) {
            contentSnippet = contentSnippet.substring(0, 50);
        }

        return RoomFreePostResponse.builder()
                .id(post.getPostId())
                .title(post.getTitle())
                .author(post.getAuthor().getUserName())
                .content(contentSnippet)
                .tag(post.getTag())
                .createdAt(post.getCreatedAt())
                .commentCount(post.getComments().size())
                .build();
    }

    // 2. 게시글 상세 조회
    public RoomFreePostDetailResponse getPostDetail(Long postId) {
        RoomFreePost post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        List<RoomFreeCommentResponse> comments = post.getComments().stream()
                .map(comment -> RoomFreeCommentResponse.builder()
                        .id(comment.getCommentId())
                        .author(comment.getAuthor().getUserName())
                        .content(comment.getContent())
                        .createdAt(comment.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return RoomFreePostDetailResponse.builder()
                .id(post.getPostId())
                .title(post.getTitle())
                .author(post.getAuthor().getUserName())
                .content(post.getContent())
                .tag(post.getTag())
                .createdAt(post.getCreatedAt())
                .comments(comments)
                .build();
    }

    // 3. 게시글 작성
    @Transactional
    public RoomFreePostResponse createPost(Long userNo, RoomFreePostRequest request) {
        if (userNo == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RoomFreePost post = RoomFreePost.builder()
                .author(user)
                .title(request.getTitle())
                .content(request.getContent())
                .tag(request.getTag())
                .build();

        RoomFreePost savedPost = postRepository.save(post);
        return convertToResponse(savedPost);
    }

    // 4. 댓글 작성
    @Transactional
    public RoomFreeCommentResponse createComment(Long userNo, Long postId, String content) {
        if (userNo == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        RoomFreePost post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        RoomFreeComment comment = RoomFreeComment.builder()
                .author(user)
                .post(post)
                .content(content)
                .build();

        RoomFreeComment savedComment = commentRepository.save(comment);

        return RoomFreeCommentResponse.builder()
                .id(savedComment.getCommentId())
                .author(savedComment.getAuthor().getUserName())
                .content(savedComment.getContent())
                .createdAt(savedComment.getCreatedAt())
                .build();
    }

    // 5. 채팅 내역 조회
    public List<RoomFreeChatResponse> getChatHistory() {
        List<RoomFreeChat> chats = chatRepository.findTop100ByOrderByCreatedAtDesc();
        Collections.reverse(chats); // 최신순으로 가져와서 과거->현재 순으로 반환

        return chats.stream()
                .map(chat -> RoomFreeChatResponse.builder()
                        .id(chat.getChatId())
                        .sender(chat.getUser().getUserName())
                        .message(chat.getMessage())
                        .createdAt(chat.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // 6. 채팅 전송
    @Transactional
    public RoomFreeChatResponse sendChat(Long userNo, String content) {
        if (userNo == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RoomFreeChat chat = RoomFreeChat.builder()
                .user(user)
                .message(content)
                .build();

        RoomFreeChat savedChat = chatRepository.save(chat);

        return RoomFreeChatResponse.builder()
                .id(savedChat.getChatId())
                .sender(savedChat.getUser().getUserName())
                .message(savedChat.getMessage())
                .createdAt(savedChat.getCreatedAt())
                .build();
    }
}
