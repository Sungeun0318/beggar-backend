package com.beggar.api.service;

import com.beggar.api.dto.room.RoomCreateRequest;
import com.beggar.api.dto.room.RoomResponse;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomPurposeTag;
import com.beggar.api.repository.RoomPurposeTagRepository;
import com.beggar.api.repository.RoomRepository;
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

        // 2. 목적 태그 리스트 일괄 DB 저장
        List<String> tagNames = request.getTags();
        if (tagNames != null) {
            for (String tagName : tagNames) {
                RoomPurposeTag tag = new RoomPurposeTag(savedRoom, tagName);
                roomPurposeTagRepository.save(tag);
            }
        }

        return new RoomResponse(
                savedRoom.getRoomNo(),
                savedRoom.getRoomName(),
                savedRoom.getRoomCode(),
                savedRoom.getOwnerUserNo(),
                savedRoom.getTotalBudget(),
                savedRoom.getIsFriends(),
                savedRoom.getLocation(),
                savedRoom.getMaxMemberCount(),
                savedRoom.getRoomCreated(),
                tagNames
        );
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

    // TODO: findMyRooms(userNo) — ACTIVE 멤버인 방 목록
    public List<RoomResponse> findMyRooms(Long userNo) {
        return null;
    }

    // TODO: findById(roomNo) — 방 상세 + 태그
    public RoomResponse findById(Long roomNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));
        return null;
    }

    // TODO: joinByCode(userNo, roomCode) — 코드로 입장
    @Transactional
    public void joinByCode(Long userNo, String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("올바르지 않은 초대 코드입니다."));
    }

    // TODO: findMembers(roomNo) — 입장 현황
    public List<Object> findMembers(Long roomNo) {
        return null;
    }

    // TODO: updateSettings(roomNo, ownerUserNo, request) — 지역/태그/최대 인원 변경
    @Transactional
    public void updateSettings(Long roomNo, Long ownerUserNo, RoomCreateRequest request) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));

        if (!room.getOwnerUserNo().equals(ownerUserNo)) {
            throw new IllegalArgumentException("방장만 설정을 변경할 수 있습니다.");
        }
    }
}





    // TODO: Room INSERT(maxMemberCount 포함) + 방장 RoomMember(ACTIVE) INSERT + tags 일괄 INSERT
    // TODO: create(ownerUserNo, request)  — 방 생성 + 방장 자동 입장 + 태그 INSERT + maxMemberCount 저장
    // TODO: findMyRooms(userNo)           — ACTIVE 멤버인 방 목록
    // TODO: findById(roomNo)              — 방 상세 + 태그
    // TODO: joinByCode(userNo, roomCode)  — 코드로 입장 (중복 입장 차단)
    // TODO: findMembers(roomNo)           — 입장 현황 (예산 제출 여부만, 금액 X)
    // TODO: updateSettings(roomNo, ownerUserNo, request) — 지역/태그/최대 인원 변경
