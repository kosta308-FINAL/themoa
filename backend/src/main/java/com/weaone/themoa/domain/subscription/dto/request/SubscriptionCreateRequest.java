package com.weaone.themoa.domain.subscription.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 가입 등록 요청.
 *
 * @param productId   원본 상품 id. 직접 입력 등록도 허용하려고 null을 받는다(그 경우 상품명·회사명은 사용자가 채운다)
 * @param appliedRate 사용자가 확정한 적용금리(연 %). 기본금리 + 체크한 우대조건 합계를 확인·수정한 값
 * @param compound    복리 여부. 모르면 단리(false)로 둔다
 * @param conditions  우대조건 체크리스트(확정본)
 */
public record SubscriptionCreateRequest(

        Long productId,

        @Size(max = 200, message = "상품명은 200자 이하여야 합니다.")
        String productName,

        @Size(max = 100, message = "회사명은 100자 이하여야 합니다.")
        String companyName,

        @Size(max = 20)
        String productType,

        @NotNull(message = "납입금액을 입력해 주세요.")
        @Min(value = 1000, message = "납입금액은 1,000원 이상이어야 합니다.")
        Long monthlyAmount,

        @NotNull(message = "적용금리를 입력해 주세요.")
        @DecimalMin(value = "0.0", message = "금리는 0 이상이어야 합니다.")
        @DecimalMax(value = "20.0", message = "금리는 20% 이하여야 합니다.")
        BigDecimal appliedRate,

        @NotNull(message = "가입기간을 입력해 주세요.")
        @Min(value = 1, message = "가입기간은 1개월 이상이어야 합니다.")
        @Max(value = 120, message = "가입기간은 120개월 이하여야 합니다.")
        Integer termMonth,

        boolean compound,

        @NotNull(message = "가입일을 입력해 주세요.")
        LocalDate startDate,

        @Valid
        List<ConditionInput> conditions
) {

    /** 우대조건 체크 항목. */
    public record ConditionInput(
            @Size(max = 200, message = "조건 설명은 200자 이하여야 합니다.")
            String description,
            BigDecimal rateBonus,
            boolean met
    ) {
    }
}
