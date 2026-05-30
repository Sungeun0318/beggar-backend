package com.beggar.api.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;
public record CreateRoomRequest(
        @NotBlank(message = "방 이름은 필수 항목입니다.")
        @Size(max = 15, message = "방 이름은 15자 이하로 입력해주세요.") String roomName,
        @NotEmpty List<String> tags, // 방 성격 태그 목록
        @NotNull(message = "최대 인원은 필수 항목입니다.")
        @Min(value = 2, message = "최소 인원은 2명입니다.")
        @Max(value = 100, message = "최대 인원은 100명입니다.") Integer maxMemberCount,
        @NotNull(message = "친구 전용 여부는 필수 선택 사항입니다.") Boolean isFriends
) {}
