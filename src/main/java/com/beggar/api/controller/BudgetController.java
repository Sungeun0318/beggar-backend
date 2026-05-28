package com.beggar.api.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms/{roomNo}/budget")
public class BudgetController {

    // TODO: POST /rooms/{roomNo}/budget          — 본인 예산 익명 제출
    // TODO: POST /rooms/{roomNo}/budget/confirm  — 예산 확정 (최저값 × 인원)
    // TODO: GET  /rooms/{roomNo}/budget/result   — 확정된 예산 결과 조회
}
