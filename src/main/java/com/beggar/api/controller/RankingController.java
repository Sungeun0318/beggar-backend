package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.ranking.RankingEntryResponse;
import com.beggar.api.service.BeggarScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final BeggarScoreService beggarScoreService;

    // room_beggar_scores 기준 방 점수 DESC 순위 후보
    // 방 내부 거지평가와 전역 랭킹은 서로 독립된 화면/정책으로 유지한다.
    @GetMapping
    public ApiResponse<List<RankingEntryResponse>> getRanking(
            @RequestParam(defaultValue = "15") int limit) {
        return ApiResponse.success(beggarScoreService.getRoomRanking(limit));
    }
}
