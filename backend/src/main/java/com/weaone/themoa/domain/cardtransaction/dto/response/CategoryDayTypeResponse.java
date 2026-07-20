package com.weaone.themoa.domain.cardtransaction.dto.response;

public record CategoryDayTypeResponse(
        AmountPercentageResponse weekday,
        AmountPercentageResponse weekend
) {
}
