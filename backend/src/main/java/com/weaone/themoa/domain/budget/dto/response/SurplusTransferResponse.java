package com.weaone.themoa.domain.budget.dto.response;

import com.weaone.themoa.domain.budget.entity.SurplusFundTransfer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 이번 급여 주기로 가져온 잉여금 1건과 처리 후 사용 가능 잔액. */
public record SurplusTransferResponse(
        Long id,
        BigDecimal amount,
        BigDecimal availableSurplusAmount,
        LocalDateTime createdAt) {

    public static SurplusTransferResponse from(SurplusFundTransfer transfer, BigDecimal availableSurplusAmount) {
        return new SurplusTransferResponse(
                transfer.getId(), transfer.getAmount(), availableSurplusAmount, transfer.getCreatedAt());
    }
}
