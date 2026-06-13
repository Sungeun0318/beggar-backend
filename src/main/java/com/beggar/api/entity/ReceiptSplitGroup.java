package com.beggar.api.entity;

import com.beggar.api.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "receipt_split_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReceiptSplitGroup extends BaseTimeEntity {

    public enum SplitGroupStatus { OPEN, CLOSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "split_group_id")
    private Long splitGroupId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_no", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipt_split_groups_room"))
    private Room room;

    @Column(name = "store_name", length = 150, nullable = false)
    private String storeName;

    @Column(name = "address", length = 200, nullable = false)
    private String address;

    @Column(name = "center_lat", precision = 10, scale = 7)
    private BigDecimal centerLat;

    @Column(name = "center_lng", precision = 10, scale = 7)
    private BigDecimal centerLng;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SplitGroupStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_room_member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipt_split_groups_created_by"))
    private RoomMember createdBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Builder
    public ReceiptSplitGroup(Room room, String storeName, String address,
                             BigDecimal centerLat, BigDecimal centerLng,
                             SplitGroupStatus status, RoomMember createdBy,
                             LocalDateTime closedAt) {
        this.room = room;
        this.storeName = storeName;
        this.address = address;
        this.centerLat = centerLat;
        this.centerLng = centerLng;
        this.status = status == null ? SplitGroupStatus.OPEN : status;
        this.createdBy = createdBy;
        this.closedAt = closedAt;
    }

    public void close() {
        this.status = SplitGroupStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }
}
