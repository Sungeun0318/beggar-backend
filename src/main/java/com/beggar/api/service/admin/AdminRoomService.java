package com.beggar.api.service.admin;

import com.beggar.api.dto.admin.RoomDetail;
import com.beggar.api.dto.admin.RoomListItem;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomBudgetResult;
import com.beggar.api.entity.RoomMember;
import com.beggar.api.entity.User;
import com.beggar.api.repository.RoomBudgetResultRepository;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.RoomMemberRepository;
import com.beggar.api.repository.RoomRepository;
import com.beggar.api.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

@Service
public class AdminRoomService {

    private static final int PAGE_SIZE = 10;
    private static final String STATUS_ALL = "ALL";
    private static final String STATUS_INVITING = "INVITING";
    private static final String STATUS_BUDGET_INPUT = "BUDGET_INPUT";
    private static final String STATUS_BUDGET_DONE = "BUDGET_DONE";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ENDED = "ENDED";
    private static final String STATUS_DELETED = "DELETED";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final NumberFormat MONEY_FORMATTER = NumberFormat.getNumberInstance(Locale.KOREA);

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomBudgetResultRepository budgetResultRepository;
    private final ReceiptRepository receiptRepository;
    private final AdminActionLogService actionLogService;

    public AdminRoomService(
            RoomRepository roomRepository,
            UserRepository userRepository,
            RoomMemberRepository roomMemberRepository,
            RoomBudgetResultRepository budgetResultRepository,
            ReceiptRepository receiptRepository,
            AdminActionLogService actionLogService
    ) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.budgetResultRepository = budgetResultRepository;
        this.receiptRepository = receiptRepository;
        this.actionLogService = actionLogService;
    }

    @Transactional(readOnly = true)
    public Page<RoomListItem> getRooms(String keyword, String status, int page) {
        int safePage = Math.max(page, 0);
        String safeStatus = normalizeStatus(status);
        Pageable pageable = PageRequest.of(
                safePage,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "roomCreated")
        );

        String trimmed = keyword == null ? "" : keyword.trim();
        Page<Room> rooms = roomRepository.searchRooms(trimmed, safeStatus, pageable);

        return new PageImpl<>(
                rooms.getContent().stream().map(this::toListItem).toList(),
                pageable,
                rooms.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public RoomDetail getRoomDetail(Long roomNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));
        Optional<RoomBudgetResult> budgetResult = budgetResultRepository.findByRoom_RoomNo(roomNo);

        return new RoomDetail(
                room.getRoomNo(),
                room.getRoomName(),
                room.getRoomCode(),
                ownerLabel(room.getOwnerUserNo()),
                blankToDash(room.getLocation()),
                room.getMaxMemberCount(),
                normalizeStatus(String.valueOf(room.getStatus())),
                formatDateTime(room.getRoomCreated()),
                formatDateTime(room.getEndedAt()),
                formatDateTime(room.getDeletedAt()),
                roomMemberRepository.countByRoom_RoomNo(roomNo),
                roomMemberRepository.countByRoom_RoomNoAndStatus(roomNo, RoomMember.Status.ACTIVE),
                budgetResult.map(result -> money(result.getMinBudgetPerPerson()) + "원").orElse("-"),
                budgetResult.map(result -> money(result.getTotalBudget()) + "원").orElse("-"),
                budgetResult.map(result -> formatDateTime(result.getConfirmedAt())).orElse("-"),
                receiptRepository.countByRoom_RoomNo(roomNo),
                money(receiptRepository.sumAmountByRoomNo(roomNo)) + "원"
        );
    }

    @Transactional
    public void endRoom(Long roomNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));
        String status = normalizeStatus(String.valueOf(room.getStatus()));

        if (STATUS_DELETED.equals(status)) {
            throw new IllegalStateException("이미 삭제된 방은 종료할 수 없습니다.");
        }
        if (!STATUS_ENDED.equals(status)) {
            room.markEnded(LocalDateTime.now());
            actionLogService.record("END", "ROOM", roomNo, "방을 강제 종료했습니다: " + room.getRoomName());
        }
    }

    @Transactional
    public void deleteRoom(Long roomNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        if (!STATUS_DELETED.equals(normalizeStatus(String.valueOf(room.getStatus())))) {
            room.markDeleted(LocalDateTime.now());
            actionLogService.record("DELETE", "ROOM", roomNo, room.getRoomName());
        }
    }

    private RoomListItem toListItem(Room room) {
        return new RoomListItem(
                room.getRoomNo(),
                room.getRoomName(),
                room.getRoomCode(),
                ownerLabel(room.getOwnerUserNo()),
                blankToDash(room.getLocation()),
                normalizeStatus(String.valueOf(room.getStatus())),
                formatDateTime(room.getRoomCreated())
        );
    }

    private String normalizeStatus(String status) {
        if (STATUS_INVITING.equals(status)
                || STATUS_BUDGET_INPUT.equals(status)
                || STATUS_BUDGET_DONE.equals(status)
                || STATUS_ACTIVE.equals(status)
                || STATUS_ENDED.equals(status)
                || STATUS_DELETED.equals(status)) {
            return status;
        }
        if (STATUS_ALL.equals(status)) {
            return STATUS_ALL;
        }
        return STATUS_ALL;
    }

    private String ownerLabel(Long ownerUserNo) {
        if (ownerUserNo == null) {
            return "-";
        }

        return userRepository.findById(ownerUserNo)
                .map(this::userLabel)
                .orElse("회원 #" + ownerUserNo);
    }

    private String userLabel(User user) {
        return "%s (#%d)".formatted(user.getUserName(), user.getUserNo());
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

    private String money(Integer value) {
        if (value == null) {
            return "-";
        }
        return money(value.longValue());
    }

    private String money(long value) {
        return MONEY_FORMATTER.format(value);
    }
}
