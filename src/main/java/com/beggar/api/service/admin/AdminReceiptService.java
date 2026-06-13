package com.beggar.api.service.admin;

import com.beggar.api.dto.admin.ReceiptDetail;
import com.beggar.api.dto.admin.ReceiptListItem;
import com.beggar.api.entity.Receipt;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomMember;
import com.beggar.api.entity.User;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.RoomMemberRepository;
import com.beggar.api.repository.RoomRepository;
import com.beggar.api.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
public class AdminReceiptService {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final NumberFormat MONEY_FORMATTER = NumberFormat.getNumberInstance(Locale.KOREA);

    private final ReceiptRepository receiptRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final AdminActionLogService actionLogService;

    public AdminReceiptService(
            ReceiptRepository receiptRepository,
            RoomRepository roomRepository,
            RoomMemberRepository roomMemberRepository,
            UserRepository userRepository,
            AdminActionLogService actionLogService
    ) {
        this.receiptRepository = receiptRepository;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.actionLogService = actionLogService;
    }

    @Transactional(readOnly = true)
    public Page<ReceiptListItem> getReceipts(
            String keyword,
            Long roomNo,
            Long roomMemberId,
            LocalDate fromDate,
            LocalDate toDate,
            int page
    ) {
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
                safePage,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        String trimmed = keyword == null ? "" : keyword.trim();
        LocalDateTime fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay();

        Page<Receipt> receipts = receiptRepository.searchReceipts(
                trimmed,
                roomNo,
                roomMemberId,
                fromDateTime,
                toDateTime,
                pageable
        );

        return new PageImpl<>(
                receipts.getContent().stream().map(this::toListItem).toList(),
                pageable,
                receipts.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public ReceiptDetail getReceiptDetail(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("영수증을 찾을 수 없어."));

        return new ReceiptDetail(
                receipt.getReceiptId(),
                roomLabel(receipt.getRoom()),
                uploaderLabel(receipt.getUploader()),
                receiptTypeLabel(receipt.getReceiptType()),
                inputMethodLabel(receipt.getInputMethod()),
                blankToDash(receipt.getImageUrl()),
                ocrStatusLabel(receipt.getOcrStatus()),
                blankToDash(receipt.getStoreName()),
                money(receipt.getTotalAmount()),
                money(receipt.getAmount()),
                blankToDash(receipt.getAddress()),
                goodPriceMatchedLabel(receipt.getGoodPriceMatched()),
                blankToDash(receipt.getGoodPriceStoreId()),
                blankToDash(receipt.getGoodPriceStoreName()),
                blankToDash(receipt.getGoodPriceStoreAddress()),
                formatDateTime(receipt.getGoodPriceVerifiedAt()),
                formatDateTime(receipt.getCreatedAt())
        );
    }

    @Transactional
    public void deleteReceipt(Long receiptId) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new IllegalArgumentException("영수증을 찾을 수 없어."));
        receiptRepository.deleteById(receiptId);
        actionLogService.record("DELETE", "RECEIPT", receiptId, "영수증을 삭제했어. 방 #" + (receipt.getRoom() != null ? receipt.getRoom().getRoomNo() : "-"));
    }

    private ReceiptListItem toListItem(Receipt receipt) {
        return new ReceiptListItem(
                receipt.getReceiptId(),
                roomLabel(receipt.getRoom()),
                uploaderLabel(receipt.getUploader()),
                blankToDash(receipt.getStoreName()),
                receiptTypeLabel(receipt.getReceiptType()),
                inputMethodLabel(receipt.getInputMethod()),
                ocrStatusLabel(receipt.getOcrStatus()),
                money(receipt.getAmount()),
                goodPriceMatchedLabel(receipt.getGoodPriceMatched()),
                formatDateTime(receipt.getCreatedAt())
        );
    }

    private String roomLabel(Room room) {
        if (room == null) {
            return "-";
        }
        return "%s (#%d)".formatted(blankToDash(room.getRoomName()), room.getRoomNo());
    }

    private String uploaderLabel(RoomMember uploader) {
        if (uploader == null) {
            return "-";
        }

        User user = uploader.getUser();
        if (user == null) {
            return "멤버 #" + uploader.getRoomMemberId();
        }

        return userLabel(user);
    }

    private String userLabel(User user) {
        return "%s (#%d)".formatted(blankToDash(user.getUserName()), user.getUserNo());
    }

    private String receiptTypeLabel(Object value) {
        if (value == null) return "-";
        String s = value.toString();
        if ("SPLIT".equals(s)) {
            return "분할";
        }
        if ("COMBINED".equals(s)) {
            return "통합";
        }
        return blankToDash(s);
    }

    private String inputMethodLabel(Object value) {
        if (value == null) return "-";
        return switch (value.toString()) {
            case "CAMERA" -> "촬영";
            case "GALLERY" -> "갤러리";
            case "MANUAL" -> "수동";
            default -> blankToDash(value.toString());
        };
    }

    private String ocrStatusLabel(Object value) {
        if (value == null) return "-";
        return switch (value.toString()) {
            case "PENDING" -> "대기";
            case "SUCCESS" -> "성공";
            case "FAILED" -> "실패";
            case "CANCELED" -> "취소";
            case "MANUAL" -> "수동";
            default -> blankToDash(value.toString());
        };
    }

    private String goodPriceMatchedLabel(Boolean matched) {
        return Boolean.TRUE.equals(matched) ? "매칭" : "미매칭";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    private String money(Integer value) {
        if (value == null) {
            return "-";
        }
        return MONEY_FORMATTER.format(value) + "원";
    }

    private String blankToDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}
