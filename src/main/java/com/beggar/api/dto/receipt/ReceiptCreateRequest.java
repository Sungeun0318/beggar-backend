package com.beggar.api.dto.receipt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReceiptCreateRequest(
        @NotNull Long roomNo,
        @NotBlank String imageUrl,
        @Min(0) Integer amount       // 수동 입력값 (OCR 전 가입력)
) {}
