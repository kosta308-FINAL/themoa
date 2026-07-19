package com.weaone.themoa.domain.budget.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** S-01 최근 N일 막대그래프(dayguide.md §3.3·§8.1). {@code guideLineAmount}는 오늘 하루 권장 소비액 수평선이다. */
public record RecentDaysResponse(
        List<DailyAmount> days,
        BigDecimal guideLineAmount
) {

    public record DailyAmount(LocalDate date, BigDecimal netAmount) {
    }
}
