package com.beggar.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_purpose_tags")
@Getter
@NoArgsConstructor
public class RoomPurposeTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer tagId;

    @ManyToOne
    @JoinColumn(name = "room_no", nullable = false) // 실제 매핑은 방이랑
    private Room room;

    @Column(name = "tag_tags", nullable = false, length = 30)
    private String tag;

    public RoomPurposeTag(Room room, String tag) {
        this.room = room;
        this.tag = tag;
    }
}