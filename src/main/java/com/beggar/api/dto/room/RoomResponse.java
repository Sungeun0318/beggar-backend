package com.beggar.api.dto.room;

import com.beggar.api.entity.Room;

import java.time.LocalDateTime;
import java.util.List;

public record RoomResponse(
        Long roomNo,
        String roomName,
        String roomCode,
        Long ownerUserNo,
        Integer totalBudget,
        LocalDateTime roomCreated,
        List<String> tags
) {
    public static RoomResponse of(Room room, List<String> tags) {
        return new RoomResponse(
                room.getRoomNo(),
                room.getRoomName(),
                room.getRoomCode(),
                room.getOwner().getUserNo(),
                room.getTotalBudget(),
                room.getRoomCreated(),
                tags
        );
    }
}
