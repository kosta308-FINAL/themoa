package com.weaone.themoa.domain.cardtransaction.dto.response;

public record CategoryPhaseResponse(
        AmountPercentageResponse early,
        AmountPercentageResponse middle,
        AmountPercentageResponse late
) {
}
