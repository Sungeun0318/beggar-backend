package com.beggar.api.dto.admin;

public class PostCommentItem {

    private final Long commentId;
    private final String authorLabel;
    private final String content;
    private final String createdAt;

    public PostCommentItem(Long commentId, String authorLabel, String content, String createdAt) {
        this.commentId = commentId;
        this.authorLabel = authorLabel;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getCommentId() {
        return commentId;
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
