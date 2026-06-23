package com.beggar.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_roulette_results",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_roulette_results_room", columnNames = "room_no"
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomRouletteResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_no", nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_roulette_results_room"))
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "winner_user_no", nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_roulette_results_winner"))
    private User winner;

    @Column(name = "winner_nickname", nullable = false, length = 50)
    private String winnerNickname;

    @Column(name = "remaining_budget", nullable = false)
    private Long remainingBudget;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    public RoomRouletteResult(Room room, User winner, Long remainingBudget) {
        this.room = room;
        this.winner = winner;
        this.winnerNickname = winner.getUserName();
        this.remainingBudget = remainingBudget;
        this.confirmedAt = LocalDateTime.now();
    }
}
