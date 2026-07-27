package com.weaone.themoa.domain.budget.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** 완료 주기에서 쌓인 사용 가능 잉여금을 이번 급여 주기 예산으로 가져온다. */
public record SurplusTransferCreateRequest(
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount) {
}
