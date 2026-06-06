package com.beggar.api.service;

import com.beggar.api.dto.room.RoomEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomEventService {
    private final SimpMessagingTemplate messagingTemplate;

    public void publishEvent(String topicSuffix, RoomEventDto event) {
        String destination = "/topic/rooms/" + event.getRoomNo() + "/" + topicSuffix;
        messagingTemplate.convertAndSend(destination, event);
    }

    public void publishMembersUpdated(Long roomNo, Object members) {
        RoomEventDto event = RoomEventDto.builder()
                .type(RoomEventDto.EventType.MEMBERS_UPDATED)
                .roomNo(roomNo)
                .data(members)
                .build();
        publishEvent("members", event);
    }

    public void publishStateChanged(Long roomNo, RoomEventDto.EventType type, String nextPath) {
        RoomEventDto event = RoomEventDto.builder()
                .type(type)
                .roomNo(roomNo)
                .data(nextPath)
                .build();
        publishEvent("state", event);
    }

    public void publishBudgetSubmitted(Long roomNo, Object budgetStatus) {
        RoomEventDto event = RoomEventDto.builder()
                .type(RoomEventDto.EventType.BUDGET_SUBMITTED)
                .roomNo(roomNo)
                .data(budgetStatus)
                .build();
        publishEvent("budget", event);
    }
}
