package com.weaone.themoa.domain.fixedexpense.service;

import com.weaone.themoa.domain.cardconnection.entity.ConnectionStatus;
import com.weaone.themoa.domain.cardconnection.repository.CardConnectionRepository;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpense;
import com.weaone.themoa.domain.fixedexpense.entity.FixedExpensePaymentMethod;
import com.weaone.themoa.domain.fixedexpense.repository.FixedExpensePaymentRepository;
import com.weaone.themoa.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * F-01/F-04 상태 배지 계산(view/fixedExpense.md §4). 카드 미연동이거나 이체형이면 대조 대상이 없어
 * null(배지 없음)을 반환한다. {@link FixedExpenseNotificationBatchService}가 쓰는 미납·예정일 판정과
 * 같은 기준(±3일 유예)을 그대로 따른다 — 여기는 조회 시점 계산이고, 그쪽은 알림 생성 배치라는 차이만 있다.
 */
@Service
@RequiredArgsConstructor
public class FixedExpensePaymentStatusService {

    private static final long DUE_SOON_WINDOW_DAYS = 3;
    private static final long MISSED_GRACE_DAYS = 3;

    private final FixedExpensePaymentRepository fixedExpensePaymentRepository;
    private final CardConnectionRepository cardConnectionRepository;

    @Transactional(readOnly = true)
    public String resolve(FixedExpense fixedExpense) {
        Member member = fixedExpense.getMember();
        boolean cardLinked = fixedExpense.getPaymentMethod() == FixedExpensePaymentMethod.CARD
                && fixedExpense.getExpectedPayDay() != null
                && cardConnectionRepository.existsByMember_IdAndStatus(member.getId(), ConnectionStatus.ACTIVE);
        if (!cardLinked) {
            return null;
        }

        LocalDate today = LocalDate.now(FixedExpenseCyclePolicy.ZONE_SEOUL);
        String yearMonth = FixedExpenseCyclePolicy.currentYearMonth(member.getPayday());
        if (fixedExpensePaymentRepository.existsByFixedExpense_IdAndYearMonth(fixedExpense.getId(), yearMonth)) {
            return "PAID";
        }

        LocalDate expectedDate = expectedDateInMonth(fixedExpense, today);
        if (today.isAfter(expectedDate.plusDays(MISSED_GRACE_DAYS))) {
            return "MISSED";
        }
        if (!expectedDate.isBefore(today) && !expectedDate.isAfter(today.plusDays(DUE_SOON_WINDOW_DAYS))) {
            return "DUE_SOON";
        }
        return null;
    }

    /** 없는 날짜(31일 등)는 그 달 말일로 당긴다(erd.md member.payday와 동일한 관례). */
    private LocalDate expectedDateInMonth(FixedExpense fixedExpense, LocalDate today) {
        int day = Math.min(fixedExpense.getExpectedPayDay(), today.lengthOfMonth());
        return today.withDayOfMonth(day);
    }
}
