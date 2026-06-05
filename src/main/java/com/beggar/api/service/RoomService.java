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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 데이터 조회 속도 최적화
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomPurposeTagRepository roomPurposeTagRepository;

    /* 1. 거지방 신규 생성 */
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request, Long userNo) {
        String roomCode = generateRandomCode();
        System.out.println("생성된 12자리 초대 코드:" + roomCode);

        Room room = new Room(
                request.getRoomName(),
                roomCode,
                userNo,
                request.getIsFriends(),
                request.getMaxMemberCount()
        );
        Room savedRoom = roomRepository.save(room);

        // 목적 태그 리스트 일괄 DB 저장
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
                savedRoom.getMaxMemberCount(),
                savedRoom.getRoomCreated(),
                tagNames
        );
    }

    /*  2. 내가 참여 중인 방 목록 조회 */
    public List<RoomResponse> findMyRooms(Long userNo) {
        // TODO: 로그인 기능 및 RoomMember 구현 시 완성할 공간
        return null;
    }

    /*  3. 방 상세 정보 및 태그 조회
    public RoomResponse findById(Long roomNo) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));
        List<String> tags = new ArrayList<>();
        try {
            tags = roomPurposeTagRepository.findByRoom_RoomNo(roomNo).stream()
                    .map(tag -> tag.getTag())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("⚠️ 태그 조회용 메서드명이 매칭되지 않았으나 방 정보를 위해 우회합니다.");
        }

        return new RoomResponse(
                room.getRoomNo(),
                room.getRoomName(),
                room.getRoomCode(),
                room.getOwnerUserNo(),
                room.getTotalBudget(),
                room.getIsFriends(),
                room.getMaxMemberCount(),
                room.getRoomCreated(),
                tags
        );
    }

    /*  4. 초대 코드로 신규 방 입장 */
    @Transactional
    public void joinByCode(Long userNo, String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("올바르지 않은 초대 코드입니다."));
    }

    /*  5. 입장 현황 조회 */
    public List<Object> findMembers(Long roomNo) {
        return null;
    }

    /*  6. 방장 전용 설정 변경 */
    @Transactional
    public void updateSettings(Long roomNo, Long ownerUserNo, RoomCreateRequest request) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거지방입니다."));

        if (!room.getOwnerUserNo().equals(ownerUserNo)) {
            throw new IllegalArgumentException("방장만 설정을 변경할 수 있습니다.");
        }
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
}