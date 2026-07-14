package com.weaone.themoa.domain.cardtransaction.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record CardTransactionListResponse(
        List<CardTransactionResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static CardTransactionListResponse from(Page<CardTransactionResponse> page) {
        return new CardTransactionListResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
