package com.beggar.api.dto.recommendation;

public record RecommendationInteractionRequest(
        String storeId,
        String storeName,
        String action,
        String requestedTag,
        String requestedRegion,
        Integer rankPosition,
        Integer expectedPrice
) {
}
