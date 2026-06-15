package com.beggar.api.controller.admin;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.admin.RoomDetail;
import com.beggar.api.dto.admin.RoomListItem;
import com.beggar.api.service.admin.AdminRoomService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AdminRoomController {

    private final AdminRoomService adminRoomService;

    public AdminRoomController(AdminRoomService adminRoomService) {
        this.adminRoomService = adminRoomService;
    }

    @GetMapping("/admin/rooms")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<RoomListItem> rooms = adminRoomService.getRooms(keyword, status, page);

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "방 관리");
        data.put("pageDescription", "방 정보를 검색하고 상세 운영 데이터를 확인하세요.");
        data.put("activeMenu", "rooms");
        data.put("rooms", rooms);
        data.put("keyword", keyword);
        data.put("status", status);

        return ApiResponse.success(data);
    }

    @GetMapping("/admin/rooms/{roomNo}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long roomNo) {
        RoomDetail room = adminRoomService.getRoomDetail(roomNo);

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "방 상세");
        data.put("pageDescription", "방 기본 정보와 예산/참여/영수증 요약을 확인해.");
        data.put("activeMenu", "rooms");
        data.put("room", room);

        return ApiResponse.success(data);
    }

    @PostMapping("/admin/rooms/{roomNo}/end")
    public ApiResponse<String> endRoom(@PathVariable Long roomNo) {
        adminRoomService.endRoom(roomNo);
        return ApiResponse.success("방을 종료 처리했습니다.");
    }

    @PostMapping("/admin/rooms/{roomNo}/delete")
    public ApiResponse<String> deleteRoom(@PathVariable Long roomNo) {
        adminRoomService.deleteRoom(roomNo);
        return ApiResponse.success("방을 삭제 처리했습니다.");
    }
}
