package com.beggar.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BeggarScoreService {

    // 방별 거지력 점수 산식 (0~100):
    //   score = 예산준수율 × 0.35 + 절약률 × 0.35 + 착한가격업소 인증 점수 × 0.30
    //
    // 칭호 5단계: 아기 거지(0-19) / 성장하는 거지(20-39) / 알뜰한 거지(40-59)
    //              / 프로 거지(60-79) / 전설의 거지(80-100)
    //
    // TODO: getRoomScore(roomNo)      — 거지방 내부의 거지평가 조회
    // TODO: getRoomRanking(limit)     — room_beggar_scores 기준 방 점수 DESC 후보
    // TODO: recalculate(roomNo)       — 해당 방 점수만 UPSERT (동기, 동일 트랜잭션)
    //                                   트리거: 영수증 INSERT/UPDATE, 예산 확정, 멤버 상태 변경
}
