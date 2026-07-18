package com.weaone.themoa.domain.cardtransaction.dto.request;

import com.weaone.themoa.domain.cardtransaction.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 수기 입력 생성 요청(entryMode.md §5). {@code paymentMethod=CARD}는 회원이
 * {@code entry_mode=MANUAL} 이거나 {@code card_sync_enabled=false}일 때만 허용된다(§5-1, 서비스 계층 검증).
 */
public record ManualTransactionCreateRequest(
        @NotNull PaymentMethod paymentMethod,
        @NotNull LocalDate usedDate,
        LocalDateTime usedAt,
        @NotNull @Positive BigDecimal amount,
        @NotNull Long categoryId,
        @NotBlank @Size(max = 255) String description,
        String memo
) {
}
