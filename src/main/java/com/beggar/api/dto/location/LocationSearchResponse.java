package com.beggar.api.dto.location;

public record LocationSearchResponse(
        String name,
        String address,
        Double lat,
        Double lng
) {
}
