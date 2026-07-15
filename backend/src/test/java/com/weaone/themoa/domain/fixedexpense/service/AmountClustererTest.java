package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** biller 그룹핑의 금액 사전 클러스터링(merchant.md §5-D-3) 검증. */
class AmountClustererTest {

    private final AmountClusterer clusterer = new AmountClusterer();

    private Member member() {
        return Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
    }

    private CardTransaction transaction(BigDecimal amount) {
        return CardTransaction.sync(member(), null, Category.seed(CategoryCode.SUBSCRIPTION, "구독"),
                "12345678", LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10).atStartOfDay(), amount, null,
                "KRW", null, false, TransactionStatus.APPROVED, null, false, "Apple", null, null, null, null);
    }

    @Test
    @DisplayName("모든 금액이 서로 10% 이내면 버킷 1개로 묶인다")
    void singleBucketWhenAllAmountsSimilar() {
        List<CardTransaction> sorted = List.of(
                transaction(BigDecimal.valueOf(6300)),
                transaction(BigDecimal.valueOf(6300)),
                transaction(BigDecimal.valueOf(6300)));

        List<List<CardTransaction>> buckets = clusterer.cluster(sorted);

        assertThat(buckets).hasSize(1);
        assertThat(buckets.get(0)).hasSize(3);
    }

    @Test
    @DisplayName("금액이 크게 다른 두 구독은 서로 다른 버킷으로 분리된다")
    void separatesTwoDifferentSubscriptions() {
        // Apple 경유 웨이브(6,300원)와 넷플릭스(9,900원)가 섞인 경우
        List<CardTransaction> sorted = List.of(
                transaction(BigDecimal.valueOf(6300)),
                transaction(BigDecimal.valueOf(6300)),
                transaction(BigDecimal.valueOf(6300)),
                transaction(BigDecimal.valueOf(9900)),
                transaction(BigDecimal.valueOf(9900)),
                transaction(BigDecimal.valueOf(9900)));

        List<List<CardTransaction>> buckets = clusterer.cluster(sorted);

        assertThat(buckets).hasSize(2);
        assertThat(buckets.get(0)).hasSize(3);
        assertThat(buckets.get(1)).hasSize(3);
    }

    @Test
    @DisplayName("정기결제와 섞인 일회성 고액 결제는 별도 버킷으로 빠진다")
    void oneOffPaymentFormsSeparateBucket() {
        List<CardTransaction> sorted = List.of(
                transaction(BigDecimal.valueOf(6300)),
                transaction(BigDecimal.valueOf(6300)),
                transaction(BigDecimal.valueOf(6300)),
                transaction(BigDecimal.valueOf(15000)),
                transaction(BigDecimal.valueOf(22000)));

        List<List<CardTransaction>> buckets = clusterer.cluster(sorted);

        assertThat(buckets).hasSize(3);
        assertThat(buckets.get(0)).hasSize(3);
    }

    @Test
    @DisplayName("빈 목록이면 버킷도 비어있다")
    void emptyInputProducesNoBuckets() {
        assertThat(clusterer.cluster(List.of())).isEmpty();
    }

    @Test
    @DisplayName("허용오차 10% 경계값은 같은 버킷으로 묶인다")
    void toleranceBoundaryStaysInSameBucket() {
        List<CardTransaction> sorted = List.of(
                transaction(BigDecimal.valueOf(10000)),
                transaction(BigDecimal.valueOf(11000))); // 10000 대비 정확히 +10%

        List<List<CardTransaction>> buckets = clusterer.cluster(sorted);

        assertThat(buckets).hasSize(1);
    }
}
