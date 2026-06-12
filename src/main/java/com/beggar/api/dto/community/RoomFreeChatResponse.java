package com.beggar.api.dto.community;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomFreeChatResponse {
    private Long id;
    private String sender;
    private String senderProfileImageUrl;
    private String message;
    private LocalDateTime createdAt;
}
