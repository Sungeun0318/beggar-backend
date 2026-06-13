package com.beggar.api.dto.admin;

public class ReceiptDetail {

    private final Long receiptId;
    private final String roomLabel;
    private final String uploaderLabel;
    private final String receiptTypeLabel;
    private final String inputMethodLabel;
    private final String imageUrl;
    private final String ocrStatusLabel;
    private final String storeName;
    private final String totalAmount;
    private final String amount;
    private final String address;
    private final String goodPriceMatchedLabel;
    private final String goodPriceStoreId;
    private final String goodPriceStoreName;
    private final String goodPriceStoreAddress;
    private final String goodPriceVerifiedAt;
    private final String createdAt;

    public ReceiptDetail(
            Long receiptId,
            String roomLabel,
            String uploaderLabel,
            String receiptTypeLabel,
            String inputMethodLabel,
            String imageUrl,
            String ocrStatusLabel,
            String storeName,
            String totalAmount,
            String amount,
            String address,
            String goodPriceMatchedLabel,
            String goodPriceStoreId,
            String goodPriceStoreName,
            String goodPriceStoreAddress,
            String goodPriceVerifiedAt,
            String createdAt
    ) {
        this.receiptId = receiptId;
        this.roomLabel = roomLabel;
        this.uploaderLabel = uploaderLabel;
        this.receiptTypeLabel = receiptTypeLabel;
        this.inputMethodLabel = inputMethodLabel;
        this.imageUrl = imageUrl;
        this.ocrStatusLabel = ocrStatusLabel;
        this.storeName = storeName;
        this.totalAmount = totalAmount;
        this.amount = amount;
        this.address = address;
        this.goodPriceMatchedLabel = goodPriceMatchedLabel;
        this.goodPriceStoreId = goodPriceStoreId;
        this.goodPriceStoreName = goodPriceStoreName;
        this.goodPriceStoreAddress = goodPriceStoreAddress;
        this.goodPriceVerifiedAt = goodPriceVerifiedAt;
        this.createdAt = createdAt;
    }

    public Long getReceiptId() {
        return receiptId;
    }

    public String getRoomLabel() {
        return roomLabel;
    }

    public String getUploaderLabel() {
        return uploaderLabel;
    }

    public String getReceiptTypeLabel() {
        return receiptTypeLabel;
    }

    public String getInputMethodLabel() {
        return inputMethodLabel;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getOcrStatusLabel() {
        return ocrStatusLabel;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public String getAmount() {
        return amount;
    }

    public String getAddress() {
        return address;
    }

    public String getGoodPriceMatchedLabel() {
        return goodPriceMatchedLabel;
    }

    public String getGoodPriceStoreId() {
        return goodPriceStoreId;
    }

    public String getGoodPriceStoreName() {
        return goodPriceStoreName;
    }

    public String getGoodPriceStoreAddress() {
        return goodPriceStoreAddress;
    }

    public String getGoodPriceVerifiedAt() {
        return goodPriceVerifiedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
