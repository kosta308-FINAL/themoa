package com.weaone.themoa.domain.cardtransaction.service;

import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.merchant.service.MemberMerchantLabelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link CardTransactionResponse}로 변환할 때 회원 개인 표시명 오버라이드({@link MemberMerchantLabelService})를
 * 항상 같이 반영한다. 목록 변환은 한 번에 조회해 N+1을 피한다.
 */
@Component
@RequiredArgsConstructor
public class CardTransactionResponseMapper {

    private final MemberMerchantLabelService memberMerchantLabelService;

    public CardTransactionResponse map(Long memberId, CardTransaction transaction) {
        Long aliasId = aliasIdOf(transaction);
        String label = aliasId == null ? null : memberMerchantLabelService.resolveLabel(memberId, aliasId).orElse(null);
        return CardTransactionResponse.from(transaction, label);
    }

    public List<CardTransactionResponse> mapList(Long memberId, List<CardTransaction> transactions) {
        Map<Long, String> labels = labelsFor(memberId, transactions);
        return transactions.stream().map(transaction -> mapWithLabels(transaction, labels)).toList();
    }

    public Page<CardTransactionResponse> mapPage(Long memberId, Page<CardTransaction> page) {
        Map<Long, String> labels = labelsFor(memberId, page.getContent());
        return page.map(transaction -> mapWithLabels(transaction, labels));
    }

    private Map<Long, String> labelsFor(Long memberId, List<CardTransaction> transactions) {
        List<Long> aliasIds = transactions.stream()
                .map(this::aliasIdOf)
                .filter(Objects::nonNull)
                .toList();
        return memberMerchantLabelService.resolveLabels(memberId, aliasIds);
    }

    private CardTransactionResponse mapWithLabels(CardTransaction transaction, Map<Long, String> labels) {
        Long aliasId = aliasIdOf(transaction);
        return CardTransactionResponse.from(transaction, aliasId == null ? null : labels.get(aliasId));
    }

    private Long aliasIdOf(CardTransaction transaction) {
        return transaction.getMerchantAlias() != null ? transaction.getMerchantAlias().getId() : null;
    }
}
