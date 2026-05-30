package com.beggar.api.entity;

import com.beggar.api.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "community_posts",
        indexes = @Index(name = "idx_community_posts_created_at", columnList = "created_at DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_no", nullable = false,
            foreignKey = @ForeignKey(name = "fk_community_posts_user"))
    private User user;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "category", length = 30)
    private String category;

    @Builder
    public CommunityPost(User user, String title, String content, String category) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.category = category;
    }
}
