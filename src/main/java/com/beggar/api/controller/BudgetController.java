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

    /* [GET] /rooms/{roomNo}/budget — 본인이 제출한 예산 조회 */
    @GetMapping
    public ResponseEntity<ApiResponse<Integer>> getMyBudget(
            @PathVariable Long roomNo,
            @LoginUser Long loginUserNo) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.findMyBudget(roomNo, loginUserNo)));
    }

    /* [POST] /rooms/{roomNo}/budget — 본인 예산 익명 제출 및 수정 */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitBudget(
            @PathVariable Long roomNo,
            @LoginUser Long loginUserNo,
            @Valid @RequestBody SubmitBudgetRequest request) {
        budgetService.submitBudget(loginUserNo, roomNo, request.budgetAmount());

        return ResponseEntity.ok(ApiResponse.success(null));  // 성공 시 200 반환
    }

    /* [POST] /rooms/{roomNo}/budget/confirm — 예산 확정 (최저값 × 인원) */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmBudget(@PathVariable Long roomNo) {

        budgetService.confirmBudget(roomNo);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /* [GET] /rooms/{roomNo}/budget/result — 확정된 예산 결과 조회 */
    @GetMapping("/result")
    public ResponseEntity<ApiResponse<BudgetResultResponse>> getResult(@PathVariable Long roomNo) {
        BudgetResultResponse response = budgetService.getResult(roomNo);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /* [GET] /rooms/{roomNo}/budget/excel — 확정된 예산 결과 엑셀 다운로드 */
    @GetMapping("/excel")
    public void downloadBudgetExcel(
            @PathVariable Long roomNo,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        String fileName = java.net.URLEncoder.encode("거지방_최종예산정산서", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xlsx");

        budgetService.exportBudgetToExcel(roomNo, response.getOutputStream());
    }
}
