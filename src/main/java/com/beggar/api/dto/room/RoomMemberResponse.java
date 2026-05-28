package com.beggar.api.dto.room;

import com.beggar.api.entity.RoomMember;

public record RoomMemberResponse(
        Long roomMemberId,
        Long userNo,
        String userName,
        String status,
        boolean budgetSubmitted    // 금액은 절대 노출 X, 제출 여부만
) {
    public static RoomMemberResponse of(RoomMember rm, boolean submitted) {
        return new RoomMemberResponse(
                rm.getRoomMemberId(),
                rm.getUser().getUserNo(),
                rm.getUser().getUserName(),
                rm.getStatus().name(),
                submitted
        );
    }
}
