package com.beggar.api.controller;

import com.beggar.api.dto.community.RoomFreeChatRequest;
import com.beggar.api.dto.community.RoomFreeChatResponse;
import com.beggar.api.security.JwtTokenProvider;
import com.beggar.api.service.RoomFreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomFreeService roomFreeService;
    private final JwtTokenProvider jwtTokenProvider;

    // 클라이언트에서 메시지 전송 시 실행 (/pub/chats)
    @MessageMapping("/chats")
    public void message(RoomFreeChatRequest request, @Header("Authorization") String authHeader) {
        // 1. 토큰 추출 및 검증
        // 토큰 없거나 유효하지 않으면 무시
        if( authHeader == null || !authHeader.startsWith("Bearer ")){
            return;
        }
        String token = authHeader.substring(7);
        if(!jwtTokenProvider.isValid(token)){
            return;
        }

        // 2. 토큰에서 실제 userNo 추출
        Long userNo = jwtTokenProvider.parseUserNo(token);

        // 3. DB 저장
        RoomFreeChatResponse response = roomFreeService.sendChat(userNo, request.getContent());

        // 4. 구독 중인 모든 유저에게 메시지 전달(/sub/chats)
        messagingTemplate.convertAndSend("/sub/chats", response);
    }
}
