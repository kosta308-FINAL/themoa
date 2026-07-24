package com.weaone.themoa.domain.subscription.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 대시보드에 보여줄 가입 상품 1건.
 *
 * @param expectedMaturityAmount 사용자 금리·납입액·기간으로 계산한 만기 예상 수령액(원, 세전 추정)
 * @param totalPrincipal         만기까지 낼 원금 합계(월납입 × 개월)
 * @param currentPrincipal       지금까지 실제로 낸 원금(월납입 × 납입회차)
 * @param currentValue           지금까지 낸 돈에 경과이자를 더한 현재 평가액(세전 추정, 중도해지이율 미반영)
 * @param unmetConditionCount    아직 충족하지 못한 우대조건 수. 0보다 크면 화면에서 강조한다
 */
public record SubscriptionResponse(
        Long id,
        Long productId,
        String productName,
        String companyName,
        String productType,
        Long monthlyAmount,
        BigDecimal appliedRate,
        int termMonth,
        boolean compound,
        LocalDate startDate,
        LocalDate maturityDate,
        Long totalPrincipal,
        Long currentPrincipal,
        Long currentValue,
        Long expectedMaturityAmount,
        int unmetConditionCount,
        List<ConditionResponse> conditions
) {

    public record ConditionResponse(Long id, String description, BigDecimal rateBonus, boolean met) {
    }
}
