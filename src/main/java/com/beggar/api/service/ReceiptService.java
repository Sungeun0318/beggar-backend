package com.beggar.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReceiptService {

    // TODO: create(roomNo, userNo, request)        — 통합/분할 영수증 등록
    //                                                CAMERA/GALLERY는 OCR PENDING, MANUAL은 OCR MANUAL
    //                                                SPLIT이면 receipt_splits까지 저장
    //                                                동일 트랜잭션 내 room_beggar_scores 재계산
    // TODO: updateAmount(roomNo, userNo, receiptId, req) — 수동 금액 보정 → 방 점수 재계산
    // TODO: listByRoom(roomNo)                     — 방별 영수증 (최신순)
    // TODO: applyOcrResult(receiptId, payload)     — OCR 콜백 + 착한가격업소 매칭 결과 반영
}
