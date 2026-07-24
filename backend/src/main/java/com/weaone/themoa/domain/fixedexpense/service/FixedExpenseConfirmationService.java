package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.PaymentMethod;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.cardtransaction.repository.CardTransactionRepository;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.member.repository.MemberRepository;
import com.weaone.themoa.domain.merchant.entity.Merchant;
import com.weaone.themoa.domain.merchant.entity.MerchantAlias;
import com.weaone.themoa.domain.merchant.repository.BillerRepository;
import com.weaone.themoa.domain.merchant.service.MerchantIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * F-05 미납 결제 확인("이 거래예요") — 이름형은 학습 루프(merchant.md §3 4단계: terms 학습 + 재태깅),
 * biller형은 {@code biller_merchant_id} 소급 채우기(troubleshooting/billerProblem.md)로 분기한다.
 * 자동탐지가 못 잡은 사각지대(직접등록 직후, 3개월 데이터 미축적)의 백업 경로다.
 */
@Service
@RequiredArgsConstructor
public class FixedExpenseConfirmationService {

    private static final String CURRENCY_KRW = "KRW";
    private static final BigDecimal DOMESTIC_TOLERANCE = new BigDecimal("0.10");
    private static final BigDecimal FOREIGN_TOLERANCE = new BigDecimal("0.15");
    private static final int CANDIDATE_WINDOW_DAYS = 3;

    private final FixedExpenseRepository fixedExpenseRepository;
    private final CardTransactionRepository cardTransactionRepository;
    private final BillerRepository billerRepository;
    private final MerchantIdentityService merchantIdentityService;
    private final FixedExpenseMatchingService fixedExpenseMatchingService;
    private final MemberRepository memberRepository;
    private final BudgetCycleService budgetCycleService;

    /** 미납 알림을 탭했을 때 보여줄 후보 거래 목록. 굳혀 둔 값이 아니라 그 시점 기준으로 재조회한다(§6). */
    @Transactional(readOnly = true)
    public List<CardTransaction> listCandidates(Long memberId, Long fixedExpenseId) {
        FixedExpense fixedExpense = getOwned(memberId, fixedExpenseId);
        LocalDate today = LocalDate.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        LocalDate expectedDate = expectedDateInMonth(fixedExpense, today);
        LocalDate start = expectedDate.minusDays(CANDIDATE_WINDOW_DAYS);
        LocalDate end = expectedDate.plusDays(CANDIDATE_WINDOW_DAYS);

        BigDecimal tolerance = CURRENCY_KRW.equals(fixedExpense.getExpectedCurrency())
                ? DOMESTIC_TOLERANCE : FOREIGN_TOLERANCE;
        BigDecimal baseAmount = fixedExpense.getExpectedAmountKrw();
        BigDecimal min = baseAmount.multiply(BigDecimal.ONE.subtract(tolerance));
        BigDecimal max = baseAmount.multiply(BigDecimal.ONE.add(tolerance));

        Long merchantAliasId = fixedExpense.getMerchantAlias() != null
                ? fixedExpense.getMerchantAlias().getId() : null;
        return cardTransactionRepository.findMissedPaymentCandidates(memberId, TransactionStatus.CANCELED,
                start, end, min, max, merchantAliasId);
    }

    /** "이 거래예요" 확정. */
    @Transactional
    public void confirm(Long memberId, Long fixedExpenseId, Long transactionId) {
        FixedExpense fixedExpense = getOwned(memberId, fixedExpenseId);
        CardTransaction transaction = cardTransactionRepository.findByIdAndMember_Id(transactionId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_TRANSACTION_NOT_FOUND));

