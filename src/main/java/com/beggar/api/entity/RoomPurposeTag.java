package com.beggar.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_purpose_tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomPurposeTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long tagId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_no", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tags_room"))
    private Room room;

    @Column(name = "tag_tags", length = 30, nullable = false)
    private String tag;

    @Builder
    public RoomPurposeTag(Room room, String tag) {
        this.room = room;
        this.tag = tag;
    }
}
