package com.weaone.themoa.domain.cardtransaction.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** 외화 환산액 정정(cardtransaction.md §4). 외화 원금이 있는(type1 해외) 거래에만 적용 가능하다. */
public record AmountCorrectionRequest(
        @NotNull @Positive BigDecimal amount
) {
}
