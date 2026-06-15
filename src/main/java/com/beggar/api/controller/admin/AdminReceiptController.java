package com.beggar.api.controller.admin;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.admin.ReceiptDetail;
import com.beggar.api.dto.admin.ReceiptListItem;
import com.beggar.api.service.admin.AdminReceiptService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AdminReceiptController {

    private final AdminReceiptService adminReceiptService;

    public AdminReceiptController(AdminReceiptService adminReceiptService) {
        this.adminReceiptService = adminReceiptService;
    }

    @GetMapping("/admin/receipts")
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long roomNo,
            @RequestParam(required = false) Long roomMemberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page
    ) {
        Page<ReceiptListItem> receipts = adminReceiptService.getReceipts(
                keyword,
                roomNo,
                roomMemberId,
                fromDate,
                toDate,
                page
        );

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "영수증 관리");
        data.put("pageDescription", "방별 지출 영수증을 검색하고 상세 정보를 확인하세요.");
        data.put("activeMenu", "receipts");
        data.put("receipts", receipts);
        data.put("keyword", keyword);
        data.put("roomNo", roomNo);
        data.put("roomMemberId", roomMemberId);
        data.put("fromDate", fromDate);
        data.put("toDate", toDate);

        return ApiResponse.success(data);
    }

    @GetMapping("/admin/receipts/{receiptId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long receiptId) {
        ReceiptDetail receipt = adminReceiptService.getReceiptDetail(receiptId);

        Map<String, Object> data = new HashMap<>();
        data.put("pageTitle", "영수증 상세");
        data.put("pageDescription", "OCR, 착한가격업소 매칭, 지출 금액을 확인하세요.");
        data.put("activeMenu", "receipts");
        data.put("receipt", receipt);

        return ApiResponse.success(data);
    }

    @PostMapping("/admin/receipts/delete")
    public ApiResponse<String> delete(@RequestParam Long receiptId) {
        adminReceiptService.deleteReceipt(receiptId);
        return ApiResponse.success("영수증을 삭제했습니다.");
    }
}
