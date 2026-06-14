package com.beggar.api.controller;

import com.beggar.api.dto.receipt.ReceiptCreateRequest;
import com.beggar.api.dto.receipt.ReceiptDuplicateCheckRequest;
import com.beggar.api.dto.receipt.ReceiptDuplicateCheckResponse;
import com.beggar.api.dto.receipt.ReceiptResponse;
import com.beggar.api.dto.receipt.ReceiptUpdateRequest;
import com.beggar.api.dto.receipt.SplitGroupCreateRequest;
import com.beggar.api.dto.receipt.SplitGroupResponse;
import com.beggar.api.entity.ReceiptSplitGroup;
import com.beggar.api.security.LoginUser;
import com.beggar.api.service.ReceiptDuplicateService;
import com.beggar.api.service.ReceiptSplitGroupService;
import com.beggar.api.service.ReceiptService;
import com.beggar.api.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms/{roomNo}/receipts")

@RequiredArgsConstructor
public class ReceiptController {
    private final ReceiptService receiptService;
    private final ReceiptDuplicateService receiptDuplicateService;
    private final ReceiptSplitGroupService receiptSplitGroupService;
    private final S3Service s3Service;

    // TODO: @LoginUser가 연결되면 request.uploaderUserNo 대신 로그인 사용자 번호를 사용한다.

    @PostMapping("/upload-url")
    public String getUploadUrl(@PathVariable Long roomNo, @RequestParam String fileName) {
        return s3Service.generatePresignedUrl(fileName);
    }

    @PostMapping
    public ReceiptResponse create(@PathVariable Long roomNo, 
                                 @LoginUser Long userNo,
                                 @RequestBody ReceiptCreateRequest request) {
        return receiptService.create(roomNo, userNo, request);
    }

    @PostMapping("/duplicate-check")
    public ReceiptDuplicateCheckResponse checkDuplicate(@PathVariable Long roomNo,
                                                        @LoginUser Long userNo,
                                                        @RequestBody ReceiptDuplicateCheckRequest request) {
        return receiptDuplicateService.check(roomNo, userNo, request);
    }

    @PostMapping("/split-groups")
    public SplitGroupResponse createSplitGroup(@PathVariable Long roomNo,
                                               @LoginUser Long userNo,
                                               @RequestBody SplitGroupCreateRequest request) {
        return receiptSplitGroupService.create(roomNo, userNo, request);
    }

    @GetMapping("/split-groups")
    public List<SplitGroupResponse> listSplitGroups(
            @PathVariable Long roomNo,
            @RequestParam(required = false) ReceiptSplitGroup.SplitGroupStatus status
    ) {
        return receiptSplitGroupService.list(roomNo, status);
    }

    @GetMapping("/split-groups/{groupId}")
    public SplitGroupResponse getSplitGroup(@PathVariable Long roomNo,
                                            @PathVariable Long groupId) {
        return receiptSplitGroupService.get(roomNo, groupId);
    }

    @PostMapping("/split-groups/{groupId}/close")
    public SplitGroupResponse closeSplitGroup(@PathVariable Long roomNo,
                                              @LoginUser Long userNo,
                                              @PathVariable Long groupId) {
        return receiptSplitGroupService.close(roomNo, userNo, groupId);
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
    public void delete(@PathVariable Long roomNo,
                       @LoginUser Long userNo,
                       @PathVariable Long receiptId) {
        receiptService.delete(roomNo, userNo, receiptId);
    }
}
