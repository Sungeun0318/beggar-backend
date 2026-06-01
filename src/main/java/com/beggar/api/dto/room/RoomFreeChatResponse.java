package com.beggar.api.dto.room;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomFreeChatResponse {
    private Long chatId;
    private String senderName;
    private String message;
    private LocalDateTime createdAt;
}
