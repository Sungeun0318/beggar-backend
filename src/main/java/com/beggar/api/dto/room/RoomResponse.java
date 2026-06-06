package com.beggar.api.dto.room;

import com.beggar.api.entity.RoomStatus;
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
    private RoomStatus status;
    private long memberCount;
    private int maxMemberCount;
    private LocalDateTime roomCreated;

    private List<String> tags;
}
