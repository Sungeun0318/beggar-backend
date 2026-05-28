package com.beggar.api.dto.recommendation;

import java.util.List;

public record RecommendationResponse(
        Long roomNo,
        Integer totalBudget,
        List<Place> places
) {
    public record Place(
            String name,
            String category,         // 식사 / 카페 / 놀거리 등
            Integer expectedPrice,
            String walkTime,         // "도보 5분"
            Double rating,
            String thumbnailUrl,
            String address,
            Double lat,
            Double lng
    ) {}
}
