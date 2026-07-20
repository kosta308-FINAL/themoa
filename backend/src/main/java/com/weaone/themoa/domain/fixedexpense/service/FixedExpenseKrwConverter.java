package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.cardtransaction.service.ExchangeRateResult;
import com.weaone.themoa.domain.cardtransaction.service.ExchangeRateService;
import com.weaone.themoa.domain.cardtransaction.service.ExchangeRateUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 고정지출 예상 금액을 예산 차감용 원화 스냅샷으로 환산한다(fixedExpense.md §5, erd.md §5). 환산 시점이
 * 두 곳(등록 즉시 · 주기 시작 배치)이라 규칙이 반복되므로 공용화한다. 환율을 못 구하면
 * {@link ExchangeRateUnavailableException}을 그대로 던지고, 등록을 막을지 이번 갱신만 건너뛸지는 호출자가 정한다.
 */
@Component
@RequiredArgsConstructor
public class FixedExpenseKrwConverter {

    static final String CURRENCY_KRW = "KRW";

    private final ExchangeRateService exchangeRateService;

    /** 국내(KRW)는 원본 그대로, 해외는 기준일 환율로 환산한다. */
    public ConvertedKrwAmount convert(BigDecimal amount, String currency, LocalDate conversionDate) {
        if (CURRENCY_KRW.equals(currency)) {
            return new ConvertedKrwAmount(amount, null, null);
        }
        ExchangeRateResult rate = exchangeRateService.getRate(currency, conversionDate);
        BigDecimal krwAmount = amount.multiply(rate.rate()).setScale(2, RoundingMode.HALF_UP);
        return new ConvertedKrwAmount(krwAmount, conversionDate, rate.rate());
    }
}
