package com.beggar.api.dto.community;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoomFreePostRequest {
    private String title;
    private String content;
    private String tag;
}
