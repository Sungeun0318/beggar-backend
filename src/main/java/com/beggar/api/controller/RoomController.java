package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.ranking.BeggarScoreResponse;
import com.beggar.api.dto.room.*;
import com.beggar.api.security.LoginUser;
import com.beggar.api.service.BeggarScoreService;
import com.beggar.api.service.RoomService;
import com.beggar.api.service.RouletteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")

public class RoomController {

    private final RoomService roomService;
    private final BeggarScoreService beggarScoreService;
    private final RouletteService rouletteService;

    // 생성자 주입
    public RoomController(RoomService roomService, BeggarScoreService beggarScoreService, RouletteService rouletteService) {
        this.roomService = roomService;
        this.beggarScoreService = beggarScoreService;
        this.rouletteService = rouletteService;
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

    /* 🔍 방 검색 API */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> searchRooms(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        return ResponseEntity.ok(ApiResponse.success(roomService.searchRooms(keyword)));
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

    // 거지룰렛 돌리기
    @PostMapping("/{roomId}/roulette")
    public ResponseEntity<RouletteResultResponse> startRoulette(
            @PathVariable("roomId") Long roomId,
            @RequestAttribute("userNo") Long loginUserNo // 혹은 현재 프로젝트의 로그인 세션/토큰 주입 방식(예: @AuthenticationPrincipal)에 맞게 커스텀하세요!
    ) {
        RouletteResultResponse result = rouletteService.runRoulette(roomId, loginUserNo);
        return ResponseEntity.ok(result);
    }

    // TODO: GET   /rooms/{roomNo}            — 방 상세 정보 및 태그 조회
    // TODO: GET   /rooms/{roomNo}/members    — 입장 현황 (보안상 예산 금액 미노출)
}
