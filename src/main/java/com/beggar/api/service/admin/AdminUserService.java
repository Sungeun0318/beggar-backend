package com.beggar.api.service.admin;

import com.beggar.api.dto.admin.UserDetail;
import com.beggar.api.entity.User;
import com.beggar.api.repository.UserRepository;
import com.beggar.api.repository.RoomRepository;
import com.beggar.api.repository.RoomMemberRepository;
import com.beggar.api.repository.RoomFreePostRepository;
import com.beggar.api.repository.RoomFreeCommentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class AdminUserService {

    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomFreePostRepository postRepository;
    private final RoomFreeCommentRepository commentRepository;

    public AdminUserService(
            UserRepository userRepository,
            RoomRepository roomRepository,
            RoomMemberRepository roomMemberRepository,
            RoomFreePostRepository postRepository,
            RoomFreeCommentRepository commentRepository
    ) {
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional(readOnly = true)
    public Page<User> getUsers(String keyword, int page) {
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(
                safePage,
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            return userRepository.findAll(pageable);
        }

        return userRepository.findByUserNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                trimmed,
                trimmed,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public UserDetail getUserDetail(Long userNo) {
        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없어."));
        return new UserDetail(
                user.getUserNo(),
                user.getUserName(),
                user.getEmail(),
                user.getRole(),
                genderLabel(user.getGender()),
                blankToDash(user.getAgeRange()),
                formatDateTime(user.getCreatedAt()),
                roomRepository.countByOwnerUserNo(userNo),
                roomMemberRepository.countByUser_UserNo(userNo),
                postRepository.countByAuthor_UserNo(userNo),
                commentRepository.countByAuthor_UserNo(userNo)
        );
    }

    private String genderLabel(Integer gender) {
        if (gender == null) {
            return "-";
        }
        if (gender == 1) {
            return "남성";
        }
        if (gender == 2) {
            return "여성";
        }
        return "기타";
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
