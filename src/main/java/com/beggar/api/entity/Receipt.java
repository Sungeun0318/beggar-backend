package com.beggar.api.entity;

import com.beggar.api.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Receipt extends BaseTimeEntity {

    public enum OcrStatus { PENDING, SUCCESS, FAILED, CANCELED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receipt_id")
    private Long receiptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_no", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipts_room"))
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_receipts_member"))
    private RoomMember uploader;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "ocr_status", length = 30, nullable = false)
    private OcrStatus ocrStatus;

    @Column(name = "store_name", length = 150)
    private String storeName;

    @Column(name = "total_amount")
    private Integer totalAmount;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "address", length = 100)
    private String address;

    @Column(name = "center_lat", precision = 10, scale = 7)
    private BigDecimal centerLat;

    @Column(name = "center_lng", precision = 10, scale = 7)
    private BigDecimal centerLng;

    @Builder
    public Receipt(Room room, RoomMember uploader, String imageUrl,
                   OcrStatus ocrStatus, Integer amount) {
        this.room = room;
        this.uploader = uploader;
        this.imageUrl = imageUrl;
        this.ocrStatus = (ocrStatus == null) ? OcrStatus.PENDING : ocrStatus;
        this.amount = (amount == null) ? 0 : amount;
    }

    public void applyOcrResult(String storeName, Integer totalAmount,
                               String address, BigDecimal lat, BigDecimal lng) {
        this.ocrStatus = OcrStatus.SUCCESS;
        this.storeName = storeName;
        this.totalAmount = totalAmount;
        this.amount = (totalAmount == null) ? this.amount : totalAmount;
        this.address = address;
        this.centerLat = lat;
        this.centerLng = lng;
    }

    public void markOcrFailed() {
        this.ocrStatus = OcrStatus.FAILED;
    }

    public void updateAmount(int newAmount) {
        this.amount = newAmount;
    }
}
