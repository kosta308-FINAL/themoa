package com.weaone.themoa.domain.cardtransaction.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** 취소금액 정정(cardtransaction.md §3-4). cancel_amount_uncertain=true 건에만 적용 가능하다. */
public record CancelAmountCorrectionRequest(
        @NotNull @PositiveOrZero BigDecimal canceledAmount
) {
}
