package com.beggar.api.dto.admin;

public class ChatListItem {

    private final Long chatId;
    private final String authorLabel;
    private final String message;
    private final String createdAt;

    public ChatListItem(Long chatId, String authorLabel, String message, String createdAt) {
        this.chatId = chatId;
        this.authorLabel = authorLabel;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getChatId() {
        return chatId;
    }

    public String getAuthorLabel() {
        return authorLabel;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
