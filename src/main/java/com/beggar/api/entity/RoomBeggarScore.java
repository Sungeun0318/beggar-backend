package com.beggar.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_beggar_scores",
        uniqueConstraints = @UniqueConstraint(name = "uk_room_beggar_scores_room", columnNames = "room_no"),
        indexes = @Index(name = "idx_room_scores_score_desc", columnList = "score DESC"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomBeggarScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long scoreId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_no", nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_beggar_scores_room"))
    private Room room;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "title", length = 30, nullable = false)
    private String title;

    @Column(name = "total_spent_amount", nullable = false)
    private Long totalSpentAmount;

    @Column(name = "total_saved_amount", nullable = false)
    private Long totalSavedAmount;

    @Column(name = "good_price_verified_count", nullable = false)
    private Integer goodPriceVerifiedCount;

    @Column(name = "budget_compliance_rate", precision = 5, scale = 2)
    private BigDecimal budgetComplianceRate;

    @Column(name = "avg_savings_ratio", precision = 5, scale = 2)
    private BigDecimal avgSavingsRatio;

    @Column(name = "last_calculated_at", nullable = false)
    private LocalDateTime lastCalculatedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public RoomBeggarScore(Room room) {
        this.room = room;
        this.score = 0;
        this.title = "아기 거지";
        this.totalSpentAmount = 0L;
        this.totalSavedAmount = 0L;
        this.goodPriceVerifiedCount = 0;
        this.budgetComplianceRate = BigDecimal.ZERO;
        this.avgSavingsRatio = BigDecimal.ZERO;
        this.lastCalculatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void update(int score, long totalSpentAmount, long totalSavedAmount,
                       int goodPriceVerifiedCount, BigDecimal compliance,
                       BigDecimal savingsRatio) {
        this.score = score;
        this.title = resolveTitle(score);
        this.totalSpentAmount = totalSpentAmount;
        this.totalSavedAmount = totalSavedAmount;
        this.goodPriceVerifiedCount = goodPriceVerifiedCount;
        this.budgetComplianceRate = compliance;
        this.avgSavingsRatio = savingsRatio;
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
