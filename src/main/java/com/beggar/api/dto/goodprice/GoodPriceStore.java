package com.beggar.api.dto.goodprice;

import java.math.BigDecimal;

public record GoodPriceStore(
        String storeId,
        String name,
        String category,
        String address,
        Integer price,
        BigDecimal lat,
        BigDecimal lng,
        String phoneNumber,
        String itemName
) {
}
