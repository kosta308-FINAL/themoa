package com.weaone.themoa.domain.cardtransaction.client;

import com.weaone.themoa.config.ExchangeRateProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 한국수출입은행 환율 API(exchangeJSON) 전용 클라이언트(cardtransaction.md §4 FX-03).
 * 비영업일에는 빈 배열을 반환한다 — 소급 재조회는 호출자({@link com.weaone.themoa.domain.cardtransaction.service.ExchangeRateService})가 담당한다.
 */
@Component
@RequiredArgsConstructor
public class ExchangeRateApiClient {

    private static final DateTimeFormatter SEARCH_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestClient exchangeRateRestClient;
    private final ExchangeRateProperties exchangeRateProperties;

    public List<ExchangeRateApiRecord> fetch(LocalDate searchDate) {
        try {
            ExchangeRateApiRecord[] records = exchangeRateRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/site/program/financial/exchangeJSON")
                            .queryParam("authkey", exchangeRateProperties.authKey())
                            .queryParam("searchdate", searchDate.format(SEARCH_DATE_FORMAT))
                            .queryParam("data", "AP01")
                            .build())
                    .retrieve()
                    .body(ExchangeRateApiRecord[].class);
            return records == null ? List.of() : List.of(records);
        } catch (Exception e) {
            throw new ExchangeRateApiException("환율 API 호출에 실패했습니다.", e);
        }
    }
}
