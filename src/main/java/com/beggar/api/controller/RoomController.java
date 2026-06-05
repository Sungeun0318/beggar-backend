package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.room.RoomCreateRequest;
import com.beggar.api.dto.room.RoomResponse;
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

    /*  1. 거지방 신규 생성 */
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            // @RequestHeader("Authorization") String token,
            @RequestBody RoomCreateRequest request) {
        // 토큰 연동 전 임시 가짜 변수 유저 번호
        Long loginUserNo = 5L;

        RoomResponse roomResponse = roomService.createRoom(request, loginUserNo);
        return ResponseEntity.ok(ApiResponse.success(roomResponse));
    }

    /*  2. 내가 참여 중인 방 목록 조회 */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> findMyRooms() {
        Long loginUserNo = 5L; // 임시 유저 번호
        List<RoomResponse> responses = roomService.findMyRooms(loginUserNo);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /*  3. 방 상세 정보 및 태그 조회
    @GetMapping("/{roomNo}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomDetail(@PathVariable("roomNo") Long roomNo) {
        // 소영님이 500 에러 안전방어용으로 완공한 Service의 findById 호출!
        RoomResponse roomResponse = roomService.findById(roomNo);
        return ResponseEntity.ok(ApiResponse.success(roomResponse));
    }

    /* 4. 방장 전용 지역/태그/최대 인원 변경 */
    @PatchMapping("/{roomNo}/settings")
    public ResponseEntity<ApiResponse<String>> updateSettings(
            @PathVariable("roomNo") Long roomNo,
            @RequestBody RoomCreateRequest request) {
        Long loginUserNo = 5L; // 임시 방장 번호
        roomService.updateSettings(roomNo, loginUserNo, request);
        return ResponseEntity.ok(ApiResponse.success("방 설정이 성공적으로 변경되었습니다."));
    }

    /* 5. 초대 코드로 신규 방 입장 */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<String>> joinByCode(@RequestParam("roomCode") String roomCode) {
        Long loginUserNo = 5L; // 임시 유저 번호
        roomService.joinByCode(loginUserNo, roomCode);
        return ResponseEntity.ok(ApiResponse.success("거지방에 성공적으로 입장했습니다."));
    }

    /* 6. 입장 현황 (보안상 예산 금액 미노출) */
    @GetMapping("/{roomNo}/members")
    public ResponseEntity<ApiResponse<List<Object>>> findMembers(@PathVariable("roomNo") Long roomNo) {
        List<Object> members = roomService.findMembers(roomNo);
        return ResponseEntity.ok(ApiResponse.success(members));
    }
}