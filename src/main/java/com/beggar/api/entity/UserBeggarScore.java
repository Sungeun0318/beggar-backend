package com.beggar.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_beggar_scores",
        indexes = @Index(name = "idx_scores_score_desc", columnList = "score DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBeggarScore {

    @Id
    @Column(name = "user_no")
    private Long userNo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_no",
            foreignKey = @ForeignKey(name = "fk_scores_user"))
    private User user;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "title", length = 30, nullable = false)
    private String title;

    @Column(name = "total_saved_amount", nullable = false)
    private Long totalSavedAmount;

    @Column(name = "budget_compliance_rate", precision = 5, scale = 2)
    private BigDecimal budgetComplianceRate;

    @Column(name = "avg_savings_ratio", precision = 5, scale = 2)
    private BigDecimal avgSavingsRatio;

    @Column(name = "participation_count", nullable = false)
    private Integer participationCount;

    @Column(name = "last_calculated_at", nullable = false)
    private LocalDateTime lastCalculatedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public UserBeggarScore(User user) {
        this.user = user;
        this.score = 0;
        this.title = "아기 거지";
        this.totalSavedAmount = 0L;
        this.participationCount = 0;
        this.lastCalculatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(int score, String title, long totalSavedAmount,
                       BigDecimal compliance, BigDecimal savingsRatio, int participation) {
        this.score = score;
        this.title = title;
        this.totalSavedAmount = totalSavedAmount;
        this.budgetComplianceRate = compliance;
        this.avgSavingsRatio = savingsRatio;
        this.participationCount = participation;
        this.lastCalculatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static String resolveTitle(int score) {
        if (score < 20) return "아기 거지";
        if (score < 40) return "성장하는 거지";
        if (score < 60) return "알뜰한 거지";
        if (score < 80) return "프로 거지";
        return "전설의 거지";
    }
}
