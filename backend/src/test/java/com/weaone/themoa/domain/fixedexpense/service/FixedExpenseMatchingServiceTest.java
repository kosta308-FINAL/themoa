package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.budget.service.BudgetCyclePolicy;
import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.category.entity.Category;
import com.weaone.themoa.domain.category.entity.CategoryCode;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpensePaymentRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.member.entity.Gender;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.repository.BillerRepository;
import com.weaone.themoa.domain.notification.entity.NotificationTypeCode;
import com.weaone.themoa.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/** 수집 매칭 오케스트레이션(fixedExpense.md §5) 검증. 순수 판정 로직은 {@link FixedExpenseMatchRulesTest} 참고. */
@ExtendWith(MockitoExtension.class)
class FixedExpenseMatchingServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock
    private FixedExpenseRepository fixedExpenseRepository;
    @Mock
    private FixedExpensePaymentRepository fixedExpensePaymentRepository;
    @Mock
    private BillerRepository billerRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BudgetCycleService budgetCycleService;

    private FixedExpenseMatchingService matchingService() {
        return new FixedExpenseMatchingService(fixedExpenseRepository, fixedExpensePaymentRepository,
                billerRepository, notificationService, budgetCycleService);
    }

    private Member member() {
        Member member = Member.signUp("user@example.com", "hash", "닉네임", Gender.MALE, LocalDate.of(2000, 1, 1), LocalDateTime.now());
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        return member;
    }

    private void stubYearMonth(Member member, LocalDate usedDate, String yearMonth) {
        given(budgetCycleService.resolveCycleForDate(eq(member), eq(usedDate)))
                .willReturn(new BudgetCyclePolicy.BudgetCycle(yearMonth, usedDate.withDayOfMonth(1), usedDate.withDayOfMonth(usedDate.lengthOfMonth())));
    }

    private MerchantAlias alias(String name) {
        MerchantAlias alias = MerchantAlias.create(name, null);
        ReflectionTestUtils.setField(alias, "id", 30L);
        return alias;
    }

    private FixedExpense cardFixedExpense(Member member, MerchantAlias alias, BigDecimal expectedAmount, short payDay) {
        Category category = Category.seed(CategoryCode.SUBSCRIPTION, "구독");
        FixedExpense fixedExpense = FixedExpense.registerDirect(member, "웨이브", category, alias,
                FixedExpensePaymentMethod.CARD, payDay, expectedAmount, "KRW", expectedAmount, null, null);
        ReflectionTestUtils.setField(fixedExpense, "id", 100L);
        return fixedExpense;
    }

    private CardTransaction transaction(Member member, MerchantAlias alias, Merchant merchant, LocalDate usedDate,
                                         BigDecimal amount) {
        CardTransaction tx = CardTransaction.sync(member, null, Category.seed(CategoryCode.SUBSCRIPTION, "구독"),
                "12345678", usedDate, usedDate.atStartOfDay(), amount, null, "KRW", null, false,
                TransactionStatus.APPROVED, null, false, "가맹점", null, null, null, null);
        tx.assignMerchant(merchant, alias);
        ReflectionTestUtils.setField(tx, "id", 500L);
        return tx;
    }

    @Test
    @DisplayName("신원·금액·결제일 조건을 모두 만족하면 태깅하고 이행 기록을 남긴다")
    void tagsAndRecordsPaymentWhenAllConditionsMatch() {
        Member member = member();
        MerchantAlias alias = alias("웨이브");
        FixedExpense fixedExpense = cardFixedExpense(member, alias, BigDecimal.valueOf(6300), (short) 5);
        CardTransaction tx = transaction(member, alias, null, LocalDate.of(2026, 7, 5), BigDecimal.valueOf(6300));
        stubYearMonth(member, LocalDate.of(2026, 7, 5), "2026-07");
        given(fixedExpenseRepository.findByMember_IdAndMerchantAlias_IdAndStatusAndPaymentMethod(
                MEMBER_ID, 30L, FixedExpenseStatus.ACTIVE, FixedExpensePaymentMethod.CARD))
                .willReturn(List.of(fixedExpense));
        given(fixedExpensePaymentRepository.existsByFixedExpense_IdAndYearMonth(100L, "2026-07")).willReturn(false);

        matchingService().match(tx);

        assertThat(tx.getFixedExpense()).isEqualTo(fixedExpense);
        then(fixedExpensePaymentRepository).should().save(any());
        then(notificationService).should(never()).createIfAbsent(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("이번 주기에 이미 이행 기록이 있으면(조건④) 다시 매칭하지 않는다")
    void skipsWhenAlreadyPaidThisCycle() {
        Member member = member();
        MerchantAlias alias = alias("웨이브");
        FixedExpense fixedExpense = cardFixedExpense(member, alias, BigDecimal.valueOf(6300), (short) 5);
        CardTransaction tx = transaction(member, alias, null, LocalDate.of(2026, 7, 5), BigDecimal.valueOf(6300));
        stubYearMonth(member, LocalDate.of(2026, 7, 5), "2026-07");
        given(fixedExpenseRepository.findByMember_IdAndMerchantAlias_IdAndStatusAndPaymentMethod(
                MEMBER_ID, 30L, FixedExpenseStatus.ACTIVE, FixedExpensePaymentMethod.CARD))
                .willReturn(List.of(fixedExpense));
        given(fixedExpensePaymentRepository.existsByFixedExpense_IdAndYearMonth(100L, "2026-07")).willReturn(true);

        matchingService().match(tx);

        assertThat(tx.getFixedExpense()).isNull();
        then(fixedExpensePaymentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("결제일이 윈도우(±3일) 밖이면 매칭하지 않는다")
    void skipsWhenPayDayOutsideWindow() {
        Member member = member();
        MerchantAlias alias = alias("웨이브");
        FixedExpense fixedExpense = cardFixedExpense(member, alias, BigDecimal.valueOf(6300), (short) 5);
        CardTransaction tx = transaction(member, alias, null, LocalDate.of(2026, 7, 20), BigDecimal.valueOf(6300));
        stubYearMonth(member, LocalDate.of(2026, 7, 20), "2026-07");
        given(fixedExpenseRepository.findByMember_IdAndMerchantAlias_IdAndStatusAndPaymentMethod(
                MEMBER_ID, 30L, FixedExpenseStatus.ACTIVE, FixedExpensePaymentMethod.CARD))
                .willReturn(List.of(fixedExpense));
        given(fixedExpensePaymentRepository.existsByFixedExpense_IdAndYearMonth(100L, "2026-07")).willReturn(false);

        matchingService().match(tx);

        assertThat(tx.getFixedExpense()).isNull();
        then(fixedExpensePaymentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("신원·결제일은 맞는데 금액만 벗어나면 태깅 대신 AMOUNT_CHANGE 알림을 만든다")
    void notifiesAmountChangeWhenOnlyAmountMismatches() {
        Member member = member();
        MerchantAlias alias = alias("웨이브");
        FixedExpense fixedExpense = cardFixedExpense(member, alias, BigDecimal.valueOf(6300), (short) 5);
        CardTransaction tx = transaction(member, alias, null, LocalDate.of(2026, 7, 5), BigDecimal.valueOf(9000));
        stubYearMonth(member, LocalDate.of(2026, 7, 5), "2026-07");
        given(fixedExpenseRepository.findByMember_IdAndMerchantAlias_IdAndStatusAndPaymentMethod(
                MEMBER_ID, 30L, FixedExpenseStatus.ACTIVE, FixedExpensePaymentMethod.CARD))
                .willReturn(List.of(fixedExpense));
        given(fixedExpensePaymentRepository.existsByFixedExpense_IdAndYearMonth(100L, "2026-07")).willReturn(false);

        matchingService().match(tx);

        assertThat(tx.getFixedExpense()).isNull();
        then(fixedExpensePaymentRepository).should(never()).save(any());
        then(notificationService).should().createIfAbsent(eq(member), eq(NotificationTypeCode.AMOUNT_CHANGE), any(),
                eq(fixedExpense), eq(null), eq("AMOUNT_CHANGE:fe=100:2026-07"));
    }

    @Test
    @DisplayName("biller(Apple 등) 경유 거래는 merchant_alias가 아니라 biller_merchant_id로 대조한다")
    void matchesViaBillerMerchant() {
        Member member = member();
        Merchant apple = Merchant.observe("Apple", null);
        ReflectionTestUtils.setField(apple, "id", 40L);
        MerchantAlias wave = alias("웨이브");
        Category category = Category.seed(CategoryCode.SUBSCRIPTION, "구독");
        FixedExpense fixedExpense = FixedExpense.fromCandidate(member, null, "웨이브", category, wave, apple,
                (short) 5, BigDecimal.valueOf(6300), "KRW", BigDecimal.valueOf(6300), null, null);
        ReflectionTestUtils.setField(fixedExpense, "id", 200L);
        CardTransaction tx = transaction(member, null, apple, LocalDate.of(2026, 7, 5), BigDecimal.valueOf(6300));
        stubYearMonth(member, LocalDate.of(2026, 7, 5), "2026-07");
        given(billerRepository.existsByNameNormalized("가맹점")).willReturn(true);
        given(fixedExpenseRepository.findByMember_IdAndBillerMerchant_IdAndStatusAndPaymentMethod(
                MEMBER_ID, 40L, FixedExpenseStatus.ACTIVE, FixedExpensePaymentMethod.CARD))
                .willReturn(List.of(fixedExpense));
        given(fixedExpensePaymentRepository.existsByFixedExpense_IdAndYearMonth(200L, "2026-07")).willReturn(false);

        matchingService().match(tx);

        assertThat(tx.getFixedExpense()).isEqualTo(fixedExpense);
        then(fixedExpensePaymentRepository).should().save(any());
    }

    @Test
    @DisplayName("취소된 거래는 매칭을 시도하지 않는다")
    void skipsCanceledTransaction() {
        Member member = member();
        MerchantAlias alias = alias("웨이브");
        CardTransaction tx = transaction(member, alias, null, LocalDate.of(2026, 7, 5), BigDecimal.valueOf(6300));
        tx.reconcileOnResync(TransactionStatus.CANCELED, tx.getAmount(), false, tx.getAmount(), null, false, null, alias);

        matchingService().match(tx);

        then(fixedExpenseRepository).should(never())
                .findByMember_IdAndMerchantAlias_IdAndStatusAndPaymentMethod(any(), any(), any(), any());
    }

    @Test
    @DisplayName("취소 재수집으로 매칭이 풀리면 이행 기록을 삭제하고 태그를 지운다")
    void unmatchesOnCancellation() {
        Member member = member();
        MerchantAlias alias = alias("웨이브");
        FixedExpense fixedExpense = cardFixedExpense(member, alias, BigDecimal.valueOf(6300), (short) 5);
        CardTransaction tx = transaction(member, alias, null, LocalDate.of(2026, 7, 5), BigDecimal.valueOf(6300));
        tx.assignFixedExpense(fixedExpense);
        given(fixedExpensePaymentRepository.findByCardTransaction_Id(500L)).willReturn(java.util.Optional.empty());

        matchingService().unmatchIfCanceled(tx);

        assertThat(tx.getFixedExpense()).isNull();
        then(fixedExpensePaymentRepository).should().findByCardTransaction_Id(500L);
    }
}
