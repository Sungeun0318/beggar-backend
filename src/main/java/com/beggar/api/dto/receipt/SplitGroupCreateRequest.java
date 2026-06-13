package com.beggar.api.dto.receipt;

import java.math.BigDecimal;

public record SplitGroupCreateRequest(
        String storeName,
        String address,
        BigDecimal centerLat,
        BigDecimal centerLng
) {
}
