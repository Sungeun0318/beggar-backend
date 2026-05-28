package com.beggar.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/receipts")
public class ReceiptController {

    // TODO: POST  /receipts              — 영수증 등록 (S3 URL + 메타)
    // TODO: PATCH /receipts/{receiptId}  — 금액 수동 보정
    // TODO: GET   /receipts?roomNo=...   — 방별 영수증 목록 (최신순)
}
