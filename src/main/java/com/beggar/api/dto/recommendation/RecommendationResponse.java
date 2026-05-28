package com.beggar.api.dto.recommendation;

import java.util.List;

public record RecommendationResponse(
        Long roomNo,
        Integer totalBudget,
        List<Place> places
) {
    public record Place(
            String name,
            String category,         // 중식 / 한식 / 일식 / 양식 / 기타요식업
            Integer expectedPrice,
            String walkTime,         // "도보 5분"
            Double rating,
            String thumbnailUrl,
            String address,
            Double lat,
            Double lng
    ) {}
}
