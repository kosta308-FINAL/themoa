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
 *
 * <p>최신 거래를 기준점으로 삼는 이유: 이미 해지된 구독이 과거 한때 규칙적으로 결제됐다는 이유만으로
 * 영원히 후보 재추천되는 것을 막기 위함이다(과거 거래부터 훑으면 이 앵커링이 사라진다). 다만 맨 끝
 * 거래 하나가 체인에 전혀 못 낄 때(예: 같은 alias로 별도 구독이 하나 더 붙어 우연히 최근에 낀 경우)
 * 그 거래 하나 때문에 뒤에 멀쩡히 있는 패턴 전체를 놓치지 않도록, 기준점을 한 칸씩 물려가며 재시도한다.
 */
@Component
public class RecurringPatternDetector {

    private static final int MIN_REPEAT_COUNT = 3;
    private static final long MIN_INTERVAL_DAYS = 28;
    private static final long MAX_INTERVAL_DAYS = 33;
    private static final BigDecimal AMOUNT_TOLERANCE_RATIO = new BigDecimal("0.10");

    /** @param transactionsAscByDate 취소 건이 제외된, 사용일자 오름차순 거래 목록(같은 회원·같은 alias). */
    public Optional<DetectedPattern> detect(List<CardTransaction> transactionsAscByDate) {
        for (int endIndex = transactionsAscByDate.size() - 1; endIndex >= MIN_REPEAT_COUNT - 1; endIndex--) {
            Optional<DetectedPattern> pattern = buildChainEndingAt(transactionsAscByDate, endIndex);
            if (pattern.isPresent()) {
                return pattern;
            }
        }
        return Optional.empty();
    }

    /**
     * 같은 alias 안에 서로 다른 구독이 여러 개 섞여 있을 수 있어(예: 같은 서비스를 계정 두 개로 구독)
     * 하나 찾으면 그 거래들을 빼고 남은 거래로 다시 찾기를 반복한다. 금액이 같은 구독끼리도 결제일
     * 간격만으로 자연히 갈린다 — 사전 클러스터링(금액 등) 없이 {@link #detect}를 그대로 재사용한다.
     */
    public List<DetectedPattern> detectAll(List<CardTransaction> transactionsAscByDate) {
        List<DetectedPattern> patterns = new ArrayList<>();
        List<CardTransaction> remaining = new ArrayList<>(transactionsAscByDate);
        Optional<DetectedPattern> found;
        while ((found = detect(remaining)).isPresent()) {
            DetectedPattern pattern = found.get();
            patterns.add(pattern);
            remaining.removeAll(pattern.transactions());
        }
        return patterns;
    }

    /** endIndex를 기준점 삼아 뒤로 체인을 이어 붙인다. 기준점이 아무와도 안 이어지면 호출자가 그 앞 거래로 재시도한다. */
    private Optional<DetectedPattern> buildChainEndingAt(List<CardTransaction> transactionsAscByDate, int endIndex) {
        List<CardTransaction> chain = new ArrayList<>();
        chain.add(transactionsAscByDate.get(endIndex));
        for (int i = endIndex - 1; i >= 0; i--) {
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
