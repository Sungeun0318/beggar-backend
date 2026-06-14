package com.beggar.api.dto.admin;

public class ReceiptListItem {

    private final Long receiptId;
    private final String roomLabel;
    private final String uploaderLabel;
    private final String storeName;
    private final String receiptTypeLabel;
    private final String inputMethodLabel;
    private final String ocrStatusLabel;
    private final String amount;
    private final String goodPriceLabel;
    private final String receiptIssuedAt;
    private final String createdAt;

    public ReceiptListItem(
            Long receiptId,
            String roomLabel,
            String uploaderLabel,
            String storeName,
            String receiptTypeLabel,
            String inputMethodLabel,
            String ocrStatusLabel,
            String amount,
            String goodPriceLabel,
            String receiptIssuedAt,
            String createdAt
    ) {
        this.receiptId = receiptId;
        this.roomLabel = roomLabel;
        this.uploaderLabel = uploaderLabel;
        this.storeName = storeName;
        this.receiptTypeLabel = receiptTypeLabel;
        this.inputMethodLabel = inputMethodLabel;
        this.ocrStatusLabel = ocrStatusLabel;
        this.amount = amount;
        this.goodPriceLabel = goodPriceLabel;
        this.receiptIssuedAt = receiptIssuedAt;
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

    public String getStoreName() {
        return storeName;
    }

    public String getReceiptTypeLabel() {
        return receiptTypeLabel;
    }

    public String getInputMethodLabel() {
        return inputMethodLabel;
    }

    public String getOcrStatusLabel() {
        return ocrStatusLabel;
    }

    public String getAmount() {
        return amount;
    }

    public String getGoodPriceLabel() {
        return goodPriceLabel;
    }

    public String getReceiptIssuedAt() {
        return receiptIssuedAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
