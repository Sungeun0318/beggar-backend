package com.beggar.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ranking")
public class RankingController {

    // TODO: GET /ranking?limit=15 — room_beggar_scores 기준 방 점수 DESC 순위 후보
    // 방 내부 거지평가와 전역 랭킹은 서로 독립된 화면/정책으로 유지한다.
}
