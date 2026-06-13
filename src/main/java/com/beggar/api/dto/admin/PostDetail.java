package com.beggar.api.dto.admin;

import java.util.List;

public class PostDetail {

    private final Long postId;
    private final String title;
    private final String content;
    private final String tag;
    private final String authorLabel;
    private final String createdAt;
    private final List<PostCommentItem> comments;

    public PostDetail(Long postId, String title, String content, String tag, String authorLabel, String createdAt, List<PostCommentItem> comments) {
        this.postId = postId;
        this.title = title;
        this.content = content;
        this.tag = tag;
        this.authorLabel = authorLabel;
        this.createdAt = createdAt;
        this.comments = comments;
    }

    public Long getPostId() {
        return postId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getTag() {
        return tag;
    }

    public String getAuthorLabel() {
        return authorLabel;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public List<PostCommentItem> getComments() {
        return comments;
    }
}
