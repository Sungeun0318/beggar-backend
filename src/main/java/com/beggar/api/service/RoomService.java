package com.beggar.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoomService {

    // TODO: create(ownerUserNo, request)  — 방 생성 + 방장 자동 입장 + 태그 INSERT
    // TODO: findMyRooms(userNo)           — ACTIVE 멤버인 방 목록
    // TODO: findById(roomNo)              — 방 상세 + 태그
    // TODO: joinByCode(userNo, roomCode)  — 코드로 입장 (중복 입장 차단)
    // TODO: findMembers(roomNo)           — 입장 현황 (예산 제출 여부만, 금액 X)
}
