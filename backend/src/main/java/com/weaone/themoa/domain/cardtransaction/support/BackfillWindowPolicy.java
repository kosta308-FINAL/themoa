package com.weaone.themoa.domain.cardtransaction.support;

import java.time.LocalDate;

/**
 * 백필 달력 상한 계산(entryMode.md §3, cardtransaction.md §6-(C)). 신규 카드연동 최초 백필과 30일 초과
 * 복귀자의 "최근 내역도 불러오기" 상한이 반드시 같은 상수·같은 계산식을 써야 한다(entryMode.md §3 각주) —
 * 고정지출 탐지가 반복 3회를 관측하려면 완전한 달이 3개 필요하기 때문이다(fixedExpense.md §2).
 */
public final class BackfillWindowPolicy {

    public static final int BACKFILL_MONTHS = 3;

    private BackfillWindowPolicy() {
    }

    /** referenceDate가 속한 달의 1일 − BACKFILL_MONTHS. 월중 어느 날이어도 같은 값이 나온다(달력 월 기준). */
    public static LocalDate calendarFloor(LocalDate referenceDate) {
        return referenceDate.withDayOfMonth(1).minusMonths(BACKFILL_MONTHS);
    }
}
