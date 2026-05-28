package com.beggar.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BudgetService {

    // TODO: submit(userNo, roomNo, amount) — 본인 예산 제출 (INSERT or UPDATE)
    //                                        모든 ACTIVE 멤버 제출 시 자동 확정
    // TODO: confirm(roomNo)                — 강제 확정 (MIN × COUNT, 같은 트랜잭션 내
    //                                        rooms.total_budget 동기화 + 거지력 재계산)
    // TODO: getResult(roomNo)              — 확정 결과 조회
}
