package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.cardtransaction.entity.CardTransaction;
import com.weaone.themoa.domain.cardtransaction.entity.TransactionStatus;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseCandidate;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePayment;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpensePaymentRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.merchant.repository.BillerRepository;
import com.weaone.themoa.domain.notification.entity.NotificationTypeCode;
import com.weaone.themoa.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 수집 매칭(fixedExpense.md §5). 신규 저장·재수집 갱신 양쪽에서 실행한다 — 온디맨드·새벽 배치 구분 없이
 * 항상 켠다(매칭을 미루면 실제 결제가 일반 소비로 남아 이중차감된다). 이미 태깅된 거래는 호출자
 * ({@link com.weaone.themoa.domain.cardtransaction.service.CardTransactionCollectionService})가
 * 재호출하지 않는다 — 재매칭은 태그를 흔들 위험만 있다.
 */
@Service
@RequiredArgsConstructor
public class FixedExpenseMatchingService {

    private final FixedExpenseRepository fixedExpenseRepository;
    private final FixedExpensePaymentRepository fixedExpensePaymentRepository;
    private final BillerRepository billerRepository;
    private final NotificationService notificationService;
    private final BudgetCycleService budgetCycleService;

    @Transactional
    public void match(CardTransaction transaction) {
        if (transaction.getStatus() != TransactionStatus.APPROVED
                && transaction.getStatus() != TransactionStatus.PARTIAL_CANCELED) {
            return;
        }
        List<FixedExpense> candidates = findCandidates(transaction);
        if (candidates.isEmpty()) {
            return;
        }
        String yearMonth = yearMonthOf(transaction);
        for (FixedExpense fixedExpense : candidates) {
            if (fixedExpensePaymentRepository.existsByFixedExpense_IdAndYearMonth(fixedExpense.getId(), yearMonth)) {
                continue; // 조건④: 이번 주기 이미 이행
            }
            int payDayWindow = FixedExpenseMatchRules.resolvePayDayWindow(observedPayDayVariance(fixedExpense));
            if (!FixedExpenseMatchRules.isPayDayWithinWindow(fixedExpense.getExpectedPayDay(),
                    transaction.getUsedDate().getDayOfMonth(), payDayWindow)) {
                continue; // 조건③
            }
            if (FixedExpenseMatchRules.isAmountMatch(fixedExpense.getExpectedCurrency(), fixedExpense.getExpectedAmount(),
                    fixedExpense.getExpectedAmountKrw(), transaction.getCurrencyCode(), transaction.getAmount(),
                    transaction.getOriginalAmount())) {
                tagAndRecord(transaction, fixedExpense, yearMonth);
                return; // 거래 1건은 최대 하나의 고정지출에만 붙는다
            }
            notifyAmountChange(fixedExpense, yearMonth); // 신원·결제일은 맞는데 금액만 벗어남 = 가격 인상 의심(§7)
        }
    }

    /**
     * 후보 승인(경로 A)으로 만들어진 고정지출은 그 그룹의 실측 변동폭을 그때그때 읽는다(스냅샷하지
     * 않음 — 그룹은 매일 밤 재탐지로 계속 갱신되므로 등록 시점 값을 굳히면 금방 낡는다). 직접 등록
     * (경로 B)은 그룹이 없어 관측치 자체가 없다.
     */
    private Short observedPayDayVariance(FixedExpense fixedExpense) {
        FixedExpenseCandidate candidate = fixedExpense.getCandidate();
        return candidate != null ? candidate.getRecurringPaymentGroup().getPayDayVariance() : null;
    }

    /** 조건①: 일반은 alias 대조, biller 경유는 merchant(결제대행사) 대조(merchant.md §5-D). */
    private List<FixedExpense> findCandidates(CardTransaction transaction) {
        Long memberId = transaction.getMember().getId();
        if (transaction.getMerchant() != null
                && billerRepository.existsByNameNormalized(transaction.getMerchantNameRaw())) {
            return fixedExpenseRepository.findByMember_IdAndBillerMerchant_IdAndStatusAndPaymentMethod(
                    memberId, transaction.getMerchant().getId(), FixedExpenseStatus.ACTIVE, FixedExpensePaymentMethod.CARD);
        }
        if (transaction.getMerchantAlias() != null) {
            return fixedExpenseRepository.findByMember_IdAndMerchantAlias_IdAndStatusAndPaymentMethod(
                    memberId, transaction.getMerchantAlias().getId(), FixedExpenseStatus.ACTIVE, FixedExpensePaymentMethod.CARD);
        }
        return List.of();
    }

    private void tagAndRecord(CardTransaction transaction, FixedExpense fixedExpense, String yearMonth) {
        transaction.assignFixedExpense(fixedExpense);
        try {
            fixedExpensePaymentRepository.save(FixedExpensePayment.paid(fixedExpense, yearMonth, transaction,
                    transaction.getAmount()));
        } catch (DataIntegrityViolationException e) {
            // 동시 경합으로 같은 주기가 이미 이행 처리됨 — 태깅은 유지하고 이행 기록만 스킵한다.
        }
    }

    /** F-05 "이 거래예요" 확인(사용자 수동 매칭). 자동 매칭과 동일한 태깅·이행기록 경로를 재사용한다. */
    @Transactional
    public void confirmMatch(CardTransaction transaction, FixedExpense fixedExpense) {
        tagAndRecord(transaction, fixedExpense, yearMonthOf(transaction));
    }

    /**
     * 거래일이 속한 급여 주기 라벨. {@code member.payday}를 그대로 쓰지 않고 급여일 변경 이력까지 반영해
     * 계산한다(payday.md §급여일 변경) — 거래일이 과거라 그 시점엔 지금과 다른 payday가 유효했을 수 있다.
     */
    private String yearMonthOf(CardTransaction transaction) {
        return budgetCycleService.resolveCycleForDate(transaction.getMember(), transaction.getUsedDate()).yearMonth();
    }

    /** 취소 재수집 시 이행 기록 삭제 → 미납 복귀(§7). 매칭 안 된 거래는 아무 일도 하지 않는다. */
    @Transactional
    public void unmatchIfCanceled(CardTransaction transaction) {
        if (transaction.getFixedExpense() == null) {
            return;
        }
        fixedExpensePaymentRepository.findByCardTransaction_Id(transaction.getId())
                .ifPresent(fixedExpensePaymentRepository::delete);
        transaction.assignFixedExpense(null);
    }

    private void notifyAmountChange(FixedExpense fixedExpense, String yearMonth) {
        String dedupKey = "AMOUNT_CHANGE:fe=" + fixedExpense.getId() + ":" + yearMonth;
        String message = fixedExpense.getName() + " 결제 금액이 평소와 달라요. 구독료가 바뀌었다면 등록된 금액을 업데이트해 주세요.";
        notificationService.createIfAbsent(fixedExpense.getMember(), NotificationTypeCode.AMOUNT_CHANGE, message,
                fixedExpense, null, dedupKey);
    }
}
