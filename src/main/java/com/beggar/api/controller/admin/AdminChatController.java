package com.beggar.api.controller.admin;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.admin.ChatListItem;
import com.beggar.api.service.admin.AdminChatService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AdminChatController {

    private final AdminChatService adminChatService;

    public AdminChatController(AdminChatService adminChatService) {
        this.adminChatService = adminChatService;
    }

    @GetMapping("/admin/chats")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long userNo,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<ChatListItem> chats = adminChatService.getChats(keyword, userNo, page);

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "채팅 관리");
        data.put("pageDescription", "커뮤니티 전체 채팅 메시지를 검색하고 삭제할 수 있어.");
        data.put("activeMenu", "chats");
        data.put("chats", chats);
        data.put("keyword", keyword);
        data.put("userNo", userNo);

        return ApiResponse.success(data);
    }

    @PostMapping("/admin/chats/delete")
    public ApiResponse<String> delete(@RequestParam Long chatId) {
        adminChatService.deleteChat(chatId);
        return ApiResponse.success("채팅 메시지를 삭제했어.");
    }
}
