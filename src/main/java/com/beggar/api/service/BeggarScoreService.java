package com.beggar.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BeggarScoreService {

    // 거지력 점수 산식 (0~100, 채택률 제외):
    //   score = 예산준수율 × 0.40 + 평균절약률 × 0.40 + 참여빈도 × 0.20
    //
    // 칭호 5단계: 아기 거지(0-19) / 성장하는 거지(20-39) / 알뜰한 거지(40-59)
    //              / 프로 거지(60-79) / 전설의 거지(80-100)
    //
    // TODO: getMyScore(userNo)        — 마이페이지 점수/칭호 조회
    // TODO: getRanking(limit)         — score DESC 상위 N명 (RankingEntryResponse)
    // TODO: recalculate(userNo)       — 단일 사용자 재계산 → UPSERT (동기, 동일 트랜잭션)
    //                                   트리거: 영수증 INSERT/UPDATE, 예산 확정, 멤버 상태 변경
}
