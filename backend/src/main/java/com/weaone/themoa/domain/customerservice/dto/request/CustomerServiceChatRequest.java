package com.weaone.themoa.domain.customerservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerServiceChatRequest(
        @NotBlank @Size(max = 500) String message,
        Long conversationId
) {
}
