package com.beggar.api.service;

import com.beggar.api.client.goodprice.GoodPriceStoreClient;
import com.beggar.api.dto.goodprice.GoodPriceStore;
import com.beggar.api.dto.recommendation.RecommendationResponse;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomPurposeTag;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.RoomPurposeTagRepository;
import com.beggar.api.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecommendationService {

    private static final int DEFAULT_RESULT_SIZE = 5;
    private static final int GOOD_PRICE_PAGE_SIZE = 100;
    private static final int GOOD_PRICE_MAX_PAGE = 10;

    private final RoomRepository roomRepository;
    private final RoomPurposeTagRepository roomPurposeTagRepository;
    private final ReceiptRepository receiptRepository;
    private final GoodPriceStoreClient goodPriceStoreClient;

    public RecommendationResponse recommend(Long roomNo, String requestedTag) {
        return recommend(roomNo, requestedTag, null);
    }

    public RecommendationResponse recommend(Long roomNo, String requestedTag, String requestedRegion) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다. roomNo=" + roomNo));
        String tag = resolveTag(roomNo, requestedTag);
        long spentAmount = receiptRepository.sumAmountByRoomNo(roomNo);
        Integer totalBudget = room.getTotalBudget();
        Integer remainingBudget = totalBudget == null ? null : Math.max(0, totalBudget - Math.toIntExact(spentAmount));

        List<GoodPriceStore> stores = findGoodPriceStores(tag, requestedRegion, remainingBudget);

        List<RecommendationResponse.Place> places = stores.stream()
                .limit(DEFAULT_RESULT_SIZE)
                .map(store -> toPlace(store, remainingBudget, tag))
                .toList();

        return new RecommendationResponse(roomNo, totalBudget, spentAmount, remainingBudget, tag, requestedRegion, places);
    }

    private List<GoodPriceStore> findGoodPriceStores(String tag, String requestedRegion, Integer remainingBudget) {
        List<GoodPriceStore> stores = new ArrayList<>();
        for (int page = 1; page <= GOOD_PRICE_MAX_PAGE && stores.size() < DEFAULT_RESULT_SIZE; page++) {
            List<GoodPriceStore> pageStores = goodPriceStoreClient.search(page, GOOD_PRICE_PAGE_SIZE).stream()
                .filter(store -> matchesRegion(store, requestedRegion))
                .filter(store -> matchesTag(store, tag))
                .sorted(Comparator.comparing((GoodPriceStore store) -> affordableRank(store, remainingBudget))
                        .thenComparing(store -> store.price() == null ? Integer.MAX_VALUE : store.price()))
                .toList();
            stores.addAll(pageStores);
        }
        return stores.stream()
                .sorted(Comparator.comparing((GoodPriceStore store) -> affordableRank(store, remainingBudget))
                        .thenComparing(store -> store.price() == null ? Integer.MAX_VALUE : store.price()))
                .toList();
    }

    private String resolveTag(Long roomNo, String requestedTag) {
        if (requestedTag != null && !requestedTag.isBlank()) {
            return requestedTag;
        }
        return roomPurposeTagRepository.findAllByRoom_RoomNo(roomNo).stream()
                .map(RoomPurposeTag::getTag)
                .findFirst()
                .orElse(null);
    }

    private boolean matchesTag(GoodPriceStore store, String tag) {
        if (tag == null || tag.isBlank()) {
            return true;
        }
        String normalizedTag = normalize(tag);
        String category = normalize(store.category());
        String itemName = normalize(store.itemName());
        String name = normalize(store.name());

        return containsAnyKeyword(new String[]{category, itemName, name}, normalizedTag)
                || ("식사".equals(normalizedTag) && containsAnyKeyword(new String[]{category, itemName, name}, "음식", "외식", "한식", "중식", "일식", "양식", "분식"))
                || ("카페".equals(normalizedTag) && containsAnyKeyword(new String[]{category, itemName, name}, "카페", "커피", "음료", "차"))
                || ("놀거리".equals(normalizedTag) && containsAnyKeyword(new String[]{category, itemName, name}, "문화", "여가", "노래", "볼링", "당구", "보드게임"));
    }

    private boolean matchesRegion(GoodPriceStore store, String region) {
        if (region == null || region.isBlank()) {
            return true;
        }
        String address = normalize(store.address());
        for (String keyword : region.trim().split("\\s+")) {
            if (!address.contains(normalize(keyword))) {
                return false;
            }
        }
        return true;
    }

    private int affordableRank(GoodPriceStore store, Integer remainingBudget) {
        if (remainingBudget == null || store.price() == null) {
            return 1;
        }
        return store.price() <= remainingBudget ? 0 : 2;
    }

    private RecommendationResponse.Place toPlace(GoodPriceStore store, Integer remainingBudget, String tag) {
        Double lat = toDouble(store.lat());
        Double lng = toDouble(store.lng());
        String reason = buildReason(store, remainingBudget, tag);
        String thumbnailUrl = thumbnailUrl(store.category(), store.itemName(), store.name());
        String mapUrl = kakaoMapSearchUrl(store.name(), store.address());
        return new RecommendationResponse.Place(
                store.storeId(),
                store.name(),
                store.category(),
                store.price(),
                null,
                null,
                thumbnailUrl,
                store.address(),
                mapUrl,
                lat,
                lng,
                "GOOD_PRICE_STORE",
                reason
        );
    }

    private String buildReason(GoodPriceStore store, Integer remainingBudget, String tag) {
        if (store.price() != null && remainingBudget != null && store.price() <= remainingBudget) {
            return "남은 예산 안에서 갈 수 있는 착한가격업소예요.";
        }
        if (tag != null && !tag.isBlank()) {
            return tag + " 태그와 맞는 착한가격업소예요.";
        }
        return "착한가격업소 기준으로 추천했어요.";
    }

    private String kakaoMapSearchUrl(String name, String address) {
        String keyword = ((address == null ? "" : address) + " " + (name == null ? "" : name)).trim();
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        return "https://map.kakao.com/link/search/" + encodedKeyword;
    }

    private String thumbnailUrl(String category, String itemName, String name) {
        String normalizedCategory = normalize(category);
        String normalizedItem = normalize(itemName);
        String normalizedName = normalize(name);

        if ("기타요식업".equals(normalizedCategory)
                && containsAnyKeyword(new String[]{normalizedItem, normalizedName}, "카페", "커피", "음료", "차", "라떼", "아메리카노")) {
            return "assets/images/figma/reco_cafe.png";
        }
        if (containsAnyKeyword(new String[]{normalizedCategory}, "한식", "양식", "일식", "중식", "기타요식업")) {
            return "assets/images/figma/reco_food.png";
        }
        return "assets/images/figma/reco_food.png";
    }

    private boolean containsAnyKeyword(String[] values, String... keywords) {
        for (String value : values) {
            for (String keyword : keywords) {
                if (value != null && keyword != null && value.contains(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsAnyKeyword(String[] values, String keyword) {
        for (String value : values) {
            if (value != null && keyword != null && value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(Locale.KOREAN);
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }
}
