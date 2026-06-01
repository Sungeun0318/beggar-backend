package com.beggar.api.client.kakao;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.location.LocationSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class KakaoLocalClient {

    private static final String BASE_URL = "https://dapi.kakao.com";

    @Value("${kakao.rest-api-key}")
    private String restApiKey;

    public List<LocationSearchResponse> searchKeyword(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        JsonNode response = request("/v2/local/search/keyword.json", query);
        return toLocations(response.path("documents"), true);
    }

    public Optional<LocationSearchResponse> searchAddress(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        JsonNode response = request("/v2/local/search/address.json", address);
        List<LocationSearchResponse> locations = toLocations(response.path("documents"), false);
        return locations.stream().findFirst();
    }

    public Optional<String> reverseRegion(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return Optional.empty();
        }
        JsonNode response = requestCoordinate(lng, lat);
        JsonNode address = response.path("documents").path(0).path("address");
        String region1 = text(address, "region_1depth_name");
        String region2 = text(address, "region_2depth_name");
        String region3 = text(address, "region_3depth_name");
        String region = joinRegion(region1, region2, region3);
        return region.isBlank() ? Optional.empty() : Optional.of(region);
    }

    private JsonNode request(String path, String query) {
        try {
            return WebClient.create(BASE_URL)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("query", query)
                            .build())
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(2));
        } catch (WebClientResponseException e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED,
                    "카카오 Local API 응답 오류: " + e.getStatusCode());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED,
                    "카카오 Local API 응답을 처리할 수 없습니다. " + e.getMessage());
        }
    }

    private JsonNode requestCoordinate(Double x, Double y) {
        try {
            return WebClient.create(BASE_URL)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/geo/coord2address.json")
                            .queryParam("x", x)
                            .queryParam("y", y)
                            .build())
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(2));
        } catch (WebClientResponseException e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED,
                    "카카오 좌표 변환 API 응답 오류: " + e.getStatusCode());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED,
                    "카카오 좌표 변환 API 응답을 처리할 수 없습니다. " + e.getMessage());
        }
    }

    private String joinRegion(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                parts.add(value);
            }
        }
        return String.join(" ", parts);
    }

    private List<LocationSearchResponse> toLocations(JsonNode documents, boolean keywordMode) {
        if (documents == null || !documents.isArray()) {
            return List.of();
        }

        List<LocationSearchResponse> locations = new ArrayList<>();
        documents.forEach(document -> {
            String name = keywordMode ? text(document, "place_name") : text(document, "address_name");
            String address = text(document, "road_address_name");
            if (address == null || address.isBlank()) {
                address = text(document, "address_name");
            }
            Double lat = decimal(document, "y");
            Double lng = decimal(document, "x");
            if (lat != null && lng != null) {
                locations.add(new LocationSearchResponse(name, address, lat, lng));
            }
        });
        return locations;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText(null);
    }

    private Double decimal(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        return value == null || value.isBlank() ? null : new BigDecimal(value).doubleValue();
    }
}
