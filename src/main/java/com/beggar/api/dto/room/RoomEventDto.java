package com.beggar.api.dto.room;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomEventDto {
    public enum EventType {
        MEMBERS_UPDATED,
        BUDGET_INPUT_STARTED,
        BUDGET_SUBMITTED,
        BUDGET_CONFIRMED,
        ROOM_STARTED,
        ROOM_ENDED
    }

    private EventType type;
    private Long roomNo;
    private Object data; // 이벤트별 추가 데이터 (members 목록, nextPath 등)
}
