package com.weaone.themoa.domain.cardtransaction.dto.request;

import jakarta.validation.constraints.Size;

/** 거래 메모 자유 입력(category.md §7, `MOA-S-CAT-CTG-06`). 빈 문자열/NULL로 보내면 메모를 지운다. */
public record MemoUpdateRequest(
        @Size(max = 2000) String memo
) {
}
