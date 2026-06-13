package com.beggar.api.dto.admin;

public class RoomDetail {

    private final Long roomNo;
    private final String roomName;
    private final String roomCode;
    private final String ownerLabel;
    private final String location;
    private final int maxMemberCount;
    private final String status;
    private final String createdAt;
    private final String endedAt;
    private final String deletedAt;
    private final long memberCount;
    private final long activeMemberCount;
    private final String minBudgetPerPerson;
    private final String totalBudget;
    private final String budgetConfirmedAt;
    private final long receiptCount;
    private final String receiptAmount;

    public RoomDetail(
            Long roomNo,
            String roomName,
            String roomCode,
            String ownerLabel,
            String location,
            int maxMemberCount,
            String status,
            String createdAt,
            String endedAt,
            String deletedAt,
            long memberCount,
            long activeMemberCount,
            String minBudgetPerPerson,
            String totalBudget,
            String budgetConfirmedAt,
            long receiptCount,
            String receiptAmount
    ) {
        this.roomNo = roomNo;
        this.roomName = roomName;
        this.roomCode = roomCode;
        this.ownerLabel = ownerLabel;
        this.location = location;
        this.maxMemberCount = maxMemberCount;
        this.status = status;
        this.createdAt = createdAt;
        this.endedAt = endedAt;
        this.deletedAt = deletedAt;
        this.memberCount = memberCount;
        this.activeMemberCount = activeMemberCount;
        this.minBudgetPerPerson = minBudgetPerPerson;
        this.totalBudget = totalBudget;
        this.budgetConfirmedAt = budgetConfirmedAt;
        this.receiptCount = receiptCount;
        this.receiptAmount = receiptAmount;
    }

    public Long getRoomNo() {
        return roomNo;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getOwnerLabel() {
        return ownerLabel;
    }

    public String getLocation() {
        return location;
    }

    public int getMaxMemberCount() {
        return maxMemberCount;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return switch (status) {
            case "INVITING" -> "초대중";
            case "BUDGET_INPUT" -> "예산 입력중";
            case "BUDGET_DONE" -> "예산 확정";
            case "ENDED" -> "종료";
            case "DELETED" -> "삭제";
            default -> "진행중";
        };
    }

    public String getStatusClass() {
        return switch (status) {
            case "ENDED" -> "status-muted";
            case "DELETED" -> "status-danger";
            default -> "status-active";
        };
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getEndedAt() {
        return endedAt;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public boolean isCanEnd() {
        return !"ENDED".equals(status) && !"DELETED".equals(status);
    }

    public boolean isCanDelete() {
        return !"DELETED".equals(status);
    }

    public long getMemberCount() {
        return memberCount;
    }

    public long getActiveMemberCount() {
        return activeMemberCount;
    }

    public String getMinBudgetPerPerson() {
        return minBudgetPerPerson;
    }

    public String getTotalBudget() {
        return totalBudget;
    }

    public String getBudgetConfirmedAt() {
        return budgetConfirmedAt;
    }

    public long getReceiptCount() {
        return receiptCount;
    }

    public String getReceiptAmount() {
        return receiptAmount;
    }
}
