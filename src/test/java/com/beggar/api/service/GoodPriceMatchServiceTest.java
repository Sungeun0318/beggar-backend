package com.beggar.api.service;

import com.beggar.api.client.goodprice.GoodPriceStoreClient;
import com.beggar.api.dto.goodprice.GoodPriceStore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoodPriceMatchServiceTest {

    private final GoodPriceStoreClient client = mock(GoodPriceStoreClient.class);
    private final GoodPriceMatchService service = new GoodPriceMatchService(client);

    @Test
    void match_returnsStore_whenStoreNameAndAddressMatch() {
        GoodPriceStore store = new GoodPriceStore(
                "store-1",
                "착한식당",
                "한식",
                "서울특별시 중구 세종대로 1",
                7000,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "02-0000-0000",
                "백반"
        );
        when(client.searchByAddressKeyword(anyString(), anyInt(), anyInt())).thenReturn(List.of(store));

        var matched = service.match("착한식당", "서울특별시 중구 세종대로 1");

        assertThat(matched.matched()).isTrue();
        assertThat(matched.store().storeId()).isEqualTo("store-1");
        assertThat(matched.score()).isGreaterThanOrEqualTo(120);
    }

    @Test
    void match_returnsEmpty_whenStoreNameDoesNotMatch() {
        GoodPriceStore store = new GoodPriceStore(
                "store-1",
                "다른식당",
                "한식",
                "서울특별시 중구 세종대로 1",
                7000,
                null,
                null,
                null,
                "백반"
        );
        when(client.searchByAddressKeyword(anyString(), anyInt(), anyInt())).thenReturn(List.of(store));

        var matched = service.match("없는식당", "서울특별시 중구 세종대로 1");

        assertThat(matched.matched()).isFalse();
        assertThat(matched.store()).isNull();
    }
}
