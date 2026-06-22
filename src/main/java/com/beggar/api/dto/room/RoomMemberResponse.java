package com.beggar.api.dto.room;

import com.beggar.api.entity.RoomMember;

public record RoomMemberResponse(
        Long roomMemberId,
        Long userNo,
        String name,
        String status,
        boolean mine,
        boolean budgetSubmitted
) {
    public static RoomMemberResponse from(RoomMember member, Long loginUserNo, boolean budgetSubmitted) {
        String displayStatus = budgetSubmitted ? "제출 완료" : "입장 완료";
        if (member.getRoom().getOwnerUserNo().equals(member.getUser().getUserNo())) {
            displayStatus = "방장";
        }

        return new RoomMemberResponse(
                member.getRoomMemberId(),
                member.getUser().getUserNo(),
                member.getUser().getUserName(),
                displayStatus,
                loginUserNo != null && member.getUser().getUserNo().equals(loginUserNo),
                budgetSubmitted
        );
    }

}
