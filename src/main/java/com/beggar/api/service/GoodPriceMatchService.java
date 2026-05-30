package com.beggar.api.service;

import com.beggar.api.client.goodprice.GoodPriceStoreClient;
import com.beggar.api.common.exception.CustomException;
import com.beggar.api.dto.goodprice.GoodPriceStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoodPriceMatchService {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGE = 10;
    private static final int MATCH_THRESHOLD = 80;

    private final GoodPriceStoreClient goodPriceStoreClient;

    public Optional<GoodPriceStore> match(String storeName, String address) {
        String normalizedName = normalize(storeName);
        String normalizedAddress = normalize(address);
        if (normalizedName.isBlank() && normalizedAddress.isBlank()) {
            return Optional.empty();
        }

        try {
            return findCandidates().stream()
                    .map(store -> new ScoredStore(store, score(store, normalizedName, normalizedAddress)))
                    .filter(scored -> scored.score() >= MATCH_THRESHOLD)
                    .max(Comparator.comparingInt(ScoredStore::score))
                    .map(ScoredStore::store);
        } catch (CustomException e) {
            return Optional.empty();
        }
    }

    private List<GoodPriceStore> findCandidates() {
        return java.util.stream.IntStream.rangeClosed(1, MAX_PAGE)
                .boxed()
                .flatMap(page -> goodPriceStoreClient.search(page, PAGE_SIZE).stream())
                .toList();
    }

    private int score(GoodPriceStore store, String normalizedName, String normalizedAddress) {
        String storeName = normalize(store.name());
        String storeAddress = normalize(store.address());

        int score = 0;
        if (!normalizedName.isBlank()) {
            if (storeName.equals(normalizedName)) {
                score += 90;
            } else if (storeName.contains(normalizedName) || normalizedName.contains(storeName)) {
                score += 65;
            }
        }

        if (!normalizedAddress.isBlank() && !storeAddress.isBlank()) {
            if (storeAddress.equals(normalizedAddress)) {
                score += 30;
            } else if (storeAddress.contains(normalizedAddress) || normalizedAddress.contains(storeAddress)) {
                score += 20;
            } else {
                score += commonAddressTokenScore(storeAddress, normalizedAddress);
            }
        }
        return score;
    }

    private int commonAddressTokenScore(String storeAddress, String receiptAddress) {
        int matched = 0;
        for (String token : receiptAddress.split("\\s+")) {
            String normalizedToken = normalize(token);
            if (!normalizedToken.isBlank() && storeAddress.contains(normalizedToken)) {
                matched++;
            }
        }
        return Math.min(20, matched * 5);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s\\-()]", "").toLowerCase(Locale.KOREAN);
    }

    private record ScoredStore(GoodPriceStore store, int score) {
    }
}
