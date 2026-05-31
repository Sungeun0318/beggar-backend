package com.beggar.api.service;

import com.beggar.api.client.goodprice.GoodPriceStoreClient;
import com.beggar.api.dto.goodprice.GoodPriceStore;
import com.beggar.api.dto.recommendation.RecommendationResponse;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomPurposeTag;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.RoomMemberRepository;
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
    private final RoomMemberRepository roomMemberRepository;
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
        long activeMemberCount = Math.max(1, roomMemberRepository.countByRoom_RoomNoAndStatus(roomNo, com.beggar.api.entity.RoomMember.Status.ACTIVE));
        Integer recommendationBudget = recommendationBudget(remainingBudget, activeMemberCount, tag);

        RecommendationCandidates candidates = findGoodPriceStores(
                tag,
                requestedRegion,
                remainingBudget,
                recommendationBudget,
                activeMemberCount
        );

        List<RecommendationResponse.Place> places = candidates.stores().stream()
                .limit(DEFAULT_RESULT_SIZE)
                .map(store -> toPlace(store, recommendationBudget, tag))
                .toList();
        boolean fallbackApplied = candidates.fallbackApplied() || (remainingBudget != null && remainingBudget <= 0);

        return new RecommendationResponse(
                roomNo,
                totalBudget,
                spentAmount,
                remainingBudget,
                recommendationBudget,
                budgetGuide(recommendationBudget, tag, fallbackApplied),
                fallbackApplied,
                tag,
                requestedRegion,
                places
        );
    }

    private RecommendationCandidates findGoodPriceStores(String tag, String requestedRegion,
                                                         Integer remainingBudget, Integer recommendationBudget,
                                                         long activeMemberCount) {
        List<GoodPriceStore> allStores = new ArrayList<>();
        for (int page = 1; page <= GOOD_PRICE_MAX_PAGE; page++) {
            allStores.addAll(goodPriceStoreClient.search(page, GOOD_PRICE_PAGE_SIZE));
        }

        List<GoodPriceStore> strictBudget = filterStores(allStores, tag, requestedRegion, recommendationBudget);
        if (strictBudget.size() >= DEFAULT_RESULT_SIZE) {
            return new RecommendationCandidates(strictBudget, false);
        }

        List<GoodPriceStore> perPersonBudget = filterStores(
                allStores,
                tag,
                requestedRegion,
                perPersonRemainingBudget(remainingBudget, activeMemberCount)
        );
        if (perPersonBudget.size() >= DEFAULT_RESULT_SIZE) {
            return new RecommendationCandidates(perPersonBudget, true);
        }

        List<GoodPriceStore> noBudgetLimit = filterStores(allStores, tag, requestedRegion, null);
        if (noBudgetLimit.size() >= DEFAULT_RESULT_SIZE || requestedRegion == null || requestedRegion.isBlank()) {
            return new RecommendationCandidates(noBudgetLimit, true);
        }

        return new RecommendationCandidates(filterStores(allStores, tag, null, null), true);
    }

    private List<GoodPriceStore> filterStores(List<GoodPriceStore> stores, String tag, String requestedRegion, Integer maxPrice) {
        return stores.stream()
                .filter(store -> matchesRegion(store, requestedRegion))
                .filter(store -> matchesTag(store, tag))
                .filter(store -> maxPrice == null || store.price() == null || store.price() <= maxPrice)
                .sorted(Comparator.comparing((GoodPriceStore store) -> affordableRank(store, maxPrice))
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

    private Integer recommendationBudget(Integer remainingBudget, long activeMemberCount, String tag) {
        Integer perPerson = perPersonRemainingBudget(remainingBudget, activeMemberCount);
        if (perPerson == null || perPerson <= 0) {
            return null;
        }

        double ratio = switch (recommendationTagGroup(tag)) {
            case "CAFE" -> 0.6;
            case "PLAY" -> 1.0;
            default -> 0.7;
        };
        return Math.max(0, (int) Math.floor(perPerson * ratio));
    }

    private Integer perPersonRemainingBudget(Integer remainingBudget) {
        return perPersonRemainingBudget(remainingBudget, 1);
    }

    private Integer perPersonRemainingBudget(Integer remainingBudget, long activeMemberCount) {
        if (remainingBudget == null) {
            return null;
        }
        if (remainingBudget <= 0) {
            return null;
        }
        return (int) Math.floor((double) remainingBudget / Math.max(1, activeMemberCount));
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

    private String buildReason(GoodPriceStore store, Integer recommendationBudget, String tag) {
        if (store.price() != null && recommendationBudget != null && store.price() <= recommendationBudget) {
            return "다음 일정까지 고려한 1인 추천 예산 안에 들어오는 착한가격업소예요.";
        }
        if (tag != null && !tag.isBlank()) {
            return tag + " 태그와 맞는 착한가격업소예요.";
        }
        return "착한가격업소 기준으로 추천했어요.";
    }

    private String budgetGuide(Integer recommendationBudget, String tag, boolean fallbackApplied) {
        if (recommendationBudget == null) {
            return fallbackApplied
                    ? "남은 예산이 부족해서 가격 조건을 빼고 태그와 지역 기준으로 추천했어요."
                    : "예산 정보 없이 지역과 태그 기준으로 추천했어요.";
        }
        String base = "%s 기준 1인 추천 예산은 약 %,d원이에요.".formatted(displayTag(tag), recommendationBudget);
        if (fallbackApplied) {
            return base + " 결과가 부족해 조건을 조금 넓혀 보여드려요.";
        }
        return base + " 다음 일정에 쓸 돈을 남기는 기준이에요.";
    }

    private String displayTag(String tag) {
        return tag == null || tag.isBlank() ? "현재 태그" : tag;
    }

    private String recommendationTagGroup(String tag) {
        String normalizedTag = normalize(tag);
        if (containsAnyKeyword(new String[]{normalizedTag}, "카페", "커피", "음료")) {
            return "CAFE";
        }
        if (containsAnyKeyword(new String[]{normalizedTag}, "놀거리", "문화", "여가")) {
            return "PLAY";
        }
        return "FOOD";
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

    private record RecommendationCandidates(List<GoodPriceStore> stores, boolean fallbackApplied) {
    }
}
