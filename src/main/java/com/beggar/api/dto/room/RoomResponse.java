package com.beggar.api.dto.room;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private Long roomNo;
    private String roomName;
    private String roomCode;
    private Long ownerUserNo;
    private Integer totalBudget;
    private Boolean isFriends;
    private Integer maxMemberCount;
    private LocalDateTime roomCreated;
    private List<String> tags;
}