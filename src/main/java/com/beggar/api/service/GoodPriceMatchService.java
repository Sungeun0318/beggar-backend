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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GoodPriceMatchService {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGE = 10;
    private static final int MIN_NAME_SCORE = 80;
    private static final int MIN_ADDRESS_SCORE = 40;
    private static final int MIN_TOTAL_SCORE = 120;
    private static final Pattern DISTRICT_PATTERN = Pattern.compile("(\\S+(시|군|구))");

    private final GoodPriceStoreClient goodPriceStoreClient;

    public MatchResult match(String storeName, String address) {
        String normalizedName = normalize(storeName);
        String normalizedAddress = normalize(address);
        if (normalizedName.isBlank() && normalizedAddress.isBlank()) {
            return MatchResult.notMatched("가게명과 주소가 없어 착한가격업소 검증을 건너뜀");
        }

        try {
            Optional<ScoredStore> best = findCandidates(address).stream()
                    .map(store -> score(store, normalizedName, normalizedAddress))
                    .max(Comparator.comparingInt(ScoredStore::totalScore));

            return best
                    .filter(ScoredStore::isMatched)
                    .map(scored -> MatchResult.matched(scored.store(), scored.totalScore(), scored.reason()))
                    .orElseGet(() -> MatchResult.notMatched(
                            best.map(ScoredStore::reason).orElse("착한가격업소 후보를 찾지 못함")
                    ));
        } catch (CustomException e) {
            return MatchResult.notMatched("착한가격업소 API 호출 실패");
        }
    }

    private List<GoodPriceStore> findCandidates(String address) {
        String keyword = extractAddressKeyword(address);
        if (!keyword.isBlank()) {
            List<GoodPriceStore> stores = goodPriceStoreClient.searchByAddressKeyword(keyword, 1, PAGE_SIZE * 3);
            if (!stores.isEmpty()) {
                return stores;
            }
        }

        return java.util.stream.IntStream.rangeClosed(1, MAX_PAGE)
                .boxed()
                .flatMap(page -> goodPriceStoreClient.search(page, PAGE_SIZE).stream())
                .toList();
    }

    private ScoredStore score(GoodPriceStore store, String normalizedName, String normalizedAddress) {
        String storeName = normalize(store.name());
        String storeAddress = normalize(store.address());

        int nameScore = 0;
        if (!normalizedName.isBlank()) {
            if (storeName.equals(normalizedName)) {
                nameScore = 100;
            } else if (storeName.contains(normalizedName) || normalizedName.contains(storeName)) {
                nameScore = 80;
            } else {
                nameScore = similarityScore(storeName, normalizedName);
            }
        }

        int addressScore = 0;
        if (!normalizedAddress.isBlank() && !storeAddress.isBlank()) {
            if (storeAddress.equals(normalizedAddress)) {
                addressScore = 60;
            } else if (storeAddress.contains(normalizedAddress) || normalizedAddress.contains(storeAddress)) {
                addressScore = 50;
            } else {
                addressScore = commonAddressTokenScore(storeAddress, normalizedAddress);
            }
        }

        int totalScore = nameScore + addressScore;
        String reason = "가게명 %d점, 주소 %d점, 총점 %d점".formatted(nameScore, addressScore, totalScore);
        return new ScoredStore(store, nameScore, addressScore, totalScore, reason);
    }

    private int commonAddressTokenScore(String storeAddress, String receiptAddress) {
        int matched = 0;
        for (String token : receiptAddress.split("\\s+")) {
            String normalizedToken = normalize(token);
            if (!normalizedToken.isBlank() && storeAddress.contains(normalizedToken)) {
                matched++;
            }
        }
        return Math.min(45, matched * 10);
    }

    private int similarityScore(String source, String target) {
        if (source.isBlank() || target.isBlank()) {
            return 0;
        }
        int maxLength = Math.max(source.length(), target.length());
        int distance = levenshteinDistance(source, target);
        return Math.max(0, (int) Math.round((1.0 - ((double) distance / maxLength)) * 100));
    }

    private int levenshteinDistance(String source, String target) {
        int[][] dp = new int[source.length() + 1][target.length() + 1];
        for (int i = 0; i <= source.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= target.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= source.length(); i++) {
            for (int j = 1; j <= target.length(); j++) {
                int cost = source.charAt(i - 1) == target.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[source.length()][target.length()];
    }

    private String extractAddressKeyword(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }
        java.util.regex.Matcher matcher = DISTRICT_PATTERN.matcher(address);
        while (matcher.find()) {
            String token = matcher.group(1);
            if (token.endsWith("구") || token.endsWith("군")) {
                return token;
            }
        }
        return address.trim().split("\\s+")[0];
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s\\-()]", "").toLowerCase(Locale.KOREAN);
    }

    public record MatchResult(
            boolean matched,
            GoodPriceStore store,
            int score,
            String reason
    ) {
        private static MatchResult matched(GoodPriceStore store, int score, String reason) {
            return new MatchResult(true, store, score, reason);
        }

        private static MatchResult notMatched(String reason) {
            return new MatchResult(false, null, 0, reason);
        }
    }

    private record ScoredStore(
            GoodPriceStore store,
            int nameScore,
            int addressScore,
            int totalScore,
            String reason
    ) {
        private boolean isMatched() {
            return nameScore >= MIN_NAME_SCORE
                    && addressScore >= MIN_ADDRESS_SCORE
                    && totalScore >= MIN_TOTAL_SCORE;
        }
    }
}
