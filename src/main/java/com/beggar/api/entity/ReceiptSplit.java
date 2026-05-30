package com.beggar.api.entity;

import com.beggar.api.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "receipt_splits",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_receipt_splits_receipt_member",
                columnNames = {"receipt_id", "room_member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReceiptSplit extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "split_id")
    private Long splitId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipt_splits_receipt"))
    private Receipt receipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipt_splits_member"))
    private RoomMember roomMember;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Builder
    public ReceiptSplit(Receipt receipt, RoomMember roomMember, Integer amount) {
        this.receipt = receipt;
        this.roomMember = roomMember;
        this.amount = (amount == null) ? 0 : amount;
    }

    public void updateAmount(int amount) {
        this.amount = amount;
    }
}
