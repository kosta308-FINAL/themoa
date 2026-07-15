package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 반복결제 패턴 판정(fixedExpense.md §2, ML 불필요 규칙 기반): 최근 거래부터 거꾸로 훑어 결제 간격이
 * 28~33일인 동안 체인을 이어 붙이고, 체인이 3건 이상이면서 금액이 평균 ±10% 이내일 때만 인정한다.
 * 취소 건은 호출자가 미리 제외한 목록을 넘긴다(§2 규칙 5).
 */
@Component
public class RecurringPatternDetector {

    private static final int MIN_REPEAT_COUNT = 3;
    private static final long MIN_INTERVAL_DAYS = 28;
    private static final long MAX_INTERVAL_DAYS = 33;
    private static final BigDecimal AMOUNT_TOLERANCE_RATIO = new BigDecimal("0.10");

    /** @param transactionsAscByDate 취소 건이 제외된, 사용일자 오름차순 거래 목록(같은 회원·같은 alias). */
    public Optional<DetectedPattern> detect(List<CardTransaction> transactionsAscByDate) {
        if (transactionsAscByDate.size() < MIN_REPEAT_COUNT) {
            return Optional.empty();
        }

        List<CardTransaction> chain = new ArrayList<>();
        chain.add(transactionsAscByDate.get(transactionsAscByDate.size() - 1));
        for (int i = transactionsAscByDate.size() - 2; i >= 0; i--) {
            CardTransaction earlier = transactionsAscByDate.get(i);
            CardTransaction latestInChain = chain.get(chain.size() - 1);
            long gapDays = ChronoUnit.DAYS.between(earlier.getUsedDate(), latestInChain.getUsedDate());
            if (gapDays < MIN_INTERVAL_DAYS || gapDays > MAX_INTERVAL_DAYS) {
                break;
            }
            chain.add(earlier);
        }
        if (chain.size() < MIN_REPEAT_COUNT) {
            return Optional.empty();
        }
        Collections.reverse(chain);

        BigDecimal avgAmount = average(chain);
        BigDecimal maxVariancePct = BigDecimal.ZERO;
        for (CardTransaction tx : chain) {
            BigDecimal diffRatio = tx.getAmount().subtract(avgAmount).abs()
                    .divide(avgAmount, 4, RoundingMode.HALF_UP);
            if (diffRatio.compareTo(AMOUNT_TOLERANCE_RATIO) > 0) {
                return Optional.empty();
            }
            maxVariancePct = maxVariancePct.max(diffRatio.multiply(BigDecimal.valueOf(100)));
        }

        int avgPayDaySum = chain.stream().mapToInt(tx -> tx.getUsedDate().getDayOfMonth()).sum();
        short avgPayDay = (short) Math.round(avgPayDaySum / (double) chain.size());
        short payDayVariance = (short) chain.stream()
                .mapToInt(tx -> Math.abs(tx.getUsedDate().getDayOfMonth() - avgPayDay))
                .max().orElse(0);

        return Optional.of(new DetectedPattern(chain, avgAmount, maxVariancePct.setScale(2, RoundingMode.HALF_UP),
                avgPayDay, payDayVariance, chain.get(chain.size() - 1).getUsedDate()));
    }

    private BigDecimal average(List<CardTransaction> chain) {
        BigDecimal sum = chain.stream().map(CardTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(chain.size()), 2, RoundingMode.HALF_UP);
    }
}
