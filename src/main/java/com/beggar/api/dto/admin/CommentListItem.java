package com.beggar.api.dto.admin;

public class CommentListItem {

    private final Long commentId;
    private final Long postId;
    private final String postTitle;
    private final String authorLabel;
    private final String content;
    private final String createdAt;

    public CommentListItem(
            Long commentId,
            Long postId,
            String postTitle,
            String authorLabel,
            String content,
            String createdAt
    ) {
        this.commentId = commentId;
        this.postId = postId;
        this.postTitle = postTitle;
        this.authorLabel = authorLabel;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getCommentId() {
        return commentId;
    }

    public Long getPostId() {
        return postId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public String getAuthorLabel() {
        return authorLabel;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
