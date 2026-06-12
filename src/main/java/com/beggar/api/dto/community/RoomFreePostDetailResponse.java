package com.beggar.api.dto.community;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RoomFreePostDetailResponse {
    private Long id;
    private String title;
    private String author;
    private String authorProfileImageUrl;
    private String content;
    private String tag;
    private LocalDateTime createdAt;
    private List<RoomFreeCommentResponse> comments;
}
