package com.beggar.api.dto.community;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomFreePostResponse {
    private Long postId;
    private String title;
    private String authorName;
    private String contentPreview;
    private LocalDateTime createdAt;
    private int commentCount;
}
