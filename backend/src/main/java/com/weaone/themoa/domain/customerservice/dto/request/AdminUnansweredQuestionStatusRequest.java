package com.weaone.themoa.domain.customerservice.dto.request;

import com.weaone.themoa.domain.customerservice.entity.UnansweredQuestionStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUnansweredQuestionStatusRequest(
        @NotNull UnansweredQuestionStatus status
) {
}
