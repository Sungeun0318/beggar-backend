package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.ranking.BeggarScoreResponse;
import com.beggar.api.dto.room.RoomCreateRequest;
import com.beggar.api.dto.room.RoomJoinRequest;
import com.beggar.api.dto.room.RoomMemberResponse;
import com.beggar.api.dto.room.RoomResponse;
import com.beggar.api.security.LoginUser;
import com.beggar.api.service.BeggarScoreService;
import com.beggar.api.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;
    private final BeggarScoreService beggarScoreService;

    // 생성자 주입
    public RoomController(RoomService roomService, BeggarScoreService beggarScoreService) {
        this.roomService = roomService;
        this.beggarScoreService = beggarScoreService;
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

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getMyRooms(@LoginUser Long loginUserNo) {
        return ResponseEntity.ok(ApiResponse.success(roomService.findMyRooms(loginUserNo)));
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

    /* 🛠️ 방 설정 변경 (방장 전용) */
    @PatchMapping("/{roomNo}/settings")
    public ResponseEntity<ApiResponse<Void>> updateSettings(
            @PathVariable Long roomNo,
            @LoginUser Long loginUserNo,
            @RequestBody RoomCreateRequest request) {
        roomService.updateSettings(roomNo, loginUserNo, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/{roomNo}/close")
    public ResponseEntity<ApiResponse<Void>> closeRoom(
            @PathVariable Long roomNo,
            @LoginUser Long loginUserNo) {
        roomService.closeRoom(roomNo, loginUserNo);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /* 🗑️ 방 목록에서 삭제 (개별 숨김 처리) */
    @DeleteMapping("/{roomNo}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @PathVariable Long roomNo,
            @LoginUser Long loginUserNo) {
        roomService.hideRoom(roomNo, loginUserNo);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // 방별 거지평가 등급 및 스코어 조회
    @GetMapping("/{roomNo}/beggar-score")
    public ResponseEntity<ApiResponse<BeggarScoreResponse>> getBeggarScore(@PathVariable Long roomNo) {
        return ResponseEntity.ok(ApiResponse.success(beggarScoreService.getRoomScore(roomNo)));
    }

    // TODO: GET   /rooms/{roomNo}            — 방 상세 정보 및 태그 조회
    // TODO: GET   /rooms/{roomNo}/members    — 입장 현황 (보안상 예산 금액 미노출)
}
