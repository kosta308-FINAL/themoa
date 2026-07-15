package com.weaone.themoa.domain.fixedexpense.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 원화 환산 결과(fixedExpense.md §4·§5). 국내(KRW)는 {@code convertedDate}·{@code exchangeRate}가 NULL이다. */
public record ConvertedKrwAmount(BigDecimal krwAmount, LocalDate convertedDate, BigDecimal exchangeRate) {
}
