package com.beggar.api.dto.room;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RoomResponse {
    private Long roomNo;
    private String roomName;
    private String roomCode;
    private Long ownerUserNo;
    private Integer totalBudget;
    private Boolean isFriends;
    private LocalDateTime roomCreated;
}
