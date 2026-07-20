package com.weaone.themoa.domain.customerservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record FaqFeedbackRequest(@NotNull Boolean helpful) {
}
