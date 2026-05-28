package com.beggar.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReceiptService {

    // TODO: create(userNo, request)                — 영수증 등록 (ocrStatus = PENDING)
    //                                                동일 트랜잭션 내 거지력 재계산
    // TODO: updateAmount(userNo, receiptId, req)   — 수동 금액 보정 → 거지력 재계산
    // TODO: listByRoom(roomNo)                     — 방별 영수증 (최신순)
    // TODO: applyOcrResult(receiptId, payload)     — OCR 콜백 (AI 서버 → 결과 반영)
}
