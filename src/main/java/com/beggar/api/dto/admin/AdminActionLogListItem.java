package com.beggar.api.dto.admin;

public class AdminActionLogListItem {

    private final Long logId;
    private final String adminUsername;
    private final String actionLabel;
    private final String targetTypeLabel;
    private final String targetId;
    private final String message;
    private final String createdAt;

    public AdminActionLogListItem(
            Long logId,
            String adminUsername,
            String actionLabel,
            String targetTypeLabel,
            String targetId,
            String message,
            String createdAt
    ) {
        this.logId = logId;
        this.adminUsername = adminUsername;
        this.actionLabel = actionLabel;
        this.targetTypeLabel = targetTypeLabel;
        this.targetId = targetId;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getLogId() {
        return logId;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public String getTargetTypeLabel() {
        return targetTypeLabel;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
