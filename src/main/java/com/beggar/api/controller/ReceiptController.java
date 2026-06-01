package com.beggar.api.controller;

import com.beggar.api.dto.receipt.ReceiptCreateRequest;
import com.beggar.api.dto.receipt.ReceiptResponse;
import com.beggar.api.dto.receipt.ReceiptUpdateRequest;
import com.beggar.api.service.ReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms/{roomNo}/receipts")

@RequiredArgsConstructor
public class ReceiptController {
    private final ReceiptService receiptService;

    // TODO: @LoginUser가 연결되면 request.uploaderUserNo 대신 로그인 사용자 번호를 사용한다.

    @PostMapping
    public ReceiptResponse create(@PathVariable Long roomNo, @RequestBody ReceiptCreateRequest request) {
        return receiptService.create(roomNo, request);
    }

    @GetMapping
    public List<ReceiptResponse> read(@PathVariable Long roomNo) {
        return receiptService.read(roomNo);
    }

    @GetMapping("/{receiptId}")
    public ReceiptResponse readOne(@PathVariable Long roomNo, @PathVariable Long receiptId) {
        return receiptService.readOne(roomNo, receiptId);
    }

    @PatchMapping("/{receiptId}")
    public ReceiptResponse updateAmount(@PathVariable Long roomNo,
                                        @PathVariable Long receiptId,
                                        @RequestBody ReceiptUpdateRequest request) {
        return receiptService.updateAmount(roomNo, receiptId, request);
    }

    @PutMapping("/{receiptId}/ocr")
    public ReceiptResponse applyOcrResult(@PathVariable Long roomNo,
                                          @PathVariable Long receiptId,
                                          @RequestBody ReceiptCreateRequest request) {
        return receiptService.applyOcrResult(roomNo, receiptId, request);
    }

    @DeleteMapping("/{receiptId}")
    public void delete(@PathVariable Long roomNo, @PathVariable Long receiptId) {
        receiptService.delete(roomNo, receiptId);
    }
}
