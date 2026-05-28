package com.beggar.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rooms_room_name", columnNames = "room_name"),
                @UniqueConstraint(name = "uk_rooms_room_code", columnNames = "room_code")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_no")
    private Long roomNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_no", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rooms_user"))
    private User owner;

    @Column(name = "room_name", length = 15, nullable = false)
    private String roomName;

    @Column(name = "room_code", length = 15, nullable = false)
    private String roomCode;

    @Column(name = "total_budget")
    private Integer totalBudget;

    @Column(name = "room_created", nullable = false)
    private LocalDateTime roomCreated;

    @Column(name = "is_friends" , nullable = false)
    private Boolean isFriends;

    @Builder
    public Room(User owner, String roomName, String roomCode, Integer totalBudget, Boolean isFriends) {
        this.owner = owner;
        this.roomName = roomName;
        this.roomCode = roomCode;
        this.totalBudget = totalBudget;
        this.isFriends = isFriends;
        this.roomCreated = LocalDateTime.now();
    }

    public void updateTotalBudget(int totalBudget) {
        this.totalBudget = totalBudget;
    }
}
