package com.beggar.api.entity;

import com.beggar.api.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Receipt extends BaseTimeEntity {

    public enum ReceiptType { COMBINED, SPLIT, PERSONAL }
    public enum InputMethod { CAMERA, GALLERY, MANUAL }
    public enum OcrStatus { PENDING, SUCCESS, FAILED, CANCELED, MANUAL }

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

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_type", length = 20, nullable = false)
    private ReceiptType receiptType;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_method", length = 20, nullable = false)
    private InputMethod inputMethod;

    @Column(name = "image_url", length = 500)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "split_group_id",
            foreignKey = @ForeignKey(name = "fk_receipts_split_group"))
    private ReceiptSplitGroup splitGroup;

    @Column(name = "good_price_store_id", length = 100)
    private String goodPriceStoreId;

    @Column(name = "good_price_store_name", length = 150)
    private String goodPriceStoreName;

    @Column(name = "good_price_store_address", length = 200)
    private String goodPriceStoreAddress;

    @Column(name = "good_price_matched", nullable = false)
    private Boolean goodPriceMatched;

    @Column(name = "good_price_verified_at")
    private java.time.LocalDateTime goodPriceVerifiedAt;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReceiptSplit> splits = new ArrayList<>();

    @Builder
    public Receipt(Room room, RoomMember uploader, ReceiptType receiptType,
                   InputMethod inputMethod, String imageUrl, OcrStatus ocrStatus,
                   String storeName, Integer totalAmount, Integer amount,
                   String address, BigDecimal centerLat, BigDecimal centerLng,
                   ReceiptSplitGroup splitGroup) {
        this.room = room;
        this.uploader = uploader;
        this.receiptType = (receiptType == null) ? ReceiptType.COMBINED : receiptType;
        this.inputMethod = (inputMethod == null) ? InputMethod.CAMERA : inputMethod;
        this.imageUrl = imageUrl;
        this.ocrStatus = (ocrStatus == null)
                ? (this.inputMethod == InputMethod.MANUAL ? OcrStatus.MANUAL : OcrStatus.PENDING)
                : ocrStatus;
        this.storeName = storeName;
        this.totalAmount = totalAmount;
        this.amount = (amount == null) ? 0 : amount;
        this.address = address;
        this.centerLat = centerLat;
        this.centerLng = centerLng;
        this.splitGroup = splitGroup;
        this.goodPriceMatched = false;
    }

    public void addSplit(ReceiptSplit split) {
        this.splits.add(split);
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

    public void updateManualInfo(String storeName, String address, BigDecimal lat, BigDecimal lng) {
        if (storeName != null && !storeName.isBlank()) {
            this.storeName = storeName;
        }
        if (address != null && !address.isBlank()) {
            this.address = address;
        }
        if (lat != null) {
            this.centerLat = lat;
        }
        if (lng != null) {
            this.centerLng = lng;
        }
    }

    public void applyGoodPriceMatch(String storeId, String storeName, String storeAddress,
                                    java.time.LocalDateTime verifiedAt) {
        this.goodPriceStoreId = storeId;
        this.goodPriceStoreName = storeName;
        this.goodPriceStoreAddress = storeAddress;
        this.goodPriceMatched = true;
        this.goodPriceVerifiedAt = verifiedAt;
    }

    public void clearGoodPriceMatch() {
        this.goodPriceStoreId = null;
        this.goodPriceStoreName = null;
        this.goodPriceStoreAddress = null;
        this.goodPriceMatched = false;
        this.goodPriceVerifiedAt = null;
    }
}
