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

    // 생성자 주입
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /* 거지방 신규 생성 */
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            // @RequestHeader("Authorization") String token,
            @RequestBody RoomCreateRequest request) {
        // 토큰 연동 전 임시 가짜 변수 유저 번호
        Long loginUserNo = 5L;

        RoomResponse roomResponse = roomService.createRoom(request, loginUserNo);

        return ResponseEntity.ok(ApiResponse.success(roomResponse));
    }

    // TODO: GET   /rooms/my                  — 내가 참여 중인 방 목록
    // TODO: GET   /rooms/{roomNo}            — 방 상세 정보 및 태그 조회
    // TODO: PATCH /rooms/{roomNo}/settings   — 방장 전용 지역/태그/최대 인원 변경
    // TODO: POST  /rooms/join                — 초대 코드로 신규 방 입장
    // TODO: GET   /rooms/{roomNo}/members    — 입장 현황 (보안상 예산 금액 미노출)
    // TODO: GET   /rooms/{roomNo}/beggar-score — 방별 거지평가 등급 및 스코어 조회
}
