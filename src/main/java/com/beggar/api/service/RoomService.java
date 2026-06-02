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

    private  final RoomPurposeTagRepository roomPurposeTagRepository;

    /* 방 생성(create) */
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest request,Long userNo){
        String roomCode = generateRandomCode(12);
        System.out.println("생성된 12자리 초대 코드:" + roomCode);

        // 1. 방 기본 정보 저장
        Room room = new Room(
                request.getRoomName(),
                roomCode,
                userNo,
                request.getIsFriends()
        );
        Room savedRoom = roomRepository.save(room);

        // 2. 태그 일괄 저장
        List<String> tagNames = request.getTags();
        if (tagNames != null){
            for(String tagName : tagNames){
                RoomPurposeTag tag = new RoomPurposeTag(savedRoom,tagName);
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
                savedRoom.getRoomCreated(),
                tagNames
        );
    }

    private String generateRandomCode(int length){
        String codeList = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++){
            int index = (int) (codeList.length() * Math.random());
            sb.append(codeList.charAt(index));
        }
        return sb.toString();
    }


    // TODO: Room INSERT(maxMemberCount 포함) + 방장 RoomMember(ACTIVE) INSERT + tags 일괄 INSERT
    // TODO: create(ownerUserNo, request)  — 방 생성 + 방장 자동 입장 + 태그 INSERT + maxMemberCount 저장
    // TODO: findMyRooms(userNo)           — ACTIVE 멤버인 방 목록
    // TODO: findById(roomNo)              — 방 상세 + 태그
    // TODO: joinByCode(userNo, roomCode)  — 코드로 입장 (중복 입장 차단)
    // TODO: findMembers(roomNo)           — 입장 현황 (예산 제출 여부만, 금액 X)
    // TODO: updateSettings(roomNo, ownerUserNo, request) — 지역/태그/최대 인원 변경
}
