package com.weaone.themoa.domain.cardtransaction.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.cardtransaction.dto.request.ManualTransactionCreateRequest;
import com.weaone.themoa.domain.cardtransaction.dto.request.ManualTransactionUpdateRequest;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionResponse;
import com.weaone.themoa.domain.cardtransaction.service.ManualTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** S-03 수기 지출 생성·수정·삭제(entryMode.md §5, dayguide.md §4.2·§8.1). */
@RestController
@RequestMapping("/api/manual-transactions")
@RequiredArgsConstructor
public class ManualTransactionController {

    private final ManualTransactionService manualTransactionService;

    @Operation(summary = "수기 지출 생성",
            description = "현금·계좌이체·(자동수집이 꺼져 있는 동안의) 카드 결제를 직접 입력합니다. 카테고리는 요청에 지정한 값으로 확정됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CardTransactionResponse>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ManualTransactionCreateRequest request) {
        CardTransactionResponse response = manualTransactionService.create(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "수기 지출 수정",
            description = "본인 소유의 source=MANUAL 거래만 수정할 수 있습니다.")
    @PatchMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<CardTransactionResponse>> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long transactionId,
            @Valid @RequestBody ManualTransactionUpdateRequest request) {
        CardTransactionResponse response = manualTransactionService.update(memberId, transactionId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "수기 지출 삭제",
            description = "본인 소유의 source=MANUAL, 대체되지 않은(replaced_at IS NULL) 거래만 물리 삭제합니다.")
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long transactionId) {
        manualTransactionService.delete(memberId, transactionId);
        return ResponseEntity.noContent().build();
    }
}
