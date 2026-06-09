package com.beggar.api.service;

import com.beggar.api.dto.room.RoomEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomEventService {
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 모든 이벤트를 /topic/rooms/{roomNo} 단일 채널로 발행합니다.
     */
    public void publishEvent(RoomEventDto event) {
        String destination = "/topic/rooms/" + event.getRoomNo();
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * 멤버 입장/퇴장 등 인원 구성 변경 시 알림
     */
    public void publishMembersUpdated(Long roomNo, Object members) {
        RoomEventDto event = RoomEventDto.builder()
                .type(RoomEventDto.EventType.MEMBERS_UPDATED)
                .roomNo(roomNo)
                .data(members)
                .build();
        publishEvent(event);
    }

    /**
     * 방의 상태 변경(예: 예산 입력 시작, 예산 확정 등) 알림
     */
    public void publishStateChanged(Long roomNo, RoomEventDto.EventType type, String nextPath) {
        RoomEventDto event = RoomEventDto.builder()
                .type(type)
                .roomNo(roomNo)
                .data(nextPath)
                .build();
        publishEvent(event);
    }

    /**
     * 예산 제출 현황 알림
     */
    public void publishBudgetSubmitted(Long roomNo, Object budgetStatus) {
        RoomEventDto event = RoomEventDto.builder()
                .type(RoomEventDto.EventType.BUDGET_SUBMITTED)
                .roomNo(roomNo)
                .data(budgetStatus)
                .build();
        publishEvent(event);
    }
}
