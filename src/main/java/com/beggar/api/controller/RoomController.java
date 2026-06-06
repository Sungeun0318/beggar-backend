package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.room.RoomCreateRequest;
import com.beggar.api.dto.room.RoomJoinRequest;
import com.beggar.api.dto.room.RoomMemberResponse;
import com.beggar.api.dto.room.RoomResponse;
import com.beggar.api.security.LoginUser;
import com.beggar.api.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @LoginUser Long loginUserNo,
            @RequestBody RoomCreateRequest request) {
        RoomResponse roomResponse = roomService.createRoom(request, loginUserNo);

        return ResponseEntity.ok(ApiResponse.success(roomResponse));
    }

    @GetMapping("/{roomNo}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(@PathVariable Long roomNo) {
        return ResponseEntity.ok(ApiResponse.success(roomService.findById(roomNo)));
    }

    @GetMapping("/{roomNo}/members")
    public ResponseEntity<ApiResponse<List<RoomMemberResponse>>> getMembers(
            @PathVariable Long roomNo,
            @LoginUser Long loginUserNo) {
        return ResponseEntity.ok(ApiResponse.success(roomService.findMembers(roomNo, loginUserNo)));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<RoomResponse>> joinRoom(
            @LoginUser Long loginUserNo,
            @RequestBody RoomJoinRequest request) {
        return ResponseEntity.ok(ApiResponse.success(roomService.joinByCode(loginUserNo, request.code())));
    }

    @PostMapping("/{roomNo}/budget/start")
    public ResponseEntity<ApiResponse<Void>> startBudget(
            @PathVariable Long roomNo,
            @LoginUser Long loginUserNo) {
        roomService.startBudget(roomNo, loginUserNo);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // TODO: GET   /rooms/my                  — 내가 참여 중인 방 목록
    // TODO: GET   /rooms/{roomNo}            — 방 상세 정보 및 태그 조회
    // TODO: PATCH /rooms/{roomNo}/settings   — 방장 전용 지역/태그/최대 인원 변경
    // TODO: GET   /rooms/{roomNo}/members    — 입장 현황 (보안상 예산 금액 미노출)
    // TODO: GET   /rooms/{roomNo}/beggar-score — 방별 거지평가 등급 및 스코어 조회
}
