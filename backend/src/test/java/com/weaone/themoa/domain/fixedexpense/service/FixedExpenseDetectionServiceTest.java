package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FixedExpenseDetectionServiceTest {

    private final Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE,
            LocalDate.of(2000, 1, 1), LocalDateTime.now());
    private final MerchantAlias alias = MerchantAlias.create("클로드 구독",
            Category.seed(CategoryCode.SUBSCRIPTION, "구독"));

    private FixedExpenseDetectionService detectionService() {
        return new FixedExpenseDetectionService(null, null, null, null, null, null, null, null,
                new RecurringPatternDetector(), new AmountClusterer(), null);
    }

    private CardTransaction transaction(LocalDate usedDate, BigDecimal amount) {
        CardTransaction tx = CardTransaction.sync(member, null, Category.seed(CategoryCode.SUBSCRIPTION, "구독"),
                "12345678", usedDate, usedDate.atStartOfDay(), amount, null, "KRW", null, false,
                TransactionStatus.APPROVED, null, false, alias.getCanonicalServiceName(), null, null, null, null);
        tx.assignMerchant(null, alias);
        return tx;
    }

    @Test
    @DisplayName("같은 alias에 가격이 크게 다른 월 구독 두 개가 섞여 있어도 각각 탐지한다")
    void detectsTwoSubscriptionsWithDifferentPricesUnderSameAlias() {
        List<CardTransaction> transactions = List.of(
                transaction(LocalDate.of(2026, 4, 9), new BigDecimal("22.00")),
                transaction(LocalDate.of(2026, 4, 15), new BigDecimal("100.00")),
                transaction(LocalDate.of(2026, 5, 10), new BigDecimal("22.00")),
                transaction(LocalDate.of(2026, 5, 15), new BigDecimal("100.00")),
                transaction(LocalDate.of(2026, 6, 10), new BigDecimal("22.00")),
                transaction(LocalDate.of(2026, 6, 15), new BigDecimal("100.00")));

        List<DetectedPattern> patterns = detectionService().detectPatternsByAmount(transactions);

        assertThat(patterns).hasSize(2);
        assertThat(patterns)
                .extracting(pattern -> pattern.avgAmount().stripTrailingZeros())
                .containsExactlyInAnyOrder(new BigDecimal("22"), new BigDecimal("1E+2"));
    }
}
