package com.beggar.api.dto.room;

import com.beggar.api.entity.RoomMember;

public record RoomMemberResponse(
        String name,
        String status,
        boolean mine
) {
    public static RoomMemberResponse from(RoomMember member, Long loginUserNo, boolean budgetSubmitted) {
        String displayStatus = budgetSubmitted ? "제출 완료" : "입장 완료";
        if (!budgetSubmitted && member.getRoom().getOwnerUserNo().equals(member.getUser().getUserNo())) {
            displayStatus = "방장";
        }

        return new RoomMemberResponse(
                member.getUser().getUserName(),
                displayStatus,
                member.getUser().getUserNo().equals(loginUserNo)
        );
    }
}
