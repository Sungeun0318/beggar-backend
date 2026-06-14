package com.beggar.api.controller.admin;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.admin.PostDetail;
import com.beggar.api.dto.admin.PostListItem;
import com.beggar.api.service.admin.AdminPostService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AdminPostController {

    private final AdminPostService adminPostService;

    public AdminPostController(AdminPostService adminPostService) {
        this.adminPostService = adminPostService;
    }

    @GetMapping("/admin/community/posts")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String tag,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<PostListItem> posts = adminPostService.getPosts(keyword, tag, page);

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "게시글 관리");
        data.put("pageDescription", "커뮤니티 게시글을 검색하고 상세 내용을 확인하세요.");
        data.put("activeMenu", "posts");
        data.put("posts", posts);
        data.put("keyword", keyword);
        data.put("tag", tag);

        return ApiResponse.success(data);
    }

    @GetMapping("/admin/community/posts/{postId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long postId) {
        PostDetail post = adminPostService.getPostDetail(postId);

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "게시글 상세");
        data.put("pageDescription", "게시글 본문과 댓글을 확인하고 관리하세요.");
        data.put("activeMenu", "posts");
        data.put("post", post);

        return ApiResponse.success(data);
    }

    @PostMapping("/admin/community/posts/{postId}/delete")
    public ApiResponse<String> delete(@PathVariable Long postId) {
        adminPostService.deletePost(postId);
        return ApiResponse.success("게시글을 삭제했습니다.");
    }
}
