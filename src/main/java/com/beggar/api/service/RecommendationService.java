package com.beggar.api.service;

import com.beggar.api.client.goodprice.GoodPriceStoreClient;
import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.goodprice.GoodPriceStore;
import com.beggar.api.dto.location.LocationSearchResponse;
import com.beggar.api.dto.recommendation.RecommendationResponse;
import com.beggar.api.entity.Room;
import com.beggar.api.entity.RoomPurposeTag;
import com.beggar.api.entity.RoomStatus;
import com.beggar.api.repository.ReceiptRepository;
import com.beggar.api.repository.RoomBudgetResultRepository;
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
import java.util.Arrays;
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
    private final RoomBudgetResultRepository roomBudgetResultRepository;
    private final ReceiptRepository receiptRepository;
    private final GoodPriceStoreClient goodPriceStoreClient;
    private final LocationService locationService;

    public RecommendationResponse recommend(Long roomNo, String requestedTag) {
        return recommend(roomNo, requestedTag, null);
    }

    public RecommendationResponse recommend(Long roomNo, String requestedTag, String requestedRegion) {
        return recommend(roomNo, requestedTag, requestedRegion, null, null, 2000);
    }

    public RecommendationResponse recommend(Long roomNo, String requestedTag, String requestedRegion,
                                            Double lat, Double lng, Integer radius) {
        Room room = roomRepository.findById(roomNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND, "방을 찾을 수 없습니다. roomNo=" + roomNo));
        
        if (room.getStatus() == RoomStatus.ENDED) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_ENDED);
        }

        String tag = resolveTag(roomNo, requestedTag);
        String region = resolveRecommendationRegion(room, requestedRegion, lat, lng);
        
        long spentAmount = receiptRepository.sumAmountByRoomNo(roomNo);
        Integer totalBudget = room.getTotalBudget();
        Integer remainingBudget = totalBudget == null ? null : Math.max(0, totalBudget - Math.toIntExact(spentAmount));
        long activeMemberCount = Math.max(1, roomMemberRepository.countByRoom_RoomNoAndStatus(roomNo, com.beggar.api.entity.RoomMember.Status.ACTIVE));
        
        // 확정된 1인 예산 정보가 있으면 우선 사용
        Integer minBudgetPerPerson = roomBudgetResultRepository.findByRoom_RoomNo(roomNo)
                .map(com.beggar.api.entity.RoomBudgetResult::getMinBudgetPerPerson)
                .orElse(perPersonRemainingBudget(remainingBudget, activeMemberCount));

        Integer recommendationBudget = recommendationBudget(minBudgetPerPerson, remainingBudget, activeMemberCount, tag);

        RecommendationCandidates candidates = findGoodPriceStores(
                tag,
                region,
                remainingBudget,
                recommendationBudget,
                activeMemberCount,
                lat,
                lng,
                radius
        );

        List<RecommendationResponse.Place> places = candidates.stores().stream()
                .limit(DEFAULT_RESULT_SIZE)
                .map(store -> toPlace(store, recommendationBudget, tag, lat, lng))
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
                region,
                places
        );
    }

    private RecommendationCandidates findGoodPriceStores(String tag, String requestedRegion,
                                                         Integer remainingBudget, Integer recommendationBudget,
                                                         long activeMemberCount,
                                                         Double lat, Double lng, Integer radius) {
        List<GoodPriceStore> allStores = new ArrayList<>();
        String addressKeyword = regionKeyword(requestedRegion);
        if (!addressKeyword.isBlank()) {
            try {
                allStores.addAll(goodPriceStoreClient.searchByAddressKeyword(addressKeyword, 1, GOOD_PRICE_PAGE_SIZE * 3));
            } catch (RuntimeException ignored) {
                allStores.clear();
            }
        }
        for (int page = 1; allStores.isEmpty() && page <= GOOD_PRICE_MAX_PAGE && allStores.size() < GOOD_PRICE_PAGE_SIZE * 3; page++) {
            List<GoodPriceStore> pageStores;
            try {
                pageStores = goodPriceStoreClient.search(page, GOOD_PRICE_PAGE_SIZE);
            } catch (RuntimeException e) {
                break;
            }
            if (pageStores.isEmpty()) {
                break;
            }
            allStores.addAll(pageStores);
        }

        Double filterLat = lat;
        Double filterLng = lng;

        List<GoodPriceStore> strictBudget = filterStores(allStores, tag, requestedRegion, recommendationBudget, filterLat, filterLng, radius);
        if (strictBudget.size() >= DEFAULT_RESULT_SIZE) {
            return new RecommendationCandidates(strictBudget, false);
        }

        List<GoodPriceStore> perPersonBudget = filterStores(
                allStores,
                tag,
                requestedRegion,
                perPersonRemainingBudget(remainingBudget, activeMemberCount),
                filterLat,
                filterLng,
                radius
        );
        if (perPersonBudget.size() >= DEFAULT_RESULT_SIZE) {
            return new RecommendationCandidates(perPersonBudget, true);
        }

        List<GoodPriceStore> noBudgetLimit = filterStores(allStores, tag, requestedRegion, null, filterLat, filterLng, radius);
        if (noBudgetLimit.size() >= DEFAULT_RESULT_SIZE
                || (!noBudgetLimit.isEmpty() && (requestedRegion == null || requestedRegion.isBlank()))) {
            return new RecommendationCandidates(noBudgetLimit, true);
        }

        List<GoodPriceStore> relaxedRegion = filterStores(allStores, tag, null, null, filterLat, filterLng, radius);
        if (!relaxedRegion.isEmpty() || lat == null || lng == null) {
            return new RecommendationCandidates(relaxedRegion, true);
        }

        return new RecommendationCandidates(filterStores(allStores, tag, null, null, null, null, null), true);
    }

    private String resolveRecommendationRegion(Room room, String requestedRegion, Double lat, Double lng) {
        if (requestedRegion != null && !requestedRegion.isBlank()) {
            return requestedRegion;
        }
        if (room.getLocation() != null && !room.getLocation().isBlank()) {
            return room.getLocation();
        }
        return locationService.resolveRegion(lat, lng).orElse(null);
    }

    private String regionKeyword(String region) {
        if (region == null || region.isBlank()) {
            return "";
        }
        for (String part : region.trim().split("\\s+")) {
            if (part.endsWith("시") && !part.endsWith("특별시") && !part.endsWith("광역시")) {
                return part.substring(0, part.length() - 1);
            }
            if (part.endsWith("군") || part.endsWith("구")) {
                return part;
            }
        }
        return region.trim();
    }

    private List<GoodPriceStore> filterStores(List<GoodPriceStore> stores, String tag, String requestedRegion,
                                              Integer maxPrice, Double lat, Double lng, Integer radius) {
        return stores.stream()
                .filter(store -> matchesRegion(store, requestedRegion))
                .filter(store -> matchesTag(store, tag))
                .filter(store -> maxPrice == null || store.price() == null || store.price() <= maxPrice)
                .filter(store -> withinRadius(store, lat, lng, radius))
                .sorted(Comparator.comparing((GoodPriceStore store) -> affordableRank(store, maxPrice))
                        .thenComparing(store -> distanceRank(store, lat, lng))
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

    private Integer recommendationBudget(Integer minBudgetPerPerson, Integer remainingBudget, long activeMemberCount, String tag) {
        Integer perPerson = (minBudgetPerPerson != null) ? minBudgetPerPerson : perPersonRemainingBudget(remainingBudget, activeMemberCount);
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

    private RecommendationResponse.Place toPlace(GoodPriceStore store, Integer remainingBudget, String tag,
                                                 Double baseLat, Double baseLng) {
        Double lat = storeLat(store);
        Double lng = storeLng(store);
        String reason = buildReason(store, remainingBudget, tag);
        String thumbnailUrl = thumbnailUrl(store.category(), store.itemName(), store.name());
        String mapUrl = kakaoMapSearchUrl(store.name(), store.address());
        String walkTime = walkTime(distanceMeters(lat, lng, baseLat, baseLng));
        return new RecommendationResponse.Place(
                store.storeId(),
                store.name(),
                store.category(),
                store.price(),
                store.itemName(),
                walkTime,
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
                    ? "조건에 맞는 추천이 부족해서 범위를 넓혀봤어요."
                    : "예산 정보 없이 지역과 태그 기준으로 추천했어요.";
        }
        String base = "%s 기준 1인 추천 예산은 약 %,d원이에요.".formatted(displayTag(tag), recommendationBudget);
        if (fallbackApplied) {
            return base + " 조건에 맞는 추천이 부족해서 범위를 넓혀봤어요.";
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
        String keyword = ((name == null ? "" : name) + " " + shortRegion(address)).trim();
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        return "https://map.kakao.com/link/search/" + encodedKeyword;
    }

    private String shortRegion(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }
        String[] parts = address.trim().split("\\s+");
        int limit = Math.min(parts.length, 3);
        return String.join(" ", Arrays.copyOf(parts, limit));
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

    private boolean withinRadius(GoodPriceStore store, Double baseLat, Double baseLng, Integer radius) {
        if (baseLat == null || baseLng == null || radius == null || radius <= 0) {
            return true;
        }
        Double distance = distanceMeters(storeLat(store), storeLng(store), baseLat, baseLng);
        return distance != null && distance <= radius;
    }

    private Double distanceRank(GoodPriceStore store, Double baseLat, Double baseLng) {
        Double distance = distanceMeters(storeLat(store), storeLng(store), baseLat, baseLng);
        return distance == null ? Double.MAX_VALUE : distance;
    }

    private Double distanceMeters(Double lat, Double lng, Double baseLat, Double baseLng) {
        if (lat == null || lng == null || baseLat == null || baseLng == null) {
            return null;
        }
        double earthRadius = 6371000.0;
        double latDistance = Math.toRadians(lat - baseLat);
        double lngDistance = Math.toRadians(lng - baseLng);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(baseLat)) * Math.cos(Math.toRadians(lat))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private Double rawStoreLat(GoodPriceStore store) {
        return toDouble(store.lat());
    }

    private Double rawStoreLng(GoodPriceStore store) {
        return toDouble(store.lng());
    }

    private Double storeLat(GoodPriceStore store) {
        Double lat = rawStoreLat(store);
        if (lat != null) {
            return lat;
        }
        return locationService.resolveAddress(store.address()).map(LocationSearchResponse::lat).orElse(null);
    }

    private Double storeLng(GoodPriceStore store) {
        Double lng = rawStoreLng(store);
        if (lng != null) {
            return lng;
        }
        return locationService.resolveAddress(store.address()).map(LocationSearchResponse::lng).orElse(null);
    }

    private String walkTime(Double distanceMeters) {
        if (distanceMeters == null) {
            return null;
        }
        int minutes = Math.max(1, (int) Math.ceil(distanceMeters / 67.0));
        return "도보 " + minutes + "분";
    }

    private record RecommendationCandidates(List<GoodPriceStore> stores, boolean fallbackApplied) {
    }
}
