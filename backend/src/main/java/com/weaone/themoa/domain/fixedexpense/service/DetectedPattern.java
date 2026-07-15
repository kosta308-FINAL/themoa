package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** {@link RecurringPatternDetector}가 찾아낸 반복결제 체인의 통계(fixedExpense.md §2). */
public record DetectedPattern(
        List<CardTransaction> transactions,
        BigDecimal avgAmount,
        BigDecimal amountVariancePct,
        short avgPayDay,
        short payDayVariance,
        LocalDate lastDetectedAt
) {
}
