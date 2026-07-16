package com.weaone.themoa.domain.fixedexpense.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * 경로 A: 추천 승인(F-02 "등록" → F-03 프리필 확정). 결제수단은 이미 후보에서 확정돼 바꿀 수 없다.
 *
 * <p>이름형 후보는 그룹이 이미 alias를 알고 있어 {@code merchantAliasId}·{@code newMerchantAliasName}이
 * 둘 다 필요 없다. biller 후보(troubleshooting/billerProblem.md)는 그룹에 alias가 없어 승인 시점에
 * 사용자가 이름을 지어야 한다 — 기존 alias를 고르거나(merchantAliasId) 새로 만든다(newMerchantAliasName).
 */
public record FixedExpenseCandidateRegisterRequest(
        @NotBlank String name,
        Long categoryId,
        Long merchantAliasId,
        String newMerchantAliasName,
        @NotNull @Positive BigDecimal expectedAmount,
        String expectedCurrency,
        @NotNull @Min(1) @Max(31) Short expectedPayDay
) {
}
