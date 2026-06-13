package com.beggar.api.dto.receipt;

import java.time.LocalDateTime;
import java.util.List;

public record SplitGroupResponse(
        Long splitGroupId,
        Long roomNo,
        String storeName,
        String address,
        String status,
        Integer totalAmount,
        int receiptCount,
        int contributorCount,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        List<Item> items
) {
    public record Item(
            Long receiptId,
            Long uploaderUserNo,
            String uploaderName,
            Integer amount,
            String imageUrl
    ) {
    }
}
