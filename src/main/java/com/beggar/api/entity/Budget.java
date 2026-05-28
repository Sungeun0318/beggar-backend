package com.beggar.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "budgets",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_budgets_member", columnNames = "room_member_id"
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "budget_id")
    private Long budgetId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_budgets_member"))
    private RoomMember roomMember;

    @Column(name = "budget_amount", nullable = false)
    private Integer budgetAmount;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Builder
    public Budget(RoomMember roomMember, Integer budgetAmount) {
        this.roomMember = roomMember;
        this.budgetAmount = budgetAmount;
        this.submittedAt = LocalDateTime.now();
    }

    public void updateAmount(int newAmount) {
        this.budgetAmount = newAmount;
        this.submittedAt = LocalDateTime.now();
    }
}
