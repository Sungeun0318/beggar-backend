package com.beggar.api.dto.room;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor // 💡 모든 필드를 파라미터로 받는 생성자를 롬복이 자동으로 만들어줍니다!
public class RoomResponse {
    private Long roomNo;
    private String roomName;
    private String roomCode;
    private Long ownerUserNo;
    private Integer totalBudget;
    private Boolean isFriends;
    private LocalDateTime roomCreated;
}
