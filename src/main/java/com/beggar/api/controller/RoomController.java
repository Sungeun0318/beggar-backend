package com.beggar.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    // TODO: GET  /rooms/my              — 내가 참여 중인 방 목록
    // TODO: POST /rooms                 — 방 생성 (tags 동시 등록)
    // TODO: GET  /rooms/{roomNo}        — 방 상세
    // TODO: POST /rooms/join            — 초대 코드로 입장
    // TODO: GET  /rooms/{roomNo}/members — 입장 현황 (금액 미노출)
}
