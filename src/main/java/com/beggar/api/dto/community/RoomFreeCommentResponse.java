package com.beggar.api.dto.community;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomFreeCommentResponse {
    private Long commentId;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;
}
