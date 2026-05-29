package com.beggar.api.controller;

import com.beggar.api.dto.receipt.ReceiptCreateRequest;
import com.beggar.api.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
public class ReceiptController {
    private final ReceiptService receiptService;

//     TODO: POST  /receipts              — 영수증 등록 (S3 URL + 메타)
//     TODO: PATCH /receipts/{receiptId}  — 금액 수동 보정
//     TODO: GET   /receipts?roomNo=...   — 방별 영수증 목록 (최신순)

    @PostMapping
    public Long create(@RequestBody ReceiptCreateRequest request) {
        return receiptService.create(request);
    }

    @GetMapping
    public List<ReceiptCreateRequest> read() { // Response 대신 Request 사용
        return receiptService.read();
    }

    @GetMapping("/{receiptId}")
    public ReceiptCreateRequest readone(@PathVariable Long receiptId) {
        return receiptService.readone(receiptId);
    }

    @PutMapping("/{receiptId}/ocr")
    public boolean update(@PathVariable Long receiptId, @RequestBody ReceiptCreateRequest request) {
        return receiptService.update(receiptId, request);
    }

    @DeleteMapping
    public boolean delete(@RequestParam Long receiptId) {
        return receiptService.delete(receiptId);
    }
}