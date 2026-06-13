package com.beggar.api.dto.admin;

public class RoomListItem {

    private final Long roomNo;
    private final String roomName;
    private final String roomCode;
    private final String ownerLabel;
    private final String location;
    private final String status;
    private final String createdAt;

    public RoomListItem(
            Long roomNo,
            String roomName,
            String roomCode,
            String ownerLabel,
            String location,
            String status,
            String createdAt
    ) {
        this.roomNo = roomNo;
        this.roomName = roomName;
        this.roomCode = roomCode;
        this.ownerLabel = ownerLabel;
        this.location = location;
        this.status = status;
        this.createdAt = createdAt;
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
}
