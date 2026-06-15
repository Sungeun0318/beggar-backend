package com.beggar.api.controller.admin;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.admin.CommentListItem;
import com.beggar.api.service.admin.AdminCommentService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    public AdminCommentController(AdminCommentService adminCommentService) {
        this.adminCommentService = adminCommentService;
    }

    @GetMapping("/admin/community/comments")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long postId,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<CommentListItem> comments = adminCommentService.getComments(keyword, postId, page);

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "댓글 관리");
        data.put("pageDescription", "커뮤니티 댓글을 검색하고 삭제할 수 있습니다.");
        data.put("activeMenu", "comments");
        data.put("comments", comments);
        data.put("keyword", keyword);
        data.put("postId", postId);

        return ApiResponse.success(data);
    }

    @PostMapping("/admin/community/comments/delete")
    public ApiResponse<String> delete(@RequestParam Long commentId) {
        adminCommentService.deleteComment(commentId);
        return ApiResponse.success("댓글을 삭제했습니다.");
    }
}