        Merchant merchant = transaction.getMerchant();
        boolean billerRouted = merchant != null && billerRepository.existsByNameNormalized(transaction.getMerchantNameRaw());
        if (billerRouted) {
            fixedExpense.assignBillerMerchant(merchant);
        } else {
            learnTermAndRetag(memberId, fixedExpense, merchant, transaction.getMerchantNameRaw());
        }
        fixedExpenseMatchingService.confirmMatch(transaction, fixedExpense);
    }

    /**
     * 카드 미연동·계좌이체형의 수기 결제처리. 대조할 실거래가 없으니 사용자가 스스로 "결제했다"고
     * 확정하는 F-05의 대체 경로다. 수기 거래의 거래일은 오늘이 아니라 이번 달 결제 예정일로 남긴다 —
     * 결제일이 14일인데 24일에 결제처리를 눌렀다고 결제내역에 24일 거래로 찍히면 고정지출의 실제 결제
     * 시점과 어긋난다(미래에 아직 안 온 결제일이면 오늘을 넘기지 않는다). 이행 판정에 쓰는 yearMonth는
     * 거래일이 아니라 확정 시점(오늘) 기준 주기로 별도 계산해 {@link FixedExpenseMatchingService#confirmManualPayment}에
     * 넘긴다 — {@link FixedExpensePaymentStatusService#resolve}가 조회하는 주기와 반드시 같아야 PAID로 반영된다.
     * 이름형 학습·재태깅은 실거래 신원이 없어 대상이 아니다.
     */
    @Transactional
    public void confirmManually(Long memberId, Long fixedExpenseId) {
        FixedExpense fixedExpense = getOwned(memberId, fixedExpenseId);
        Member member = memberRepository.getReferenceById(memberId);
        LocalDate today = LocalDate.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        LocalDate expectedDate = expectedDateInMonth(fixedExpense, today);
        LocalDate paidDate = expectedDate.isAfter(today) ? today : expectedDate;
        String yearMonth = member.getPayday() == null
                ? FixedExpenseCyclePolicy.currentYearMonth(null)
                : budgetCycleService.resolveCycleForDate(member, today).yearMonth();
        PaymentMethod paymentMethod = fixedExpense.getPaymentMethod() == FixedExpensePaymentMethod.CARD
                ? PaymentMethod.CARD : PaymentMethod.TRANSFER;
        CardTransaction transaction = CardTransaction.manual(member, fixedExpense.getCategory(), paymentMethod,
                paidDate, paidDate.atStartOfDay(), fixedExpense.getExpectedAmountKrw(), fixedExpense.getName(), null);
        cardTransactionRepository.save(transaction);
        fixedExpenseMatchingService.confirmManualPayment(transaction, fixedExpense, yearMonth);
    }

    /** merchant.md §3 4단계 ①②: terms 학습 + 이 회원의 같은 원본가맹점 거래 전체 재태깅. */
    private void learnTermAndRetag(Long memberId, FixedExpense fixedExpense, Merchant merchant, String merchantNameRaw) {
        MerchantAlias alias = fixedExpense.getMerchantAlias();
        if (alias == null) {
            throw new BusinessException(ErrorCode.FIXED_EXPENSE_MERCHANT_ALIAS_REQUIRED);
        }
        merchantIdentityService.learnTerm(memberId, alias.getId(), merchantNameRaw);
        if (merchant == null) {
            return;
        }
        for (CardTransaction tx : cardTransactionRepository.findByMember_IdAndMerchant_Id(memberId, merchant.getId())) {
            tx.assignMerchant(merchant, alias);
        }
    }

    /** 없는 날짜(31일 등)는 그 달 말일로 당긴다. */
    private LocalDate expectedDateInMonth(FixedExpense fixedExpense, LocalDate today) {
        if (fixedExpense.getExpectedPayDay() == null) {
            return today;
        }
        int day = Math.min(fixedExpense.getExpectedPayDay(), today.lengthOfMonth());
        return today.withDayOfMonth(day);
    }

    private FixedExpense getOwned(Long memberId, Long fixedExpenseId) {
        return fixedExpenseRepository.findByIdAndMember_Id(fixedExpenseId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIXED_EXPENSE_NOT_FOUND));
    }
}
