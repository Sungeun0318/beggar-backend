package com.beggar.api.dto.room;

import com.beggar.api.entity.Room;

import java.time.LocalDateTime;
import java.util.List;

public record RoomResponse(
        Long roomNo,
        String roomName,
        String roomCode,
        Long ownerUserNo,
        Integer maxMemberCount,
        Integer totalBudget,
        Boolean isFriends,
        LocalDateTime roomCreated,
        List<String> tags
) {
    public static RoomResponse of(Room room, List<String> tags) {
        return new RoomResponse(
                room.getRoomNo(),
                room.getRoomName(),
                room.getRoomCode(),
                room.getOwner().getUserNo(),
                room.getMaxMemberCount(),
                room.getTotalBudget(),
                room.getIsFriends(),
                room.getRoomCreated(),
                tags
        );
    }
}
