package com.weaone.themoa.domain.coaching.dto.request;

import com.weaone.themoa.domain.coaching.entity.CoachingDismissType;
import jakarta.validation.constraints.NotNull;

public record CoachingCardDismissRequest(
        @NotNull CoachingDismissType dismissType
) {
}
