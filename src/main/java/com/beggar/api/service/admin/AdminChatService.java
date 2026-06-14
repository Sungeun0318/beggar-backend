package com.beggar.api.service.admin;

import com.beggar.api.dto.admin.ChatListItem;
import com.beggar.api.entity.RoomFreeChat;
import com.beggar.api.entity.User;
import com.beggar.api.repository.RoomFreeChatRepository;
import com.beggar.api.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AdminChatService {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final RoomFreeChatRepository chatRepository;
    private final UserRepository userRepository;
    private final AdminActionLogService actionLogService;

    public AdminChatService(
            RoomFreeChatRepository chatRepository,
            UserRepository userRepository,
            AdminActionLogService actionLogService
    ) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.actionLogService = actionLogService;
    }

    @Transactional(readOnly = true)
    public Page<ChatListItem> getChats(String keyword, Long userNo, int page) {
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
                safePage,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        String trimmed = keyword == null ? "" : keyword.trim();

        Page<RoomFreeChat> chats;
        if (userNo != null && !trimmed.isEmpty()) {
            chats = chatRepository.findByUser_UserNoAndMessageContainingIgnoreCase(userNo, trimmed, pageable);
        } else if (userNo != null) {
            chats = chatRepository.findByUser_UserNo(userNo, pageable);
        } else if (!trimmed.isEmpty()) {
            chats = chatRepository.findByMessageContainingIgnoreCase(trimmed, pageable);
        } else {
            chats = chatRepository.findAll(pageable);
        }

        return new PageImpl<>(
                chats.getContent().stream().map(this::toListItem).toList(),
                pageable,
                chats.getTotalElements()
        );
    }

    @Transactional
    public void deleteChat(Long chatId) {
        RoomFreeChat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("채팅을 찾을 수 없어."));
        chatRepository.deleteById(chatId);
        actionLogService.record("DELETE", "CHAT", chatId, "#" + chatId + " 채팅 삭제");
    }

    private ChatListItem toListItem(RoomFreeChat chat) {
        return new ChatListItem(
                chat.getChatId(),
                authorLabel(chat.getUser().getUserNo()),
                chat.getMessage(),
                formatDateTime(chat.getCreatedAt())
        );
    }

    private String authorLabel(Long userNo) {
        if (userNo == null) {
            return "-";
        }
        return userRepository.findById(userNo)
                .map(this::userLabel)
                .orElse("회원 #" + userNo);
    }

    private String userLabel(User user) {
        return "%s (#%d)".formatted(user.getUserName(), user.getUserNo());
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
