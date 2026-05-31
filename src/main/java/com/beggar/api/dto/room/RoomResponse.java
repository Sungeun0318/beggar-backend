package com.beggar.api.dto.room;

import java.time.LocalDateTime;
import java.util.List;

public class RoomResponse {
    private Long roomNo;
    private String roomName;
    private String roomCode;
    private Long ownerUserNo;
    private Integer totalBudget;
    private boolean isFriends;
    private LocalDateTime roomCreated;
    private List<String> tags;


}
