package com.weaone.themoa.domain.cardtransaction.entity;

import com.weaone.themoa.domain.cardconnection.entity.Card;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CardTransactionTest {

    private Member member() {
        return Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1));
    }

    private Category category(CategoryCode code) {
        return Category.seed(code, code.name());
    }

    private CardTransaction approvedTransaction(BigDecimal amount) {
        return CardTransaction.sync(member(), null, category(CategoryCode.SUBSCRIPTION), "12345678",
                LocalDate.of(2026, 6, 10), LocalDateTime.of(2026, 6, 10, 10, 30), amount, null, "KRW", null, false,
                TransactionStatus.APPROVED, null, false, "ANTHROPIC* CLAUDE SUB", null, null, null);
    }

    @Test
    @DisplayName("취소금액이 없으면 실지출은 amount 그대로다")
    void netAmountWithoutCancellation() {
        CardTransaction transaction = approvedTransaction(BigDecimal.valueOf(30000));

        assertThat(transaction.getNetAmount()).isEqualByComparingTo("30000");
    }

    @Test
    @DisplayName("실지출 = amount - canceledAmount")
    void netAmountWithPartialCancellation() {
        CardTransaction transaction = approvedTransaction(BigDecimal.valueOf(30000));
        transaction.reconcileOnResync(TransactionStatus.PARTIAL_CANCELED, BigDecimal.valueOf(10000), false,
                transaction.getAmount(), null, false, null, null);

        assertThat(transaction.getNetAmount()).isEqualByComparingTo("20000");
    }

    @Test
    @DisplayName("재수집은 사용자가 정정한 취소금액을 덮어쓰지 않는다")
    void reconcileDoesNotOverwriteUserCorrectedCancelAmount() {
        CardTransaction transaction = approvedTransaction(BigDecimal.valueOf(30000));
        transaction.reconcileOnResync(TransactionStatus.PARTIAL_CANCELED, null, true,
                transaction.getAmount(), null, false, null, null);
        transaction.correctCanceledAmount(BigDecimal.valueOf(9700));

        // 재수집이 카드사 추정값(우연히 다른 값)으로 되돌리려 해도 사용자 정정값이 보존된다.
        transaction.reconcileOnResync(TransactionStatus.PARTIAL_CANCELED, BigDecimal.valueOf(15000), true,
                transaction.getAmount(), null, false, null, null);

        assertThat(transaction.getCanceledAmount()).isEqualByComparingTo("9700");
        assertThat(transaction.isCancelAmountUncertain()).isFalse();
        // status는 카드사가 나중에 바꾸는 사실이라 계속 갱신된다.
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PARTIAL_CANCELED);
    }

    @Test
    @DisplayName("재수집은 사용자가 정정한 환산액을 덮어쓰지 않는다")
    void reconcileDoesNotOverwriteUserCorrectedAmount() {
        CardTransaction transaction = CardTransaction.sync(member(), null, category(CategoryCode.SUBSCRIPTION),
                "590688", LocalDate.of(2026, 6, 10), LocalDateTime.of(2026, 6, 10, 10, 30),
                BigDecimal.valueOf(29500), BigDecimal.valueOf(22.00), "USD", BigDecimal.valueOf(1340.9091), false,
                TransactionStatus.APPROVED, null, false, "ANTHROPIC* CLAUDE SUB", null, null, null);
        transaction.correctAmount(BigDecimal.valueOf(30200));

        transaction.reconcileOnResync(TransactionStatus.APPROVED, null, false,
                BigDecimal.valueOf(29800), BigDecimal.valueOf(1354.5), true, null, null);

        assertThat(transaction.getAmount()).isEqualByComparingTo("30200");
        assertThat(transaction.isAmountUserCorrected()).isTrue();
    }

    @Test
    @DisplayName("재수집은 카테고리를 건드리지 않는다 — 호출자가 애초에 category를 넘기지 않는다")
    void reconcileNeverTouchesCategory() {
        CardTransaction transaction = approvedTransaction(BigDecimal.valueOf(30000));
        Category original = transaction.getCategory();

        transaction.reconcileOnResync(TransactionStatus.CANCELED, transaction.getAmount(), false,
                transaction.getAmount(), null, false, null, null);

        assertThat(transaction.getCategory()).isEqualTo(original);
        assertThat(transaction.isCategoryUserCorrected()).isFalse();
    }

    @Test
    @DisplayName("건별 카테고리 수정 시 사용자정정 플래그가 세워진다")
    void correctCategorySetsFlag() {
        CardTransaction transaction = approvedTransaction(BigDecimal.valueOf(6300));
        Category corrected = category(CategoryCode.LEISURE);
        ReflectionTestUtils.setField(corrected, "id", 8L);

        transaction.correctCategory(corrected);

        assertThat(transaction.getCategory()).isEqualTo(corrected);
        assertThat(transaction.isCategoryUserCorrected()).isTrue();
    }

    @Test
    @DisplayName("신원 해석 결과는 merchant/merchantAlias 양쪽에 반영된다")
    void assignMerchantSetsBothFields() {
        CardTransaction transaction = approvedTransaction(BigDecimal.valueOf(6300));
        Merchant merchant = Merchant.observe("Apple", null);
        MerchantAlias alias = MerchantAlias.create("웨이브", null);

        transaction.assignMerchant(merchant, alias);

        assertThat(transaction.getMerchant()).isEqualTo(merchant);
        assertThat(transaction.getMerchantAlias()).isEqualTo(alias);
    }
}
