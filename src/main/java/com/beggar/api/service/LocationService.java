package com.beggar.api.service;

import com.beggar.api.client.kakao.KakaoLocalClient;
import com.beggar.api.dto.location.LocationSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class LocationService {
    private static final int SEARCH_RETRY_COUNT = 3;
    private static final long SEARCH_RETRY_DELAY_MS = 250;

    private final KakaoLocalClient kakaoLocalClient;
    private final ConcurrentHashMap<String, List<LocationSearchResponse>> keywordCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocationSearchResponse> addressCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> coordinateRegionCache = new ConcurrentHashMap<>();

    public List<LocationSearchResponse> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalizedQuery = query.trim();
        List<LocationSearchResponse> cached = keywordCache.get(normalizedQuery);
        if (cached != null) {
            return cached;
        }

        RuntimeException lastError = null;
        for (int attempt = 0; attempt < SEARCH_RETRY_COUNT; attempt++) {
            try {
                List<LocationSearchResponse> result = kakaoLocalClient.searchKeyword(normalizedQuery);
                if (!result.isEmpty()) {
                    List<LocationSearchResponse> stableResult = List.copyOf(result);
                    keywordCache.put(normalizedQuery, stableResult);
                    return stableResult;
                }
            } catch (RuntimeException e) {
                lastError = e;
            }
            sleepBeforeRetry(attempt);
        }
        if (lastError != null) {
            throw lastError;
        }
        return List.of();
    }

    public Optional<LocationSearchResponse> resolveAddress(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        LocationSearchResponse cached = addressCache.get(address);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<LocationSearchResponse> resolved;
        try {
            resolved = kakaoLocalClient.searchAddress(address);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        resolved.ifPresent(location -> addressCache.put(address, location));
        return resolved;
    }

    public Optional<String> resolveRegion(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return Optional.empty();
        }
        String key = "%.5f,%.5f".formatted(lat, lng);
        String cached = coordinateRegionCache.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<String> resolved;
        try {
            resolved = kakaoLocalClient.reverseRegion(lat, lng);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        resolved.ifPresent(region -> coordinateRegionCache.put(key, region));
        return resolved;
    }

    private void sleepBeforeRetry(int attempt) {
        if (attempt >= SEARCH_RETRY_COUNT - 1) {
            return;
        }
        try {
            Thread.sleep(SEARCH_RETRY_DELAY_MS * (attempt + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
