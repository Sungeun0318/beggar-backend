package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.ranking.BeggarScoreResponse;
import com.beggar.api.dto.room.RoomCreateRequest;
import com.beggar.api.dto.room.RoomJoinRequest;
import com.beggar.api.dto.room.RoomMemberResponse;
import com.beggar.api.dto.room.RoomResponse;
import com.beggar.api.dto.room.RouletteResultResponse;
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

    public RoomController(RoomService roomService, BeggarScoreService beggarScoreService, RouletteService rouletteService) {
        this.roomService = roomService;
        this.beggarScoreService = beggarScoreService;
        this.rouletteService = rouletteService;
    }

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

    @DeleteMapping("/{roomNo}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @PathVariable Long roomNo,
            @LoginUser Long loginUserNo) {
        roomService.hideRoom(roomNo, loginUserNo);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{roomNo}/beggar-score")
    public ResponseEntity<ApiResponse<BeggarScoreResponse>> getBeggarScore(@PathVariable Long roomNo) {
        return ResponseEntity.ok(ApiResponse.success(beggarScoreService.getRoomScore(roomNo)));
    }

    @GetMapping("/{roomNo}/roulette")
    public ResponseEntity<ApiResponse<RouletteResultResponse>> getRouletteResult(
            @PathVariable Long roomNo,
            @LoginUser Long loginUserNo
    ) {
        return ResponseEntity.ok(ApiResponse.success(rouletteService.getRouletteResult(roomNo, loginUserNo)));
    }

    @PostMapping("/{roomNo}/roulette")
    public ResponseEntity<ApiResponse<RouletteResultResponse>> startRoulette(
            @PathVariable Long roomNo,
            @LoginUser Long loginUserNo
    ) {
        RouletteResultResponse result = rouletteService.runRoulette(roomNo, loginUserNo);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
