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
 * 수기 입력 생성 요청(entryMode.md §5, dayguide.md §4.2·§8.4). {@code paymentMethod=CARD}는 회원이
 * {@code entry_mode=MANUAL} 이거나 {@code card_sync_enabled=false}일 때만 허용된다(§5-1, 서비스 계층 검증).
 * {@code usedTime} 미입력 시 {@code 00:00:00}으로 저장하고, 조합한 사용일시가 미래면 거부한다(§4.2).
 */
public record ManualTransactionCreateRequest(
        @NotNull PaymentMethod paymentMethod,
        @NotNull LocalDate usedDate,
        LocalTime usedTime,
        @NotNull @Positive BigDecimal amount,
        @NotNull Long categoryId,
        @NotBlank @Size(max = 255) String merchantName,
        String memo
) {
}
