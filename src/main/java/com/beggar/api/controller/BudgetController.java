package com.beggar.api.controller;

import com.beggar.api.common.response.ApiResponse;
import com.beggar.api.dto.budget.BudgetResultResponse;
import com.beggar.api.dto.budget.SubmitBudgetRequest;
import com.beggar.api.security.LoginUser;
import com.beggar.api.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rooms/{roomNo}/budget")
public class BudgetController {

    private final BudgetService budgetService;

    // 생성자 주입
    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /* 💰 0. [GET] /rooms/{roomNo}/budget — 본인이 제출한 예산 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<Integer>> getMyBudget(
            @PathVariable Long roomNo,
            @LoginUser Long loginUserNo) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.findMyBudget(roomNo, loginUserNo)));
    }

    /* 💸 1. [POST] /rooms/{roomNo}/budget — 본인 예산 익명 제출 및 수정 */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitBudget(
            @PathVariable Long roomNo,
            @LoginUser Long loginUserNo,
            @Valid @RequestBody SubmitBudgetRequest request) { // 🌟 소영님의 SubmitBudgetRequest 레코드 매핑!
        // 레코드 문법에 맞춰 request.budgetAmount()로 금액을 꺼내옵니다.
        budgetService.submitBudget(loginUserNo, roomNo, request.budgetAmount());

        return ResponseEntity.ok(ApiResponse.success(null));  // 성공 시 200 반환
    }

    /* 🔒 2. [POST] /rooms/{roomNo}/budget/confirm — 예산 확정 (최저값 × 인원) */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmBudget(@PathVariable Long roomNo) {

        budgetService.confirmBudget(roomNo);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /* 📊 3. [GET] /rooms/{roomNo}/budget/result — 확정된 예산 결과 조회 */
    @GetMapping("/result")
    public ResponseEntity<ApiResponse<BudgetResultResponse>> getResult(@PathVariable Long roomNo) {

        // 🌟 서비스가 이제 진짜 BudgetResultResponse를 주니까 형변환 없이 바로 받으면 끝!
        BudgetResultResponse response = budgetService.getResult(roomNo);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
