package com.beggar.api.dto.room;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class RoomCreateRequest {
    private String roomName;
    private List<String> tags;
    private Boolean isFriends;
}