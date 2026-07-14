package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.domain.cardtransaction.client.ExchangeRateApiClient;
import com.weaone.themoa.domain.cardtransaction.client.ExchangeRateApiException;
import com.weaone.themoa.domain.cardtransaction.client.ExchangeRateApiRecord;
import com.weaone.themoa.domain.cardtransaction.entity.ExchangeRateCache;
import com.weaone.themoa.domain.cardtransaction.repository.ExchangeRateCacheRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    private static final LocalDate TX_DATE = LocalDate.of(2026, 6, 10);

    @Mock
    private ExchangeRateApiClient exchangeRateApiClient;
    @Mock
    private ExchangeRateCacheRepository exchangeRateCacheRepository;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Test
    @DisplayName("캐시에 거래일 환율이 있으면 API를 호출하지 않는다")
    void returnsCachedRateWithoutCallingApi() {
        ExchangeRateCache cached = ExchangeRateCache.of(TX_DATE, "USD", BigDecimal.valueOf(1392.40), null);
        given(exchangeRateCacheRepository.findByRateDateAndCurrencyCode(TX_DATE, "USD"))
                .willReturn(Optional.of(cached));

        ExchangeRateResult result = exchangeRateService.getRate("USD", TX_DATE);

        assertThat(result.rate()).isEqualByComparingTo("1392.40");
        assertThat(result.estimated()).isFalse();
        then(exchangeRateApiClient).should(never()).fetch(any());
    }

    @Test
    @DisplayName("당일 응답이 비어 있으면(비영업일) 하루씩 소급해 성공한 날짜의 환율을 쓴다")
    void walksBackOnEmptyResponse() {
        given(exchangeRateCacheRepository.findByRateDateAndCurrencyCode(any(), eq("USD")))
                .willReturn(Optional.empty());
        given(exchangeRateApiClient.fetch(TX_DATE)).willReturn(List.of());
        given(exchangeRateApiClient.fetch(TX_DATE.minusDays(1)))
                .willReturn(List.of(new ExchangeRateApiRecord(1, "USD", "1,392.40")));

        ExchangeRateResult result = exchangeRateService.getRate("USD", TX_DATE);

        assertThat(result.rate()).isEqualByComparingTo("1392.40");
        assertThat(result.estimated()).isFalse();
        then(exchangeRateCacheRepository).should().save(any(ExchangeRateCache.class));
    }

    @Test
    @DisplayName("JPY는 100엔 단위 고시라 100으로 나눠 저장한다")
    void dividesJpyByHundred() {
        given(exchangeRateCacheRepository.findByRateDateAndCurrencyCode(TX_DATE, "JPY"))
                .willReturn(Optional.empty());
        given(exchangeRateApiClient.fetch(TX_DATE))
                .willReturn(List.of(new ExchangeRateApiRecord(1, "JPY(100)", "890.12")));

        ExchangeRateResult result = exchangeRateService.getRate("JPY", TX_DATE);

        assertThat(result.rate()).isEqualByComparingTo("8.9012");
    }

    @Test
    @DisplayName("API 장애 시 캐시의 직전 성공 환율로 폴백하고 estimated=true로 표시한다")
    void fallsBackToLastCachedRateOnApiFailure() {
        given(exchangeRateCacheRepository.findByRateDateAndCurrencyCode(any(), eq("USD")))
                .willReturn(Optional.empty());
        given(exchangeRateApiClient.fetch(any())).willThrow(new ExchangeRateApiException("장애", null));
        ExchangeRateCache lastKnown = ExchangeRateCache.of(TX_DATE.minusDays(3), "USD", BigDecimal.valueOf(1380), null);
        given(exchangeRateCacheRepository
                .findFirstByCurrencyCodeAndRateDateLessThanEqualOrderByRateDateDesc("USD", TX_DATE))
                .willReturn(Optional.of(lastKnown));

        ExchangeRateResult result = exchangeRateService.getRate("USD", TX_DATE);

        assertThat(result.rate()).isEqualByComparingTo("1380");
        assertThat(result.estimated()).isTrue();
        then(exchangeRateApiClient).should(times(1)).fetch(any());
    }

    @Test
    @DisplayName("캐시가 완전히 비어 있으면 이 거래는 건너뛰도록 예외를 던진다")
    void throwsWhenCacheCompletelyEmpty() {
        given(exchangeRateCacheRepository.findByRateDateAndCurrencyCode(any(), eq("USD")))
                .willReturn(Optional.empty());
        given(exchangeRateApiClient.fetch(any())).willThrow(new ExchangeRateApiException("장애", null));
        given(exchangeRateCacheRepository
                .findFirstByCurrencyCodeAndRateDateLessThanEqualOrderByRateDateDesc("USD", TX_DATE))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> exchangeRateService.getRate("USD", TX_DATE))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }
}
