package com.beggar.api.service.admin;


import com.beggar.api.dto.admin.AdminActionLogListItem;
import com.beggar.api.entity.AdminActionLog;
import com.beggar.api.repository.admin.AdminActionLogRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AdminActionLogService {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final AdminActionLogRepository logRepository;

    public AdminActionLogService(AdminActionLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminActionLogListItem> getLogs(
            String adminUsername,
            String action,
            String targetType,
            String keyword,
            int page
    ) {
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
                safePage,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<AdminActionLog> logs = logRepository.searchLogs(
                clean(adminUsername),
                clean(action),
                clean(targetType),
                clean(keyword),
                pageable
        );

        return new PageImpl<>(
                logs.getContent().stream().map(this::toListItem).toList(),
                pageable,
                logs.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public AdminActionLogListItem getLog(Long logId) {
        AdminActionLog log = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("운영 로그를 찾을 수 없어."));
        return toListItem(log);
    }

    @Transactional
    public void record(String action, String targetType, Object targetId, String message) {
        logRepository.save(new AdminActionLog(
                "admin",
                action,
                targetType,
                targetId == null ? null : String.valueOf(targetId),
                message
        ));
    }

    private AdminActionLogListItem toListItem(AdminActionLog log) {
        return new AdminActionLogListItem(
                log.getLogId(),
                log.getAdminUsername(),
                actionLabel(log.getAction()),
                targetTypeLabel(log.getTargetType()),
                blankToDash(log.getTargetId()),
                blankToDash(log.getMessage()),
                formatDateTime(log.getCreatedAt())
        );
    }

//    private String currentAdminUsername() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || authentication.getName() == null) {
//            return "unknown";
//        }
//        return authentication.getName();
//    }

    private String actionLabel(String action) {
        return switch (action == null ? "" : action) {
            case "CREATE" -> "생성";
            case "UPDATE" -> "수정";
            case "DELETE" -> "삭제";
            case "DISABLE" -> "비활성화";
            case "END" -> "종료";
            case "TOGGLE_VISIBLE" -> "노출 변경";
            default -> blankToDash(action);
        };
    }

    private String targetTypeLabel(String targetType) {
        return switch (targetType == null ? "" : targetType) {
            case "ROOM" -> "방";
            case "POST" -> "게시글";
            case "COMMENT" -> "댓글";
            case "CHAT" -> "채팅";
            case "RECEIPT" -> "영수증";
            default -> blankToDash(targetType);
        };
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
