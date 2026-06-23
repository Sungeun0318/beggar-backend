package com.beggar.api.dto.room;

import com.beggar.api.entity.RoomMember;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public record RouletteResultResponse(
    Long roomId,
    Long winnerUserNo,
    String winnerNickname,
    Long remainingBudget,
    List<RoomMemberResponse> allMembers
){}
