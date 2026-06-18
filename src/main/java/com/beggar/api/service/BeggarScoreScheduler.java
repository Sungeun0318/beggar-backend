package com.beggar.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 방 거지력 점수 일일 재계산 배치.
 *
 * - 랭킹 화면은 저장된 점수만 읽고(가벼움), 실제 재계산은 여기서 하루 1회만 한다.
 * - cron 의 zone 을 Asia/Seoul 로 지정해 서버 타임존(UTC 등)과 무관하게
 *   "한국시간 매일 0시 0분 0초"에 정확히 실행된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BeggarScoreScheduler {

    private final BeggarScoreService beggarScoreService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void refreshDailyRanking() {
        log.info("[ranking] 일일 점수 재계산 시작 (한국시간 0시)");
        int count = beggarScoreService.recalculateAllScores();
        log.info("[ranking] 일일 점수 재계산 완료: {}개 방", count);
    }
}
