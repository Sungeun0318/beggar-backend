package com.beggar.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_action_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AdminActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(nullable = false)
    private String adminUsername;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String targetType;

    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String message;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AdminActionLog(String adminUsername, String action, String targetType, String targetId, String message) {
        this.adminUsername = adminUsername;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.message = message;
    }
}
