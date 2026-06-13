package com.beggar.api.service.admin;
import com.beggar.api.dto.admin.CommentListItem;
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
public class AdminCommentService {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final RoomFreeCommentRepository commentRepository;
    private final RoomFreePostRepository postRepository;
    private final UserRepository userRepository;
    private final AdminActionLogService actionLogService;

    public AdminCommentService(
            RoomFreeCommentRepository commentRepository,
            RoomFreePostRepository postRepository,
            UserRepository userRepository,
            AdminActionLogService actionLogService
    ) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.actionLogService = actionLogService;
    }

    @Transactional(readOnly = true)
    public Page<CommentListItem> getComments(String keyword, Long postId, int page) {
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
                safePage,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        String trimmed = keyword == null ? "" : keyword.trim();

        Page<RoomFreeComment> comments;
        if (postId != null && !trimmed.isEmpty()) {
            comments = commentRepository.findByPost_PostIdAndContentContainingIgnoreCase(postId, trimmed, pageable);
        } else if (postId != null) {
            comments = commentRepository.findByPost_PostId(postId, pageable);
        } else if (!trimmed.isEmpty()) {
            comments = commentRepository.findByContentContainingIgnoreCase(trimmed, pageable);
        } else {
            comments = commentRepository.findAll(pageable);
        }

        return new PageImpl<>(
                comments.getContent().stream().map(this::toListItem).toList(),
                pageable,
                comments.getTotalElements()
        );
    }

    @Transactional
    public void deleteComment(Long commentId) {
        RoomFreeComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없어."));
        commentRepository.deleteById(commentId);
        actionLogService.record("DELETE", "COMMENT", commentId, "댓글을 삭제했어. 게시글 #" + comment.getPost().getPostId());
    }

    private CommentListItem toListItem(RoomFreeComment comment) {
        return new CommentListItem(
                comment.getCommentId(),
                comment.getPost().getPostId(),
                postTitle(comment.getPost().getPostId()),
                authorLabel(comment.getAuthor().getUserNo()),
                comment.getContent(),
                formatDateTime(comment.getCreatedAt())
        );
    }

    private String postTitle(Long postId) {
        if (postId == null) {
            return "-";
        }
        return postRepository.findById(postId)
                .map(RoomFreePost::getTitle)
                .orElse("게시글 #" + postId);
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

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
