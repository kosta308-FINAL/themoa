package com.weaone.themoa.domain.budget.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** 수입 직접 입력 생성. amount는 양수(추가 수입)·음수(차액 보정) 둘 다 허용하며 0은 서비스에서 거부한다. */
public record IncomeAdjustmentCreateRequest(
        @NotNull BigDecimal amount,
        @Size(max = 255) String memo) {
}
