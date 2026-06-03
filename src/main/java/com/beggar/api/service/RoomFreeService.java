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
                .map(post -> RoomFreePostResponse.builder()
                        .id(post.getPostId())
                        .title(post.getTitle())
                        .author(post.getAuthor().getUserName())
                        .content(post.getContent().substring(0, Math.min(post.getContent().length(), 50)))
                        .tag(post.getTag())
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
    public void createPost(Long userNo, RoomFreePostRequest request) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RoomFreePost post = RoomFreePost.builder()
                .author(user)
                .title(request.getTitle())
                .content(request.getContent())
                .tag(request.getTag())
                .build();

        postRepository.save(post);
    }

    // 4. 댓글 작성
    @Transactional
    public void createComment(Long userNo, Long postId, String content) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        RoomFreePost post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        RoomFreeComment comment = RoomFreeComment.builder()
                .author(user)
                .post(post)
                .content(content)
                .build();

        commentRepository.save(comment);
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
    public void sendChat(Long userNo, String content) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RoomFreeChat chat = RoomFreeChat.builder()
                .user(user)
                .message(content)
                .build();

        chatRepository.save(chat);
    }
}
