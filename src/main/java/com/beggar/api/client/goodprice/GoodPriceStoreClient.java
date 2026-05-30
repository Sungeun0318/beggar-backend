package com.beggar.api.client.goodprice;

import com.beggar.api.common.exception.CustomException;
import com.beggar.api.common.exception.ErrorCode;
import com.beggar.api.dto.goodprice.GoodPriceStore;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GoodPriceStoreClient {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d[\\d,]*");

    @Value("${good-price.base-url}")
    private String baseUrl;

    @Value("${good-price.api-key}")
    private String apiKey;

    @Value("${good-price.path}")
    private String path;

    public List<GoodPriceStore> search(int pageNo, int numOfRows) {
        try {
            String url = baseUrl + path
                    + "?serviceKey=" + apiKey
                    + "&page=" + pageNo
                    + "&perPage=" + numOfRows
                    + "&returnType=JSON";

            JsonNode response = WebClient.create()
                    .get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return toStores(response);
        } catch (WebClientResponseException e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED,
                    "착한가격업소 API 응답 오류: " + e.getStatusCode());
        } catch (Exception e) {
            throw new CustomException(ErrorCode.EXTERNAL_API_FAILED,
                    "착한가격업소 API 응답을 처리할 수 없습니다. " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private List<GoodPriceStore> toStores(JsonNode response) {
        JsonNode items = findItems(response);
        List<GoodPriceStore> stores = new ArrayList<>();
        if (items == null || items.isMissingNode()) {
            return stores;
        }

        if (items.isObject()) {
            stores.add(toStore(items));
            return stores;
        }

        items.forEach(item -> stores.add(toStore(item)));
        return stores;
    }

    private JsonNode findItems(JsonNode response) {
        if (response == null) {
            return null;
        }
        JsonNode data = response.at("/response/body/items/item");
        if (!data.isMissingNode()) {
            return data;
        }
        data = response.at("/response/body/items");
        if (!data.isMissingNode()) {
            return data;
        }
        data = response.path("data");
        if (!data.isMissingNode()) {
            return data;
        }
        data = response.path("items");
        if (!data.isMissingNode()) {
            return data;
        }
        return response.isArray() ? response : null;
    }

    private GoodPriceStore toStore(JsonNode item) {
        String id = firstText(item, "idx", "id", "storeId", "NO", "연번");
        String name = firstText(item, "sj", "BSSH_NM", "bsshNm", "BSSH", "업소명", "상호", "상호명");
        String category = firstText(item, "cn", "RM", "rm", "업종", "업태", "구분", "category");
        String address = firstText(item, "adres", "RDNMADR", "rdnmadr", "도로명주소", "소재지도로명주소", "주소");
        Integer price = minInt(item, "PC", "pc", "가격", "착한가격 품목 가격", "대표메뉴가격", "가격1", "가격2", "가격3", "가격4");
        BigDecimal lat = firstDecimal(item, "latitude", "lat", "위도");
        BigDecimal lng = firstDecimal(item, "longitude", "lng", "경도");
        String phone = firstText(item, "tel", "CTTPC", "cttpc", "전화번호", "연락처");
        String itemName = firstText(item, "REPRSNT_PRDLST", "reprsntPrdlst", "대표 품목", "대표메뉴", "주요품목", "품목1", "메뉴1", "메뉴2", "메뉴3", "메뉴4");
        return new GoodPriceStore(id, name, category, address, price, lat, lng, phone, itemName);
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                String text = value.asText(null);
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private Integer firstInt(JsonNode node, String... names) {
        String text = firstText(node, names);
        if (text == null) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        Integer min = null;
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group().replace(",", ""));
            if (value <= Integer.MAX_VALUE && (min == null || value < min)) {
                min = (int) value;
            }
        }
        return min;
    }

    private Integer minInt(JsonNode node, String... names) {
        Integer min = null;
        for (String name : names) {
            Integer value = firstInt(node, name);
            if (value != null && (min == null || value < min)) {
                min = value;
            }
        }
        return min;
    }

    private BigDecimal firstDecimal(JsonNode node, String... names) {
        String text = firstText(node, names);
        return text == null ? null : new BigDecimal(text);
    }
}
