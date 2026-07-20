package com.weaone.themoa.domain.budget.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 급여일 변경(payday.md §급여일 변경). 항상 다음 주기부터만 적용되며 적용 시점 선택지가 없다. */
public record PaydayUpdateRequest(
        @NotNull @Min(1) @Max(31) Integer payday) {
}
