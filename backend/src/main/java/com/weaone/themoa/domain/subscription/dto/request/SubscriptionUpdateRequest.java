package com.weaone.themoa.domain.subscription.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 가입한 예·적금 수정 요청. 월납입액·적용금리·기간·가입일만 바꾼다(우대조건은 별도 토글 API).
 *
 * <p>상품명·회사명 등 스냅샷과 원본 상품 id는 그대로 두고, 만기일은 서버에서 가입일+기간으로 다시 계산한다.
 */
public record SubscriptionUpdateRequest(
        @NotNull @Min(10000) Long monthlyAmount,
        @NotNull @DecimalMin("0.0") BigDecimal appliedRate,
        @Min(1) int termMonth,
        @NotNull LocalDate startDate) {
}
