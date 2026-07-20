package com.weaone.themoa.domain.recommend.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.weaone.themoa.domain.recommend.entity.SavingsProduct;
import com.weaone.themoa.domain.recommend.entity.SavingsProductOption;

/**
 * 금리 상대순위 점수 계산 - "목표 개월수에 가장 가까운 기간" 옵션 금리로 비교한다.
 * SHORT/MID/LONG 3구간으로 뭉뚱그리지 않고 정확한 개월수 차이로 비교한다
 * (예: 목표 20개월이면 12개월짜리보다 24개월짜리가 더 가깝다고 정확히 판단).
 * 점수: 상위10% +20 / 10~30% +15 / 30~50% +10 / 하위 +5.
 */
public class RateRanking {

    private final int targetMonths;
    private final List<BigDecimal> ratesDesc;

    public RateRanking(List<SavingsProduct> pool, int targetMonths) {
        this.targetMonths = targetMonths;
        this.ratesDesc = pool.stream()
                .map(p -> rateAtTarget(p, targetMonths))
                .filter(Objects::nonNull)
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /** 이 상품이 (목표 개월수 기준 금리로) 전체 풀 중 상위 N위 안에 드는지. 공격형 성향 판정에 쓴다. */
    public boolean isTopRate(SavingsProduct p, int n) {
        BigDecimal rate = rateAtTarget(p, targetMonths);
        if (rate == null) {
            return false;
        }
        long strictlyHigher = ratesDesc.stream().filter(r -> r.compareTo(rate) > 0).count();
        return strictlyHigher < n;
    }

    /** 해당 상품의 (목표 개월수 기준) 금리 순위 점수. */
    public int scoreOf(SavingsProduct p) {
        BigDecimal rate = rateAtTarget(p, targetMonths);
        if (rate == null || ratesDesc.isEmpty()) {
            return 5;
        }
        long higher = ratesDesc.stream().filter(r -> r.compareTo(rate) > 0).count();
        double percentile = (double) higher / ratesDesc.size();
        if (percentile < 0.10) {
            return 20;
        }
        if (percentile < 0.30) {
            return 15;
        }
        if (percentile < 0.50) {
            return 10;
        }
        return 5;
    }

    /**
     * 목표 개월수에 가장 가까운 옵션(정확한 개월 차이 기준).
     * 거리가 같으면 금리가 높은 것을 고른다. 기간(term) 있는 옵션이 하나도 없으면 전체 최고금리 옵션으로 대체한다.
     */
    public static SavingsProductOption targetOption(SavingsProduct p, int targetMonths) {
        SavingsProductOption best = null;
        int bestDistance = Integer.MAX_VALUE;
        BigDecimal bestRate = null;
        for (SavingsProductOption o : p.getOptions()) {
            if (o.getTermMonth() == null) {
                continue;
            }
            int distance = Math.abs(o.getTermMonth() - targetMonths);
            BigDecimal rate = rateOf(o);
            if (distance < bestDistance || (distance == bestDistance && isGreater(rate, bestRate))) {
                best = o;
                bestDistance = distance;
                bestRate = rate;
            }
        }
        return best != null ? best : globalBestOption(p);
    }

    /** 목표 개월수 기준 금리(없으면 null). */
    public static BigDecimal rateAtTarget(SavingsProduct p, int targetMonths) {
        SavingsProductOption o = targetOption(p, targetMonths);
        return o == null ? null : rateOf(o);
    }

    /** 상품의 대표 가입기간(개월) = 최고금리를 주는 옵션의 기간. 이 상품이 '어느 기간대에 최적'인지. */
    public static Integer signatureTerm(SavingsProduct p) {
        SavingsProductOption o = globalBestOption(p);
        return o == null ? null : o.getTermMonth();
    }

    private static SavingsProductOption globalBestOption(SavingsProduct p) {
        SavingsProductOption best = null;
        BigDecimal bestRate = null;
        for (SavingsProductOption o : p.getOptions()) {
            BigDecimal rate = rateOf(o);
            if (rate != null && (bestRate == null || rate.compareTo(bestRate) > 0)) {
                bestRate = rate;
                best = o;
            }
        }
        return best;
    }

    private static BigDecimal rateOf(SavingsProductOption o) {
        return o.getMaxRate() != null ? o.getMaxRate() : o.getBaseRate();
    }

    private static boolean isGreater(BigDecimal a, BigDecimal b) {
        return a != null && (b == null || a.compareTo(b) > 0);
    }
}
