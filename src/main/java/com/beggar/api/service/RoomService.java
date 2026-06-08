package com.beggar.api.service;

import com.beggar.api.dto.room.RoomCreateRequest;
import com.beggar.api.dto.room.RoomEventDto;
import com.beggar.api.dto.room.RoomMemberResponse;
import com.beggar.api.dto.room.RoomResponse;
import com.beggar.api.entity.*;
import com.beggar.api.repository.BudgetRepository;
import com.beggar.api.repository.RoomMemberRepository;
import com.beggar.api.repository.RoomPurposeTagRepository;
import com.beggar.api.repository.RoomRepository;
import com.beggar.api.repository.UserRepository;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 데이터 조회 속도 최적화
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomPurposeTagRepository roomPurposeTagRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final RoomEventService roomEventService;

    /* 👑 방 생성(create) */
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request, Long userNo) {
        String roomCode = generateRandomCode();
        System.out.println("생성된 12자리 초대 코드:" + roomCode);

        // 1. 방 기본 정보 저장
        Room room = new Room(
                request.getRoomName(),
                roomCode,
                userNo,
                request.getIsFriends(),
                request.getLocation(),
                request.getMaxMemberCount()
        );
        Room savedRoom = roomRepository.save(room);
        User owner = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("임시 로그인 유저를 찾을 수 없습니다."));
        roomMemberRepository.save(RoomMember.builder()
                .room(savedRoom)
                .user(owner)
                .status(RoomMember.Status.ACTIVE)
                .build());

        // 2. 목적 태그 리스트 일괄 DB 저장
        List<String> tagNames = request.getTags();
        if (tagNames != null) {
            for (String tagName : tagNames) {
                RoomPurposeTag tag = new RoomPurposeTag(savedRoom, tagName);
                roomPurposeTagRepository.save(tag);
            }
        }

        return toResponse(savedRoom, tagNames, 1);
    }

    private RoomResponse toResponse(Room room, List<String> tagNames, long memberCount) {
        return new RoomResponse(
                room.getRoomNo(),
                room.getRoomName(),
                room.getRoomCode(),
                room.getOwnerUserNo(),
                room.getTotalBudget(),
                room.getIsFriends(),
                room.getLocation(),
                room.getStatus(),
                memberCount,
                room.getMaxMemberCount(),
                room.getRoomCreated(),
                tagNames
        );
    }

    private RoomResponse toResponse(Room room) {
        List<String> tagNames = roomPurposeTagRepository.findAllByRoom_RoomNo(room.getRoomNo()).stream()
                .map(RoomPurposeTag::getTag)
                .toList();
        long memberCount = roomMemberRepository.countByRoom_RoomNoAndStatus(room.getRoomNo(), RoomMember.Status.ACTIVE);

        return toResponse(room, tagNames, memberCount);
    }

    /* 12자리 고유 랜덤 초대 코드 생성기 */
    private String generateRandomCode() {
        String codeList = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 12; i++) {
            int index = (int) (codeList.length() * Math.random());
            sb.append(codeList.charAt(index));
        }
        return sb.toString();
    }

    /* 👑 내가 참여 중인 방 목록 조회 */
    public List<RoomResponse> findMyRooms(Long userNo) {
        return roomMemberRepository.findByUser_UserNoAndStatus(userNo, RoomMember.Status.ACTIVE).stream()
                .map(RoomMember::getRoom)
                .map(this::toResponse)
                .toList();
    }

    public RoomResponse findById(Long roomNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));
        List<String> tagNames = roomPurposeTagRepository.findAllByRoom_RoomNo(roomNo).stream()
                .map(RoomPurposeTag::getTag)
                .toList();
        long memberCount = roomMemberRepository.countByRoom_RoomNoAndStatus(roomNo, RoomMember.Status.ACTIVE);

        return toResponse(room, tagNames, memberCount);
    }

    @Transactional
    public RoomResponse joinByCode(Long userNo, String roomCode) {
        if (!StringUtils.hasText(roomCode)) {
            throw new IllegalArgumentException("초대 코드가 필요합니다.");
        }

        Room room = roomRepository.findByRoomCode(roomCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("올바르지 않은 초대 코드입니다."));
        
        // 방 상태가 INVITING 또는 BUDGET_INPUT일 때만 입장 허용 (기획 5.2 참고)
        if (room.getStatus() != RoomStatus.INVITING && room.getStatus() != RoomStatus.BUDGET_INPUT) {
            throw new IllegalArgumentException("현재는 입장이 불가능한 상태의 방입니다.");
        }

        User user = userRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("로그인 사용자를 찾을 수 없습니다."));

        RoomMember member = roomMemberRepository.findByRoom_RoomNoAndUser_UserNo(room.getRoomNo(), userNo)
                .orElse(null);

        if (member == null) {
            // 신규 입장 시 인원 제한 체크
            long activeCount = roomMemberRepository.countByRoom_RoomNoAndStatus(room.getRoomNo(), RoomMember.Status.ACTIVE);
            if (activeCount >= room.getMaxMemberCount()) {
                throw new IllegalArgumentException("정원이 초과되어 입장할 수 없습니다.");
            }

            roomMemberRepository.save(RoomMember.builder()
                    .room(room)
                    .user(user)
                    .status(RoomMember.Status.ACTIVE)
                    .build());
        } else {
            // 기존 멤버 상태에 따른 처리
            if (member.getStatus() == RoomMember.Status.KICKED) {
                throw new IllegalArgumentException("강퇴당한 방에는 다시 입장할 수 없습니다.");
            }
            if (member.getStatus() == RoomMember.Status.LEFT) {
                // 재입장 시에도 인원 제한 체크 필요할 수 있음
                long activeCount = roomMemberRepository.countByRoom_RoomNoAndStatus(room.getRoomNo(), RoomMember.Status.ACTIVE);
                if (activeCount >= room.getMaxMemberCount()) {
                    throw new IllegalArgumentException("정원이 초과되어 입장할 수 없습니다.");
                }
                member.rejoin();
            }
            // ACTIVE인 경우는 그대로 통과
        }

        // 멤버 갱신 이벤트 발행
        List<RoomMemberResponse> members = findMembers(room.getRoomNo(), null);
        roomEventService.publishMembersUpdated(room.getRoomNo(), members);

        return toResponse(room);
    }

    public List<RoomMemberResponse> findMembers(Long roomNo, Long loginUserNo) {
        if (!roomRepository.existsById(roomNo)) {
            throw new IllegalArgumentException("존재하지 않는 거지방입니다.");
        }

        return roomMemberRepository.findByRoom_RoomNoOrderByJoinedAtAsc(roomNo).stream()
                .filter(member -> member.getStatus() == RoomMember.Status.ACTIVE)
                .map(member -> RoomMemberResponse.from(
                        member,
                        loginUserNo,
                        budgetRepository.existsByRoomNoAndUserNo(roomNo, member.getUser().getUserNo())
                ))
                .toList();
    }

    @Transactional
    public void updateSettings(Long roomNo, Long ownerUserNo, RoomCreateRequest request) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));

        if (!room.getOwnerUserNo().equals(ownerUserNo)) {
            throw new IllegalArgumentException("방장만 설정을 변경할 수 있습니다.");
        }

        // 1. 최대 인원 제한 체크 (현재 참여 중인 인원보다 적게 설정 불가)
        long currentMemberCount = roomMemberRepository.countByRoom_RoomNoAndStatus(roomNo, RoomMember.Status.ACTIVE);
        if (request.getMaxMemberCount() < currentMemberCount) {
            throw new IllegalArgumentException("현재 참여 중인 인원(" + currentMemberCount + "명)보다 적게 최대 인원을 설정할 수 없습니다.");
        }

        // 2. 방 기본 정보 업데이트
        room.update(request.getRoomName(), request.getLocation(), request.getMaxMemberCount());

        // 3. 태그 업데이트 (기존 태그 삭제 후 재등록)
        roomPurposeTagRepository.deleteAllByRoom_RoomNo(roomNo);
        List<String> tagNames = request.getTags();
        if (tagNames != null) {
            for (String tagName : tagNames) {
                roomPurposeTagRepository.save(new RoomPurposeTag(room, tagName));
            }
        }
    }

    @Transactional
    public void startBudget(Long roomNo, Long loginUserNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));

        if (!room.getOwnerUserNo().equals(loginUserNo)) {
            throw new IllegalArgumentException("방장만 예산 입력을 시작할 수 있습니다.");
        }

        if (room.getStatus() != RoomStatus.INVITING) {
            throw new IllegalArgumentException("이미 예산 입력이 시작되었거나 입장이 불가능한 상태입니다.");
        }

        // 상태 변경
        room.startBudgetInput();

        // 이벤트 발행
        roomEventService.publishStateChanged(roomNo, RoomEventDto.EventType.BUDGET_INPUT_STARTED, "/budget/input?roomNo=" + roomNo);
    }
}
