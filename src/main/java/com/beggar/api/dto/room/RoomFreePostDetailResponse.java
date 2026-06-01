package com.beggar.api.dto.room;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RoomFreePostDetailResponse {
    private Long postId;
    private String title;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;
    private List<RoomFreeCommentResponse> comments;
}
