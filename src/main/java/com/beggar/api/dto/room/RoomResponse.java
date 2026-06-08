// 📄 com/beggar/api/dto/room/RoomResponse.java 파일 최종 수정

package com.beggar.api.dto.room;

import com.beggar.api.entity.RoomStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class RoomResponse {

    @JsonProperty("roomNo") // 프론트가 인식할 이름을 명확하게 고정!
    private Long roomNo;

    @JsonProperty("roomName")
    private String roomName;

    @JsonProperty("roomCode")
    private String roomCode;

    private Long ownerUserNo;
    private Integer totalBudget;
    private Boolean isFriends;
    private String location;
    private RoomStatus status;
    private long memberCount;
    private int maxMemberCount;
    private LocalDateTime roomCreated;

    private List<String> tags;
}