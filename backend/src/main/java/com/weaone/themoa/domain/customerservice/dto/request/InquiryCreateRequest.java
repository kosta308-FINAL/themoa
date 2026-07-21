package com.weaone.themoa.domain.customerservice.dto.request;

import jakarta.validation.constraints.NotNull;

/** {@code multipart/form-data}의 {@code request} 파트(customerservice.md §4-2). 문자열 길이·trim 검증은 서비스에서 한다. */
public record InquiryCreateRequest(
        @NotNull Long inquiryCategoryId,
        String title,
        String content,
        @NotNull Boolean agreedPrivacy
) {
}
