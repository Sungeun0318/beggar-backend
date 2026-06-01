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

    private final KakaoLocalClient kakaoLocalClient;
    private final ConcurrentHashMap<String, LocationSearchResponse> addressCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> coordinateRegionCache = new ConcurrentHashMap<>();

    public List<LocationSearchResponse> search(String query) {
        return kakaoLocalClient.searchKeyword(query);
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
}
