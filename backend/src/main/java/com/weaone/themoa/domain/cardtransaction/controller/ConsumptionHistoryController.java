package com.weaone.themoa.domain.cardtransaction.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionListResponse;
import com.weaone.themoa.domain.cardtransaction.dto.response.ConsumptionHistorySummaryResponse;
import com.weaone.themoa.domain.cardtransaction.service.ConsumptionHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전체 소비내역 상세 화면(consumeHistoryDetail.md). {@code dayguide.md}의 S-04 "전체 소비내역"과 같은
 * 화면을 새 계약(교체형 페이지네이션, 필터 없음)으로 대체하지만, 기존 {@code /api/spending-guide/transactions}는
 * 구 화면(SpendingHistoryPage.jsx)이 계속 쓰므로 경로를 분리했다.
 */
@RestController
@RequestMapping("/api/spending-guide/consumption-history")
@RequiredArgsConstructor
public class ConsumptionHistoryController {

    private final ConsumptionHistoryService consumptionHistoryService;

    @Operation(summary = "전체 소비내역 상세 급여주기 요약",
            description = "budgetId 생략 시 현재 급여 주기를 조회합니다. 순사용액, 취소·환불 반영액, 직전 주기 대비 증감, "
                    + "많이 쓴 곳 TOP 5, 전체 날짜의 일별 소비 추이를 한 번에 반환합니다.")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ConsumptionHistorySummaryResponse>> summary(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Long budgetId) {
        ConsumptionHistorySummaryResponse response = consumptionHistoryService.getSummary(memberId, budgetId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "전체 소비내역 상세 결제내역 페이지",
            description = "budgetId 생략 시 현재 급여 주기를 조회합니다. 정렬은 usedAt DESC, id DESC로 고정되며 "
                    + "페이지당 결과는 기존 목록을 대체합니다(누적 아님). size는 1~30만 허용합니다.")
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<CardTransactionListResponse>> transactions(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Long budgetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        CardTransactionListResponse response = consumptionHistoryService.getTransactions(memberId, budgetId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
