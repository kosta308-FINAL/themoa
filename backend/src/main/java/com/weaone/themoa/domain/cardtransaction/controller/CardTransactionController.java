package com.weaone.themoa.domain.cardtransaction.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.cardtransaction.dto.request.AmountCorrectionRequest;
import com.weaone.themoa.domain.cardtransaction.dto.request.CancelAmountCorrectionRequest;
import com.weaone.themoa.domain.cardtransaction.dto.request.CategoryCorrectionRequest;
import com.weaone.themoa.domain.cardtransaction.dto.request.RecoveryRequest;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionListResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.RecoveryStatusResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.SyncResponse;
import com.weaone.themoa.domain.cardtransaction.service.CardTransactionCorrectionService;
import com.weaone.themoa.domain.cardtransaction.service.CardTransactionQueryService;
import com.weaone.themoa.domain.cardtransaction.service.CardTransactionSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 거래 조회 + 건별 사용자 정정(category.md §2-④, cardtransaction.md §3-4·§4). 카드 수집 거래(source=SYNC)는
 * 삭제 API를 제공하지 않는다(§6-3) — 카드사 원장이 정본이라 삭제하면 롤링 윈도우 재수집이 되살린다.
 */
@RestController
@RequestMapping("/api/card-transactions")
@RequiredArgsConstructor
public class CardTransactionController {

    private final CardTransactionQueryService cardTransactionQueryService;
    private final CardTransactionCorrectionService cardTransactionCorrectionService;
    private final CardTransactionSyncService cardTransactionSyncService;

    @GetMapping
    public ResponseEntity<ApiResponse<CardTransactionListResponse>> list(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 30) Pageable pageable) {
        CardTransactionListResponse response = CardTransactionListResponse
                .from(cardTransactionQueryService.list(memberId, pageable));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{transactionId}/category")
    public ResponseEntity<Void> correctCategory(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long transactionId,
            @Valid @RequestBody CategoryCorrectionRequest request) {
        cardTransactionCorrectionService.correctCategory(memberId, transactionId, request.categoryId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{transactionId}/canceled-amount")
    public ResponseEntity<Void> correctCanceledAmount(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long transactionId,
            @Valid @RequestBody CancelAmountCorrectionRequest request) {
        cardTransactionCorrectionService.correctCanceledAmount(memberId, transactionId, request.canceledAmount());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{transactionId}/amount")
    public ResponseEntity<Void> correctAmount(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long transactionId,
            @Valid @RequestBody AmountCorrectionRequest request) {
        cardTransactionCorrectionService.correctAmount(memberId, transactionId, request.amount());
        return ResponseEntity.noContent().build();
    }

    /** 앱 열기(자동, manual=false → 30분 쓰로틀) / 수동 새로고침(manual=true → 쓰로틀 없이 락만, §6 (A)). */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<SyncResponse>> sync(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(defaultValue = "false") boolean manual) {
        SyncResponse response = SyncResponse.from(cardTransactionSyncService.syncOnDemand(memberId, manual));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 앱 홈 진입 시 30일 초과 복귀 여부를 판정한다(§6-C). true면 클라이언트가 복귀 선택 화면을 띄운다. */
    @GetMapping("/sync/recovery-status")
    public ResponseEntity<ApiResponse<RecoveryStatusResponse>> recoveryStatus(
            @AuthenticationPrincipal Long memberId) {
        boolean returning = cardTransactionSyncService.isReturningAfterLongAbsence(memberId);
        return ResponseEntity.ok(ApiResponse.success(new RecoveryStatusResponse(returning)));
    }

    /** 복귀 선택 완료 시에만 호출된다. 완료 후 member.last_active_at을 갱신한다(§6-C). */
    @PostMapping("/sync/recovery")
    public ResponseEntity<ApiResponse<SyncResponse>> recover(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RecoveryRequest request) {
        SyncResponse response = SyncResponse.from(cardTransactionSyncService.recover(memberId, request.mode()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
