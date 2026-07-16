package com.weaone.themoa.domain.fixedexpense.dto.response;

import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidate;
import com.weaone.themoa.domain.fixedexpense.entity.RecurringPaymentGroup;
import com.weaone.themoa.domain.merchant.entity.Merchant;

import java.math.BigDecimal;

public record FixedExpenseCandidateResponse(
        Long id,
        Long recurringGroupId,
        String merchantAliasName,
        BigDecimal avgAmount,
        short avgPayDay,
        Long recommendedCategoryId,
        String recommendMessage,
        String status
) {

    public static FixedExpenseCandidateResponse from(FixedExpenseCandidate candidate) {
        RecurringPaymentGroup group = candidate.getRecurringPaymentGroup();
        String displayName = group.isBillerGroup()
                ? billerDisplayName(group.getBillerMerchant())
                : group.getMerchantAlias().getCanonicalServiceName();
        return new FixedExpenseCandidateResponse(
                candidate.getId(),
                group.getId(),
                displayName,
                group.getAvgAmount(),
                group.getAvgPayDay(),
                candidate.getRecommendedCategory().getId(),
                candidate.getRecommendMessage(),
                candidate.getStatus().name()
        );
    }

    /** biller형은 alias가 아직 없다(승인 시점에야 사용자가 이름을 짓는다) — 결제대행사 표시명으로 대신 보여준다. */
    private static String billerDisplayName(Merchant billerMerchant) {
        return billerMerchant.getDisplayName() != null
                ? billerMerchant.getDisplayName()
                : billerMerchant.getMerchantNameRaw();
    }
}
