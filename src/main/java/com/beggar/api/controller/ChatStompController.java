package com.beggar.api.controller;

import com.beggar.api.dto.community.RoomFreeChatRequest;
import com.beggar.api.dto.community.RoomFreeChatResponse;
import com.beggar.api.service.RoomFreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomFreeService roomFreeService;

    // 클라이언트에서 메시지 전송 시 실행 (/pub/chats)
    @MessageMapping("/chats")
    public void message(RoomFreeChatRequest request) {
        // 1. DB에 저장 (Service 호출)
        // 인증 시스템 만들어지면 request에 userNo 포함시켜야 함
        Long tempUserNo = 1L; 
        RoomFreeChatResponse response = roomFreeService.sendChat(tempUserNo, request.getContent());

        // 2. 구독 중인 모든 유저에게 메시지 전달 (/sub/chats)
        messagingTemplate.convertAndSend("/sub/chats", response);
    }
}
