package com.beggar.api.service;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
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

    /* 방 생성(create) */
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request, Long userNo) {
        // 방 이름 중복 체크
        if (roomRepository.existsByRoomName(request.getRoomName())) {
            throw new CustomException(ErrorCode.DUPLICATE_ROOM_NAME);
        }

        String roomCode = generateRandomCode();
        System.out.println("생성된 12자리 초대 코드:" + roomCode);

        // 방 기본 정보 저장 (DRAFT 상태로 시작)
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

        // 목적 태그 리스트 일괄 DB 저장
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

    /* 내가 참여 중인 방 목록 조회 */
    public List<RoomResponse> findMyRooms(Long userNo) {
        return roomMemberRepository.findByUser_UserNoAndStatusAndIsHiddenFalse(userNo, RoomMember.Status.ACTIVE).stream()
                .map(RoomMember::getRoom)
                .filter(room -> room.getStatus() != RoomStatus.DELETED) // 삭제된 방 제외
                .map(this::toResponse)
                .toList();
    }

    /* 방 검색 기능 (이름, 지역, 코드 기반) */
    public List<RoomResponse> searchRooms(String keyword) {
        // Pageable.unpaged()를 사용하여 페이징 없이 모든 결과를 가져옵니다.
        // Repository의 searchRooms 쿼리는 keyword가 비어있으면 전체를 조회하도록 설계되어 있습니다.
        return roomRepository.searchRooms(keyword, "ALL", org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void hideRoom(Long roomNo, Long userNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));

        RoomMember member = roomMemberRepository.findByRoom_RoomNoAndUser_UserNo(roomNo, userNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 방의 참여자가 아닙니다."));

        boolean isOwner = room.getOwnerUserNo().equals(userNo);

        if (isOwner) {
            // 방장은 진행 중(ACTIVE)인 방만 아니면 삭제(방 전체 삭제) 가능
            if (room.getStatus() == RoomStatus.ACTIVE) {
                throw new IllegalArgumentException("진행 중인 방은 삭제할 수 없습니다. 종료 후 삭제해 주세요.");
            }
            // 방장이 삭제하면 방 상태 자체를 DELETED로 변경하여 모두에게서 숨김
            room.markDeleted(java.time.LocalDateTime.now());
        } else {
            // 방장이 아닌 멤버는 종료된(ENDED) 방만 목록에서 숨길 수 있음
            if (room.getStatus() != RoomStatus.ENDED) {
                throw new IllegalArgumentException("종료된 방만 목록에서 삭제할 수 있습니다.");
            }
        }

        member.hide();
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
        
        // 방 상태가 DRAFT, INVITING 또는 BUDGET_INPUT일 때만 입장 허용
        if (room.getStatus() != RoomStatus.DRAFT && room.getStatus() != RoomStatus.INVITING && room.getStatus() != RoomStatus.BUDGET_INPUT) {
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
        System.out.println("DEBUG: [" + room.getRoomNo() + "] 멤버 갱신 방송 송출 - 현재 인원: " + members.size());

        // [변경] 인원이 찼을 때 자동으로 넘기지 않고, 이미 상태가 BUDGET_INPUT인 경우에만 경로 안내
        if (room.getStatus() == RoomStatus.BUDGET_INPUT) {
            roomEventService.publishStateChanged(
                    room.getRoomNo(),
                    RoomEventDto.EventType.BUDGET_INPUT_STARTED,
                    "/budget/input?roomNo=" + room.getRoomNo()
            );
            System.out.println("DEBUG: [" + room.getRoomNo() + "] 이미 BUDGET_INPUT 단계인 방 - 상태 동기화 방송 송출");
        }

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

        if (room.getStatus() == RoomStatus.ENDED) {
            throw new IllegalArgumentException("이미 종료된 방의 설정은 변경할 수 없습니다.");
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

        // 1. 최소 인원 체크 (방장 포함 최소 2명은 있어야 함)
        long activeMemberCount = roomMemberRepository.countByRoom_RoomNoAndStatus(roomNo, RoomMember.Status.ACTIVE);
        if (activeMemberCount < 2) {
            throw new IllegalArgumentException("최소 2명 이상의 멤버가 모여야 예산 입력을 시작할 수 있습니다. 친구를 초대해 주세요!");
        }

        // 2. 상태 체크 (DRAFT 또는 INVITING 상태에서만 시작 가능)
        if (room.getStatus() != RoomStatus.DRAFT && room.getStatus() != RoomStatus.INVITING) {
            throw new IllegalArgumentException("이미 예산 입력이 시작되었거나 입장이 불가능한 상태입니다.");
        }

        // 상태 변경
        room.startBudgetInput();

        // [중요] 모든 멤버에게 "우리 방 번호(roomNo)를 가지고 예산 입력창으로 이동해!"라고 알림
        roomEventService.publishStateChanged(roomNo, RoomEventDto.EventType.BUDGET_INPUT_STARTED, "/budget/input?roomNo=" + roomNo);
    }

    @Transactional
    public void closeRoom(Long roomNo, Long loginUserNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));

        if (!room.getOwnerUserNo().equals(loginUserNo)) {
            throw new IllegalArgumentException("방장만 방을 종료할 수 있습니다.");
        }

        if (room.getStatus() == RoomStatus.ENDED) {
            throw new IllegalArgumentException("이미 종료된 방입니다.");
        }

        room.close();

        // 방 종료 이벤트 발행 (필요하다면 EventType 추가 가능, 현재는 상태 변경만 발행)
        roomEventService.publishStateChanged(roomNo, RoomEventDto.EventType.ROOM_ENDED, null);
    }
}
