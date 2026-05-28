package com.beggar.api.dto.ranking;

import com.beggar.api.entity.UserBeggarScore;

public record RankingEntryResponse(
        int rank,
        Long userNo,
        String userName,
        Integer score,
        String title
) {
    public static RankingEntryResponse of(int rank, UserBeggarScore s) {
        return new RankingEntryResponse(
                rank,
                s.getUser().getUserNo(),
                s.getUser().getUserName(),
                s.getScore(),
                s.getTitle()
        );
    }
}
