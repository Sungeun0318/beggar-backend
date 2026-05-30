package com.beggar.api.dto.ranking;

import com.beggar.api.entity.RoomBeggarScore;

public record RankingEntryResponse(
        int rank,
        Long roomNo,
        String roomName,
        Integer score,
        String title
) {
    public static RankingEntryResponse of(int rank, RoomBeggarScore s) {
        return new RankingEntryResponse(
                rank,
                s.getRoom().getRoomNo(),
                s.getRoom().getRoomName(),
                s.getScore(),
                s.getTitle()
        );
    }
}
