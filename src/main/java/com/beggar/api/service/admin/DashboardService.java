package com.beggar.api.service.admin;

import com.beggar.api.dto.admin.DashboardListItem;
import com.beggar.api.dto.admin.DashboardStats;
import com.beggar.api.entity.RoomStatus;
import com.beggar.api.repository.UserRepository;
import com.beggar.api.repository.RoomRepository;
import com.beggar.api.repository.RoomFreePostRepository;
import com.beggar.api.repository.RoomFreeCommentRepository;
import com.beggar.api.repository.RoomFreeChatRepository;
import com.beggar.api.repository.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DashboardService {

    private static final List<RoomStatus> IN_PROGRESS_ROOM_STATUSES =
            List.of(RoomStatus.INVITING, RoomStatus.BUDGET_INPUT, RoomStatus.BUDGET_DONE, RoomStatus.ACTIVE);

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomFreePostRepository postRepository;
    private final RoomFreeCommentRepository commentRepository;
    private final RoomFreeChatRepository chatRepository;
    private final ReceiptRepository receiptRepository;

    public DashboardService(
            UserRepository userRepository,
            RoomRepository roomRepository,
            RoomFreePostRepository postRepository,
            RoomFreeCommentRepository commentRepository,
            RoomFreeChatRepository chatRepository,
            ReceiptRepository receiptRepository
    ) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.chatRepository = chatRepository;
        this.receiptRepository = receiptRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        long totalRooms = roomRepository.count();

        return new DashboardStats(
                userRepository.count(),
                totalRooms,
                roomRepository.countByStatusIn(IN_PROGRESS_ROOM_STATUSES),
                roomRepository.countByStatus(RoomStatus.ENDED),
                roomRepository.countByStatus(RoomStatus.DELETED),
                postRepository.count(),
                commentRepository.count(),
                chatRepository.count(),
                receiptRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public List<DashboardListItem> getRecentUsers() {
        return userRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(user -> new DashboardListItem(
                        user.getUserName(),
                        user.getEmail(),
                        formatDateTime(user.getCreatedAt())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardListItem> getRecentRooms() {
        return roomRepository.findTop5ByOrderByRoomCreatedDesc().stream()
                .map(room -> new DashboardListItem(
                        room.getRoomName(),
                        room.getLocation() == null ? "지역 미설정" : room.getLocation(),
                        formatDateTime(room.getRoomCreated())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DashboardListItem> getRecentPosts() {
        return postRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(post -> new DashboardListItem(
                        post.getTitle(),
                        post.getTag() == null ? "태그 없음" : post.getTag(),
                        formatDateTime(post.getCreatedAt())
                ))
                .toList();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }

        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
