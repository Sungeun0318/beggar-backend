package com.beggar.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 데이터 조회 속도 최적화
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final RoomPurposeTagRepository roomPurposeTagRepository;


    /* 방 생성(create) */
    @Transactional // 데이터삽입.   + readOnly끄기위해 붙여줌
    public RoomResponse create(Long ownerUserNo , CreateRoomRequest request){
        // 방장 유저 존재 여부 검증
        User owner = userRepository.findById(ownerUserNo)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            

    }
    // TODO: create(ownerUserNo, request)  — 방 생성 + 방장 자동 입장 + 태그 INSERT
    // TODO: findMyRooms(userNo)           — ACTIVE 멤버인 방 목록
    // TODO: findById(roomNo)              — 방 상세 + 태그
    // TODO: joinByCode(userNo, roomCode)  — 코드로 입장 (중복 입장 차단)
    // TODO: findMembers(roomNo)           — 입장 현황 (예산 제출 여부만, 금액 X)
}