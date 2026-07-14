package com.weaone.themoa.domain.cardtransaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 수출입은행 환율(매매기준율) DB 영속 캐시(cardtransaction.md §4). 서버 메모리 캐시가 아니라 DB에 두는 이유는
 * 재시작 직후에도 폴백 환율을 잃지 않기 위해서다. {@code rateDate}는 실제 고시일(searchdate)이며, 비영업일
 * 소급 재조회로 채워진 경우 원 거래일과 다를 수 있다.
 */
@Entity
@Table(name = "exchange_rate_cache",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rate_date", "currency_code"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExchangeRateCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "deal_base_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal dealBaseRate;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    private ExchangeRateCache(LocalDate rateDate, String currencyCode, BigDecimal dealBaseRate, LocalDateTime fetchedAt) {
        this.rateDate = rateDate;
        this.currencyCode = currencyCode;
        this.dealBaseRate = dealBaseRate;
        this.fetchedAt = fetchedAt;
    }

    public static ExchangeRateCache of(LocalDate rateDate, String currencyCode, BigDecimal dealBaseRate, LocalDateTime fetchedAt) {
        return new ExchangeRateCache(rateDate, currencyCode, dealBaseRate, fetchedAt);
    }
}
