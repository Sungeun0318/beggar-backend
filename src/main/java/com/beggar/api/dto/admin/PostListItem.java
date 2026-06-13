package com.beggar.api.dto.admin;

public class PostListItem {

    private final Long postId;
    private final String title;
    private final String tag;
    private final String authorLabel;
    private final long commentCount;
    private final String createdAt;

    public PostListItem(Long postId, String title, String tag, String authorLabel, long commentCount, String createdAt) {
        this.postId = postId;
        this.title = title;
        this.tag = tag;
        this.authorLabel = authorLabel;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
    }

    public Long getPostId() {
        return postId;
    }

    public String getTitle() {
        return title;
    }

    public String getTag() {
        return tag;
    }

    public String getAuthorLabel() {
        return authorLabel;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
