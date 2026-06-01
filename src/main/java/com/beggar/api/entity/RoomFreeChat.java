package com.beggar.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_free_chats", indexes = {
        @Index(name = "idx_rf_chat_created", columnList = "created_at DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomFreeChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long chatId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_no", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rf_chats_user"))
    private User user;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public RoomFreeChat(User user, String message) {
        this.user = user;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }
}
