package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.budget.service.BudgetCycleService;
import com.weaone.themoa.domain.cardconnection.entity.ConnectionStatus;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpenseStatus;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpensePaymentRepository;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpenseRepository;
import com.weaone.themoa.domain.member.entity.Member;
import com.weaone.themoa.domain.notification.entity.NotificationType;
import com.weaone.themoa.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 미납·결제 예정일 알림 새벽 배치(fixedExpense.md §6). 연동 여부로 이원화된다: 카드 연동 회원의 CARD형
 * 고정지출은 카드내역 대조 미납(MISSED_PAYMENT), 그 외(TRANSFER형 또는 카드 미연동)는 예정일 리마인더
 * (PAYMENT_DUE)만 준다. §2와 동일하게 마지막 이용 후 30일 초과 사용자는 제외한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixedExpenseNotificationBatchService {

    private static final long INACTIVITY_LIMIT_DAYS = 30;
    private static final long MISSED_PAYMENT_GRACE_DAYS = 3;

    private final FixedExpenseRepository fixedExpenseRepository;
    private final FixedExpensePaymentRepository fixedExpensePaymentRepository;
    private final CardConnectionRepository cardConnectionRepository;
    private final NotificationService notificationService;
    private final BudgetCycleService budgetCycleService;

    /** 탐지 배치(03:30) 이후, 매칭 반영이 끝난 시각에 돌도록 04:00으로 잡는다. */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void runNightlyBatch() {
        LocalDate today = LocalDate.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        LocalDateTime now = LocalDateTime.now(FixedExpenseCyclePolicy.ZONE_SEOUL);

        List<FixedExpense> activeFixedExpenses = fixedExpenseRepository.findByStatus(FixedExpenseStatus.ACTIVE);
        for (FixedExpense fixedExpense : activeFixedExpenses) {
            try {
                evaluate(fixedExpense, today, now);
            } catch (RuntimeException e) {
                log.warn("고정지출 알림 판정 1건 실패, 다음 건으로 계속 진행합니다. fixedExpenseId={}", fixedExpense.getId(), e);
            }
        }
    }

    @Transactional
    public void evaluate(FixedExpense fixedExpense, LocalDate today, LocalDateTime now) {
        Member member = fixedExpense.getMember();
        if (member.isReturningAfterAbsence(now, INACTIVITY_LIMIT_DAYS) || fixedExpense.getExpectedPayDay() == null) {
            return;
        }
        budgetCycleService.ensurePaydayPromoted(member, today);
        String yearMonth = budgetCycleService.resolveCycleForDate(member, today).yearMonth();

        boolean cardLinked = fixedExpense.getPaymentMethod() == FixedExpensePaymentMethod.CARD
                && cardConnectionRepository.existsByMember_IdAndStatus(member.getId(), ConnectionStatus.ACTIVE);
        if (cardLinked) {
            evaluateMissedPayment(fixedExpense, member, today, yearMonth);
        } else {
            evaluatePaymentDue(fixedExpense, member, today, yearMonth);
        }
    }

    private void evaluateMissedPayment(FixedExpense fixedExpense, Member member, LocalDate today, String yearMonth) {
        LocalDate expectedDate = expectedDateInMonth(fixedExpense, today);
        if (!today.isAfter(expectedDate.plusDays(MISSED_PAYMENT_GRACE_DAYS))) {
            return; // 아직 결제일 윈도우가 안 지났다
        }
        if (fixedExpensePaymentRepository.existsByFixedExpense_IdAndYearMonth(fixedExpense.getId(), yearMonth)) {
            return; // 이미 이행됨
        }
        String dedupKey = "MISSED_PAYMENT:fe=" + fixedExpense.getId() + ":" + yearMonth;
        String message = "이번 달 " + fixedExpense.getName() + " 결제가 안 보여요. 이 거래인가요?";
        notificationService.createIfAbsent(member, NotificationType.MISSED_PAYMENT, message, fixedExpense, dedupKey);
    }

    private void evaluatePaymentDue(FixedExpense fixedExpense, Member member, LocalDate today, String yearMonth) {
        if (!today.isEqual(expectedDateInMonth(fixedExpense, today))) {
            return;
        }
        String dedupKey = "PAYMENT_DUE:fe=" + fixedExpense.getId() + ":" + yearMonth;
        String message = "오늘 " + fixedExpense.getName() + " 결제일이에요.";
        notificationService.createIfAbsent(member, NotificationType.PAYMENT_DUE, message, fixedExpense, dedupKey);
    }

    /** 없는 날짜(31일 등)는 그 달 말일로 당긴다(erd.md member.payday와 동일한 관례). */
    private LocalDate expectedDateInMonth(FixedExpense fixedExpense, LocalDate today) {
        int day = Math.min(fixedExpense.getExpectedPayDay(), today.lengthOfMonth());
        return today.withDayOfMonth(day);
    }
}
