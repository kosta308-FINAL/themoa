package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePayment;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpensePaymentRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.entity.MerchantAliasTerms;
import com.weaone.themoa.domain.merchant.repository.BillerRepository;
import com.weaone.themoa.domain.merchant.repository.MerchantAliasRepository;
import com.weaone.themoa.domain.merchant.service.MerchantIdentityResult;
import com.weaone.themoa.domain.merchant.service.MerchantIdentityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FixedExpenseConfirmationServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long FIXED_EXPENSE_ID = 100L;
    private static final Long TRANSACTION_ID = 500L;

    @Mock
    private FixedExpenseRepository fixedExpenseRepository;
    @Mock
    private FixedExpensePaymentRepository fixedExpensePaymentRepository;
    @Mock
    private CardTransactionRepository cardTransactionRepository;
    @Mock
    private BillerRepository billerRepository;
    @Mock
    private MerchantAliasRepository merchantAliasRepository;
    @Mock
    private MerchantIdentityService merchantIdentityService;
    @Mock
    private FixedExpenseMatchingService fixedExpenseMatchingService;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BudgetCycleService budgetCycleService;

    @InjectMocks
    private FixedExpenseConfirmationService confirmationService;

    private Member member() {
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE,
                LocalDate.of(2000, 1, 1), LocalDateTime.now());
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    private Category category() {
        Category category = Category.seed(CategoryCode.SUBSCRIPTION, "구독");
        ReflectionTestUtils.setField(category, "id", 7L);
        return category;
    }

    private MerchantAlias alias() {
        MerchantAlias alias = MerchantAlias.create("waave 구독", category());
        ReflectionTestUtils.setField(alias, "id", 30L);
        return alias;
    }

    private FixedExpense fixedExpense(Member member, MerchantAlias alias) {
        FixedExpense fixedExpense = FixedExpense.registerDirect(member, "waave 구독", category(), alias,
                FixedExpensePaymentMethod.CARD, (short) 21, BigDecimal.valueOf(6300), "KRW",
                BigDecimal.valueOf(6300), null, null);
        ReflectionTestUtils.setField(fixedExpense, "id", FIXED_EXPENSE_ID);
        return fixedExpense;
    }

    private Merchant merchant(long id, String rawName) {
        Merchant merchant = Merchant.observe(rawName, null);
        ReflectionTestUtils.setField(merchant, "id", id);
        return merchant;
    }

    private CardTransaction transaction(Member member, Category category, Merchant merchant, MerchantAlias alias) {
        CardTransaction transaction = CardTransaction.sync(member, null, category, "12345678",
                LocalDate.of(2026, 7, 21), LocalDate.of(2026, 7, 21).atStartOfDay(),
                BigDecimal.valueOf(6300), null, "KRW", null, false, TransactionStatus.APPROVED,
                null, false, merchant.getMerchantNameRaw(), null, null, null, null);
        transaction.assignMerchant(merchant, alias);
        ReflectionTestUtils.setField(transaction, "id", TRANSACTION_ID);
        return transaction;
    }

    @Test
    @DisplayName("목록 재조회에서 빠진 거래는 transactionId를 직접 보내도 확정하지 않는다")
    void confirmRejectsTransactionOutsideCurrentCandidates() {
        Member member = member();
        FixedExpense fixedExpense = fixedExpense(member, alias());
        given(fixedExpenseRepository.findByIdAndMember_Id(FIXED_EXPENSE_ID, MEMBER_ID))
                .willReturn(Optional.of(fixedExpense));
        given(cardTransactionRepository.findMissedPaymentCandidates(
                eq(MEMBER_ID), eq(TransactionStatus.CANCELED), any(), any(), any(), any(),
                anyLong(), anyLong())).willReturn(List.of());

        assertThatThrownBy(() -> confirmationService.confirm(MEMBER_ID, FIXED_EXPENSE_ID, TRANSACTION_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FIXED_EXPENSE_PAYMENT_CANDIDATE_INVALID);
        then(fixedExpenseMatchingService).should(never())
                .confirmMatch(any(), any(), any(), eq(false));
    }

    @Test
    @DisplayName("사용자가 직접 연결한 Apple 결제를 해제하면 태그·이행·신규 biller 연결을 되돌린다")
    void undoConfirmationRestoresBillerMatch() {
        Member member = member();
        MerchantAlias alias = alias();
        FixedExpense fixedExpense = fixedExpense(member, alias);
        Merchant apple = merchant(40L, "Apple");
        CardTransaction transaction = transaction(member, category(), apple, null);
        transaction.assignFixedExpense(fixedExpense);
        assertThat(fixedExpense.assignBillerMerchant(apple)).isTrue();
        FixedExpensePayment payment = FixedExpensePayment.userConfirmed(
                fixedExpense, "2026-07", transaction, BigDecimal.valueOf(6300), null, true);
        given(fixedExpenseRepository.findByIdAndMember_Id(FIXED_EXPENSE_ID, MEMBER_ID))
                .willReturn(Optional.of(fixedExpense));
        given(fixedExpensePaymentRepository.findByCardTransaction_Id(TRANSACTION_ID))
                .willReturn(Optional.of(payment));

        confirmationService.undoConfirmation(MEMBER_ID, FIXED_EXPENSE_ID, TRANSACTION_ID);

        assertThat(transaction.getFixedExpense()).isNull();
        assertThat(fixedExpense.getBillerMerchant()).isNull();
        then(fixedExpensePaymentRepository).should().delete(payment);
        then(fixedExpensePaymentRepository).should().flush();
    }

    @Test
    @DisplayName("연결 해제는 이 확정에서 새로 만든 학습어와 해당 소급 태깅을 되돌린다")
    void undoConfirmationRemovesOnlyLearningCreatedByConfirmation() {
        Member member = member();
        MerchantAlias alias = alias();
        FixedExpense fixedExpense = fixedExpense(member, alias);
        Merchant merchant = merchant(41L, "UNKNOWN WAAVE PAYMENT");
        CardTransaction transaction = transaction(member, category(), merchant, alias);
        transaction.assignFixedExpense(fixedExpense);
        MerchantAliasTerms learnedTerm = MerchantAliasTerms.learn(alias, member, merchant.getMerchantNameRaw());
        ReflectionTestUtils.setField(learnedTerm, "id", 900L);
        FixedExpensePayment payment = FixedExpensePayment.userConfirmed(
                fixedExpense, "2026-07", transaction, BigDecimal.valueOf(6300), learnedTerm, false);
        given(fixedExpenseRepository.findByIdAndMember_Id(FIXED_EXPENSE_ID, MEMBER_ID))
                .willReturn(Optional.of(fixedExpense));
        given(fixedExpensePaymentRepository.findByCardTransaction_Id(TRANSACTION_ID))
                .willReturn(Optional.of(payment));
        given(merchantIdentityService.resolve(MEMBER_ID, merchant.getMerchantNameRaw()))
                .willReturn(MerchantIdentityResult.identified(merchant.getId(), null));
        given(cardTransactionRepository.findByMember_IdAndMerchant_Id(MEMBER_ID, merchant.getId()))
                .willReturn(List.of(transaction));

        confirmationService.undoConfirmation(MEMBER_ID, FIXED_EXPENSE_ID, TRANSACTION_ID);

        then(merchantIdentityService).should().unlearnTerm(MEMBER_ID, alias.getId(), 900L);
        assertThat(transaction.getFixedExpense()).isNull();
        assertThat(transaction.getMerchantAlias()).isNull();
    }
}
