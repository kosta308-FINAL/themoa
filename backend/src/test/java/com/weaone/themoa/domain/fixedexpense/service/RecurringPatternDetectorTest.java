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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** fixedExpense.md §2 탐지 규칙(3회 이상/28~33일/±10%) 검증. 실데이터 검출 사례를 재현한다. */
class RecurringPatternDetectorTest {

    private final RecurringPatternDetector detector = new RecurringPatternDetector();

    private Member member() {
        return Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
    }

    private MerchantAlias alias(String name) {
        return MerchantAlias.create(name, Category.seed(CategoryCode.SUBSCRIPTION, "구독"));
    }

    private CardTransaction transaction(MerchantAlias alias, LocalDate usedDate, BigDecimal amount) {
        CardTransaction tx = CardTransaction.sync(member(), null, Category.seed(CategoryCode.SUBSCRIPTION, "구독"),
                "12345678", usedDate, usedDate.atStartOfDay(), amount, null, "KRW", null, false,
                TransactionStatus.APPROVED, null, false, alias.getCanonicalServiceName(), null, null, null, null);
        tx.assignMerchant(null, alias);
        return tx;
    }

    @Test
    @DisplayName("결제가 2회뿐이면 탐지되지 않는다")
    void notDetectedWhenFewerThanThreePayments() {
        MerchantAlias alias = alias("웨이브");
        List<CardTransaction> transactions = List.of(
                transaction(alias, LocalDate.of(2026, 4, 9), BigDecimal.valueOf(22)),
                transaction(alias, LocalDate.of(2026, 5, 10), BigDecimal.valueOf(22)));

        Optional<DetectedPattern> result = detector.detect(transactions);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("CLAUDE.AI SUBSCRIPTION 실데이터 사례: 4/9·5/10·6/10, $22.00 고정 → 탐지된다")
    void detectsClaudeSubscriptionPattern() {
        MerchantAlias alias = alias("클로드 구독");
        List<CardTransaction> transactions = List.of(
                transaction(alias, LocalDate.of(2026, 4, 9), new BigDecimal("22.00")),
                transaction(alias, LocalDate.of(2026, 5, 10), new BigDecimal("22.00")),
                transaction(alias, LocalDate.of(2026, 6, 10), new BigDecimal("22.00")));

        Optional<DetectedPattern> result = detector.detect(transactions);

        assertThat(result).isPresent();
        DetectedPattern pattern = result.get();
        assertThat(pattern.transactions()).hasSize(3);
        assertThat(pattern.avgAmount()).isEqualByComparingTo("22.00");
        assertThat(pattern.lastDetectedAt()).isEqualTo(LocalDate.of(2026, 6, 10));
    }

    @Test
    @DisplayName("쿠팡와우 실데이터 사례: 3/22·4/22·5/22·6/22, 7,890원 고정 → 탐지된다")
    void detectsCoupangWowPattern() {
        MerchantAlias alias = alias("쿠팡와우");
        List<CardTransaction> transactions = List.of(
                transaction(alias, LocalDate.of(2026, 3, 22), BigDecimal.valueOf(7890)),
                transaction(alias, LocalDate.of(2026, 4, 22), BigDecimal.valueOf(7890)),
                transaction(alias, LocalDate.of(2026, 5, 22), BigDecimal.valueOf(7890)),
                transaction(alias, LocalDate.of(2026, 6, 22), BigDecimal.valueOf(7890)));

        Optional<DetectedPattern> result = detector.detect(transactions);

        assertThat(result).isPresent();
        assertThat(result.get().transactions()).hasSize(4);
        assertThat(result.get().avgPayDay()).isEqualTo((short) 22);
    }

    @Test
    @DisplayName("대한적십자사 실데이터 사례: 3/3·4/1·5/4(29일·33일 간격), 30,000원 고정 → 탐지된다")
    void detectsRedCrossDonationPattern() {
        MerchantAlias alias = alias("대한적십자사");
        List<CardTransaction> transactions = List.of(
                transaction(alias, LocalDate.of(2026, 3, 3), BigDecimal.valueOf(30000)),
                transaction(alias, LocalDate.of(2026, 4, 1), BigDecimal.valueOf(30000)),
                transaction(alias, LocalDate.of(2026, 5, 4), BigDecimal.valueOf(30000)));

        Optional<DetectedPattern> result = detector.detect(transactions);

        assertThat(result).isPresent();
        assertThat(result.get().transactions()).hasSize(3);
    }

    @Test
    @DisplayName("간격이 27일이면(28일 미만) 탐지되지 않는다")
    void notDetectedWhenIntervalTooShort() {
        MerchantAlias alias = alias("웨이브");
        List<CardTransaction> transactions = List.of(
                transaction(alias, LocalDate.of(2026, 4, 1), BigDecimal.valueOf(6300)),
                transaction(alias, LocalDate.of(2026, 4, 28), BigDecimal.valueOf(6300)),
                transaction(alias, LocalDate.of(2026, 5, 25), BigDecimal.valueOf(6300)));

        Optional<DetectedPattern> result = detector.detect(transactions);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("간격이 34일이면(33일 초과) 탐지되지 않는다")
    void notDetectedWhenIntervalTooLong() {
        MerchantAlias alias = alias("웨이브");
        List<CardTransaction> transactions = List.of(
                transaction(alias, LocalDate.of(2026, 4, 1), BigDecimal.valueOf(6300)),
                transaction(alias, LocalDate.of(2026, 5, 5), BigDecimal.valueOf(6300)),
                transaction(alias, LocalDate.of(2026, 6, 8), BigDecimal.valueOf(6300)));

        Optional<DetectedPattern> result = detector.detect(transactions);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("금액이 평균 대비 10%를 초과해 벗어나면 탐지되지 않는다")
    void notDetectedWhenAmountVarianceExceedsTolerance() {
        MerchantAlias alias = alias("웨이브");
        List<CardTransaction> transactions = List.of(
                transaction(alias, LocalDate.of(2026, 4, 1), BigDecimal.valueOf(6300)),
                transaction(alias, LocalDate.of(2026, 5, 1), BigDecimal.valueOf(6300)),
                transaction(alias, LocalDate.of(2026, 6, 1), BigDecimal.valueOf(8000)));

        Optional<DetectedPattern> result = detector.detect(transactions);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("금액이 평균 대비 10% 이내면(오차 허용) 탐지된다")
    void detectedWhenAmountWithinTolerance() {
        MerchantAlias alias = alias("웨이브");
        List<CardTransaction> transactions = List.of(
                transaction(alias, LocalDate.of(2026, 4, 1), BigDecimal.valueOf(6300)),
                transaction(alias, LocalDate.of(2026, 5, 1), BigDecimal.valueOf(6300)),
                transaction(alias, LocalDate.of(2026, 6, 1), BigDecimal.valueOf(6800)));

        Optional<DetectedPattern> result = detector.detect(transactions);

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("일회성 결제가 섞여 있어도 최근 3회 이상이 조건을 만족하면 탐지된다")
    void detectsTrailingChainAfterAGap() {
        MerchantAlias alias = alias("Apple");
        List<CardTransaction> transactions = List.of(
                transaction(alias, LocalDate.of(2026, 1, 5), BigDecimal.valueOf(15000)), // 일회성, 간격 무관
                transaction(alias, LocalDate.of(2026, 3, 21), BigDecimal.valueOf(6300)),
                transaction(alias, LocalDate.of(2026, 4, 22), BigDecimal.valueOf(6300)),
                transaction(alias, LocalDate.of(2026, 5, 21), BigDecimal.valueOf(6300)),
                transaction(alias, LocalDate.of(2026, 6, 21), BigDecimal.valueOf(6300)));

        Optional<DetectedPattern> result = detector.detect(transactions);

        assertThat(result).isPresent();
        assertThat(result.get().transactions()).hasSize(4);
        assertThat(result.get().transactions()).doesNotContain(transactions.get(0));
    }
}
