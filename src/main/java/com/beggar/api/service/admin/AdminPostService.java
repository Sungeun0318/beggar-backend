package com.beggar.api.service.admin;

import com.beggar.api.dto.admin.PostCommentItem;
import com.beggar.api.dto.admin.PostDetail;
import com.beggar.api.dto.admin.PostListItem;
import com.beggar.api.entity.RoomFreeComment;
import com.beggar.api.entity.RoomFreePost;
import com.beggar.api.entity.User;
import com.beggar.api.repository.RoomFreeCommentRepository;
import com.beggar.api.repository.RoomFreePostRepository;
import com.beggar.api.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AdminPostService {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final RoomFreePostRepository postRepository;
    private final RoomFreeCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final AdminActionLogService actionLogService;

    public AdminPostService(
            RoomFreePostRepository postRepository,
            RoomFreeCommentRepository commentRepository,
            UserRepository userRepository,
            AdminActionLogService actionLogService
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.actionLogService = actionLogService;
    }

    @Transactional(readOnly = true)
    public Page<PostListItem> getPosts(String keyword, String tag, int page) {
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
                safePage,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        String trimmed = keyword == null ? "" : keyword.trim();
        String tagValue = tag == null ? "" : tag.trim();

        Page<RoomFreePost> posts;
        if (!tagValue.isEmpty() && !trimmed.isEmpty()) {
            posts = postRepository.findByTagAndTitleContainingIgnoreCaseOrTagAndContentContainingIgnoreCase(
                    tagValue,
                    trimmed,
                    tagValue,
                    trimmed,
                    pageable
            );
        } else if (!trimmed.isEmpty()) {
            posts = postRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(trimmed, trimmed, pageable);
        } else if (!tagValue.isEmpty()) {
            posts = postRepository.findByTagAndTitleContainingIgnoreCaseOrTagAndContentContainingIgnoreCase(
                    tagValue,
                    "",
                    tagValue,
                    "",
                    pageable
            );
        } else {
            posts = postRepository.findAll(pageable);
        }

        return new PageImpl<>(
                posts.getContent().stream().map(this::toListItem).toList(),
                pageable,
                posts.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PostDetail getPostDetail(Long postId) {
        RoomFreePost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없어."));

        return new PostDetail(
                post.getPostId(),
                post.getTitle(),
                post.getContent(),
                blankToDash(post.getTag()),
                authorLabel(post.getAuthor().getUserNo()),
                formatDateTime(post.getCreatedAt()),
                commentRepository.findByPost_PostIdOrderByCreatedAtAsc(postId)
                        .stream()
                        .map(this::toCommentItem)
                        .toList()
        );
    }

    @Transactional
    public void deletePost(Long postId) {
        RoomFreePost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없어."));
        commentRepository.deleteByPost_PostId(postId);
        postRepository.deleteById(postId);
        actionLogService.record("DELETE", "POST", postId, "게시글을 삭제했어: " + post.getTitle());
    }

    private PostListItem toListItem(RoomFreePost post) {
        return new PostListItem(
                post.getPostId(),
                post.getTitle(),
                blankToDash(post.getTag()),
                authorLabel(post.getAuthor().getUserNo()),
                commentRepository.countByPost_PostId(post.getPostId()),
                formatDateTime(post.getCreatedAt())
        );
    }

    private PostCommentItem toCommentItem(RoomFreeComment comment) {
        return new PostCommentItem(
                comment.getCommentId(),
                authorLabel(comment.getAuthor().getUserNo()),
                comment.getContent(),
                formatDateTime(comment.getCreatedAt())
        );
    }

    private String authorLabel(Long userNo) {
        if (userNo == null) {
            return "-";
        }
        return userRepository.findById(userNo)
                .map(this::userLabel)
                .orElse("회원 #" + userNo);
    }

    private String userLabel(User user) {
        return "%s (#%d)".formatted(user.getUserName(), user.getUserNo());
    }

    private String blankToDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
