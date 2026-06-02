package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.community.RoomFreeChatResponse;
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

    // 1. 댓글 작성
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

    // 2. 채팅 내역 조회
    public List<RoomFreeChatResponse> getChatHistory() {
        List<RoomFreeChat> chats = chatRepository.findTop100ByOrderByCreatedAtDesc();
        Collections.reverse(chats); // 최신순으로 가져와서 과거->현재 순으로 반환

        return chats.stream()
                .map(chat -> RoomFreeChatResponse.builder()
                        .chatId(chat.getChatId())
                        .senderName(chat.getUser().getUserName())
                        .message(chat.getMessage())
                        .createdAt(chat.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // 3. 채팅 전송
    @Transactional
    public void sendChat(Long userNo, String message) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        RoomFreeChat chat = RoomFreeChat.builder()
                .user(user)
                .message(message)
                .build();

        chatRepository.save(chat);
    }
}
