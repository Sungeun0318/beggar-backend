package com.beggar.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_members_room_user", columnNames = {"room_no", "user_no"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMember {

    public enum Status { ACTIVE, LEFT, KICKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_member_id")
    private Long roomMemberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_no", nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_members_room"))
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_no", nullable = false,
            foreignKey = @ForeignKey(name = "fk_room_members_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 15, nullable = false)
    private Status status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Builder
    public RoomMember(Room room, User user, Status status) {
        this.room = room;
        this.user = user;
        this.status = (status == null) ? Status.ACTIVE : status;
        this.joinedAt = LocalDateTime.now();
    }

    public void leave() {
        this.status = Status.LEFT;
        this.leftAt = LocalDateTime.now();
    }

    public void kick() {
        this.status = Status.KICKED;
        this.leftAt = LocalDateTime.now();
    }
}
