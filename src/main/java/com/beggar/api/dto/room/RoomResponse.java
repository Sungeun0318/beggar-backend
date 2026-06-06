package com.beggar.api.dto.room;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class RoomResponse {
    private Long roomNo;
    private String roomName;
    private String roomCode;
    private Long ownerUserNo;
    private Integer totalBudget;
    private Boolean isFriends;
    private String location;
    private long memberCount;
    private int maxMemberCount;
    private LocalDateTime roomCreated;

    private List<String> tags;
}
