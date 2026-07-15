package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * biller 경유 그룹핑의 금액 사전 클러스터링(merchant.md §5-D-3, troubleshooting/billerProblem.md §6-항목2).
 * biller(Apple 등) 거래는 이름으로 신원 판별이 안 되므로, 반복 패턴 판정({@link RecurringPatternDetector})에
 * 넣기 전에 먼저 금액이 비슷한 것끼리 묶어야 한다 — 안 그러면 같은 merchant 안의 서로 다른 두 구독
 * (또는 구독+일회성 결제)이 섞여 전체가 통째로 탐지 실패한다.
 */
@Component
public class AmountClusterer {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.10");

    /** 금액 오름차순 목록을 그리디로 버킷팅한다 — 인접 금액 차이가 허용오차를 넘으면 새 버킷을 연다. */
    public List<List<CardTransaction>> cluster(List<CardTransaction> sortedByAmountAsc) {
        List<List<CardTransaction>> buckets = new ArrayList<>();
        List<CardTransaction> current = new ArrayList<>();
        BigDecimal lastAmount = null;
        for (CardTransaction tx : sortedByAmountAsc) {
            if (lastAmount != null && !withinTolerance(lastAmount, tx.getAmount())) {
                buckets.add(current);
                current = new ArrayList<>();
            }
            current.add(tx);
            lastAmount = tx.getAmount();
        }
        if (!current.isEmpty()) {
            buckets.add(current);
        }
        return buckets;
    }

    /** 기존 biller 그룹의 find-or-create 매칭에도 같은 오차를 재사용한다. */
    public boolean withinTolerance(BigDecimal a, BigDecimal b) {
        BigDecimal diffRatio = a.subtract(b).abs().divide(a, 4, RoundingMode.HALF_UP);
        return diffRatio.compareTo(TOLERANCE) <= 0;
    }
}
