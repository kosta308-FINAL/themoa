package com.weaone.themoa.domain.cardtransaction.dto.request;

import com.weaone.themoa.domain.cardtransaction.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 수기 입력 수정 요청(dayguide.md §4.1·§4.2·§8.4). S-03이 생성과 같은 입력창을 재사용하므로 요청 계약도
 * 생성과 동일하게 전체 필드를 다시 받는다.
 */
public record ManualTransactionUpdateRequest(
        @NotNull PaymentMethod paymentMethod,
        @NotNull LocalDate usedDate,
        LocalTime usedTime,
        @NotNull @Positive BigDecimal amount,
        @NotNull Long categoryId,
        @NotBlank @Size(max = 255) String merchantName,
        String memo
) {
}
