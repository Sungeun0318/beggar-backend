package com.beggar.api.dto.receipt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record ReceiptUpdateRequest(
        @NotNull @Min(0) Integer amount,       // 사용자 수동 보정 금액
        String storeName,                      // 사용자 수동 보정 가게명
        String address,                        // 주소
        BigDecimal centerLat,                  // 위도
        BigDecimal centerLng,                  // 경도
        List<ReceiptCreateRequest.SplitItem> splits // 분할 내역 추가
) {}
