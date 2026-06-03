package com.beggar.api.dto.community;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RoomFreePostResponse {
    private Long id;
    private String title;
    private String author;
    private String content;
    private String tag;
    private LocalDateTime createdAt;
    private int commentCount;
}
