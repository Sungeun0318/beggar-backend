package com.beggar.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms/{roomNo}/receipts")
public class ReceiptController {

    // TODO: POST  /rooms/{roomNo}/receipts              — 통합/분할 영수증 등록
    // TODO: PATCH /rooms/{roomNo}/receipts/{receiptId}  — 금액 수동 보정
    // TODO: GET   /rooms/{roomNo}/receipts              — 방별 영수증 목록 (최신순)
}
