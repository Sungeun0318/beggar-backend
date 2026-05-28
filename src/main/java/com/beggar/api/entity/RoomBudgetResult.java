package com.beggar.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_budget_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_results_room", columnNames = "room_no"
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomBudgetResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_no", nullable = false,
            foreignKey = @ForeignKey(name = "fk_results_room"))
    private Room room;

    @Column(name = "min_budget_per_person", nullable = false)
    private Integer minBudgetPerPerson;

    @Column(name = "member_count", nullable = false)
    private Integer memberCount;

    @Column(name = "total_budget", nullable = false)
    private Integer totalBudget;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    @Builder
    public RoomBudgetResult(Room room, Integer minBudgetPerPerson, Integer memberCount, Integer totalBudget) {
        this.room = room;
        this.minBudgetPerPerson = minBudgetPerPerson;
        this.memberCount = memberCount;
        this.totalBudget = totalBudget;
        this.confirmedAt = LocalDateTime.now();
    }
}
