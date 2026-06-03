package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.room.RoomCreateRequest;
import com.beggar.api.dto.room.RoomResponse;
import com.beggar.api.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService){
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
           // @RequestHeader("Authorization") String token,
            @RequestBody RoomCreateRequest request){
        Long loginUserNo = 5L; // 로그인 연동하지 않은 상태에서 방 생성 로직만 테스트하고 싶을 때.. 임시로 가짜변수 5번 유저라고 적어둔 거.

        RoomResponse roomResponse = roomService.createRoom(request,loginUserNo);

        return ResponseEntity.ok(ApiResponse.success(roomResponse));
    }

    // TODO: GET  /rooms/my              — 내가 참여 중인 방 목록
    // TODO: POST /rooms                 — 방 생성 (maxMemberCount + tags 동시 등록)
    // TODO: GET  /rooms/{roomNo}        — 방 상세
    // TODO: PATCH /rooms/{roomNo}/settings — 방장 전용 지역/태그/최대 인원 변경
    // TODO: POST /rooms/join            — 초대 코드로 입장
    // TODO: GET  /rooms/{roomNo}/members — 입장 현황 (금액 미노출)
    // TODO: GET  /rooms/{roomNo}/beggar-score — 방별 거지평가 조회
}
