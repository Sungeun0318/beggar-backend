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
     * 멤버 입장/퇴장 등 인원 구성 변경 시 알림 (/topic/rooms/{roomNo}/members)
     */
    public void publishMembersUpdated(Long roomNo, Object members) {
        String destination = "/topic/rooms/" + roomNo + "/members";
        // 프론트엔드에서 배열 형식을 바로 사용할 수 있도록 RoomEventDto로 감싸지 않고 직접 보냅니다.
        messagingTemplate.convertAndSend(destination, members);
    }

    /**
     * 방의 상태 변경 알림 (/topic/rooms/{roomNo}/state)
     */
    public void publishStateChanged(Long roomNo, RoomEventDto.EventType type, String nextPath) {
        String destination = "/topic/rooms/" + roomNo + "/state";
        RoomEventDto event = RoomEventDto.builder()
                .type(type)
                .roomNo(roomNo)
                .data(nextPath)
                .build();
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * 예산 제출 현황 알림 (/topic/rooms/{roomNo}/budget)
     */
    public void publishBudgetSubmitted(Long roomNo, Object budgetStatus) {
        String destination = "/topic/rooms/" + roomNo + "/budget";
        RoomEventDto event = RoomEventDto.builder()
                .type(RoomEventDto.EventType.BUDGET_SUBMITTED)
                .roomNo(roomNo)
                .data(budgetStatus)
                .build();
        messagingTemplate.convertAndSend(destination, event);
    }
}
