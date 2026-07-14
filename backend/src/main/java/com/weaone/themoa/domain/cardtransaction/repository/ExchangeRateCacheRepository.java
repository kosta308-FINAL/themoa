package com.weaone.themoa.domain.cardtransaction.repository;

import com.weaone.themoa.domain.cardtransaction.entity.ExchangeRateCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateCacheRepository extends JpaRepository<ExchangeRateCache, Long> {

    Optional<ExchangeRateCache> findByRateDateAndCurrencyCode(LocalDate rateDate, String currencyCode);

    /** 폴백 조회: 해당 통화의 rateDate 이하 중 가장 최근 행(cardtransaction.md §4). */
    Optional<ExchangeRateCache> findFirstByCurrencyCodeAndRateDateLessThanEqualOrderByRateDateDesc(
            String currencyCode, LocalDate rateDate);
}
