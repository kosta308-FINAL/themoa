package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.domain.cardtransaction.client.ExchangeRateApiClient;
import com.weaone.themoa.domain.cardtransaction.client.ExchangeRateApiException;
import com.weaone.themoa.domain.cardtransaction.client.ExchangeRateApiRecord;
import com.weaone.themoa.domain.cardtransaction.entity.ExchangeRateCache;
import com.weaone.themoa.domain.cardtransaction.repository.ExchangeRateCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 한국수출입은행 환율 조회(cardtransaction.md §4 FX-03). 거래일 기준 매매기준율을 DB 캐시 우선으로 구하고,
 * 비영업일은 최대 7일 소급하며, API 자체가 실패하면 캐시의 직전 성공 환율(폴백)로 대체한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final int MAX_LOOKBACK_DAYS = 7;
    private static final String UNIT_DIVISOR_MARKER = "(100)";

    private final ExchangeRateApiClient exchangeRateApiClient;
    private final ExchangeRateCacheRepository exchangeRateCacheRepository;

    @Transactional
    public ExchangeRateResult getRate(String currencyCode, LocalDate transactionDate) {
        for (int offset = 0; offset <= MAX_LOOKBACK_DAYS; offset++) {
            LocalDate candidate = transactionDate.minusDays(offset);

            Optional<ExchangeRateCache> cached = exchangeRateCacheRepository
                    .findByRateDateAndCurrencyCode(candidate, currencyCode);
            if (cached.isPresent()) {
                return new ExchangeRateResult(cached.get().getDealBaseRate(), false);
            }

            List<ExchangeRateApiRecord> records;
            try {
                records = exchangeRateApiClient.fetch(candidate);
            } catch (ExchangeRateApiException e) {
                log.warn("환율 API 호출 실패, 폴백 환율로 전환합니다. currency={}", currencyCode, e);
                break;
            }

            Optional<BigDecimal> rate = extractRate(records, currencyCode);
            if (rate.isPresent()) {
                saveCache(candidate, currencyCode, rate.get());
                return new ExchangeRateResult(rate.get(), false);
            }
            // 빈 응답 = 비영업일. 하루 더 소급한다.
        }
        return fallback(currencyCode, transactionDate);
    }

    private ExchangeRateResult fallback(String currencyCode, LocalDate transactionDate) {
        return exchangeRateCacheRepository
                .findFirstByCurrencyCodeAndRateDateLessThanEqualOrderByRateDateDesc(currencyCode, transactionDate)
                .map(cache -> new ExchangeRateResult(cache.getDealBaseRate(), true))
                .orElseThrow(() -> new ExchangeRateUnavailableException(
                        "환율 캐시가 비어 있어 환율을 구할 수 없습니다: " + currencyCode));
    }

    private Optional<BigDecimal> extractRate(List<ExchangeRateApiRecord> records, String currencyCode) {
        return records.stream()
                .filter(record -> record.result() == 1)
                .filter(record -> matchesCurrency(record.curUnit(), currencyCode))
                .findFirst()
                .map(record -> parseRate(record.dealBasR(), record.curUnit()));
    }

    private boolean matchesCurrency(String curUnit, String currencyCode) {
        return curUnit != null && normalizeCurrencyUnit(curUnit).equalsIgnoreCase(currencyCode);
    }

    private String normalizeCurrencyUnit(String curUnit) {
        int idx = curUnit.indexOf('(');
        return idx < 0 ? curUnit.trim() : curUnit.substring(0, idx).trim();
    }

    private BigDecimal parseRate(String dealBasR, String curUnit) {
        BigDecimal rate = new BigDecimal(dealBasR.replace(",", "").trim());
        if (curUnit.contains(UNIT_DIVISOR_MARKER)) {
            rate = rate.divide(BigDecimal.valueOf(100));
        }
        return rate;
    }

    private void saveCache(LocalDate rateDate, String currencyCode, BigDecimal rate) {
        try {
            exchangeRateCacheRepository.save(ExchangeRateCache.of(rateDate, currencyCode, rate, LocalDateTime.now()));
        } catch (DataIntegrityViolationException e) {
            log.debug("환율 캐시 동시 저장 경합, 기존 값을 사용합니다. rateDate={}, currency={}", rateDate, currencyCode);
        }
    }
}
