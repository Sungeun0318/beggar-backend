package com.beggar.api.dto.receipt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReceiptUpdateRequest(
        @NotNull @Min(0) Integer amount       // 사용자 수동 보정
) {}
