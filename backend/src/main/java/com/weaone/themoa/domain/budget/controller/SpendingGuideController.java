package com.weaone.themoa.domain.budget.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.budget.dto.request.SalaryUpdateRequest;
import com.weaone.themoa.domain.budget.dto.request.SavingsGoalUpdateRequest;
import com.weaone.themoa.domain.budget.dto.request.SpendingGuideSetupRequest;
import com.weaone.themoa.domain.budget.dto.response.RecentDaysResponse;
import com.weaone.themoa.domain.budget.dto.response.SpendingGuideSummaryResponse;
import com.weaone.themoa.domain.budget.dto.response.TodayTransactionsResponse;
import com.weaone.themoa.domain.budget.service.SpendingGuideService;
import com.weaone.themoa.domain.cardtransaction.dto.response.CardTransactionListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 소비 가이드 예산·권장액(dailyBudget.md, dayguide.md §8.1 BUD 범위). */
@RestController
@RequestMapping("/api/spending-guide")
@RequiredArgsConstructor
public class SpendingGuideController {

    private final SpendingGuideService spendingGuideService;

    @Operation(summary = "소비 가이드 최초 설정(S-00A)",
            description = "월급과 명목 급여일을 저장하고 현재 급여 주기 예산을 생성합니다. 저축 목표·고정지출 미설정값은 0원으로 계산합니다.")
    @PutMapping("/setup")
    public ResponseEntity<ApiResponse<SpendingGuideSummaryResponse>> setup(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SpendingGuideSetupRequest request) {
        SpendingGuideSummaryResponse response = spendingGuideService.setup(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "오늘 현황·예산 기준(S-01)",
            description = "하루 권장 소비액·오늘 순사용액·오늘 사용 가능 금액·남은 예산을 계산해 반환합니다. 월급·급여일 미등록이면 오류가 아니라 setupRequired=true로 반환합니다.")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SpendingGuideSummaryResponse>> summary(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(spendingGuideService.getSummary(memberId)));
    }

    @Operation(summary = "월급 수정",
            description = "월급 원본을 갱신합니다. applyFrom=CURRENT_CYCLE이면 현재 주기 예산·하루 권장액도 즉시 다시 계산하고, NEXT_CYCLE이면 다음 주기부터 반영합니다.")
    @PatchMapping("/salary")
    public ResponseEntity<Void> changeSalary(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SalaryUpdateRequest request) {
        spendingGuideService.changeSalary(memberId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "저축 목표 설정·수정",
            description = "월 저축 목표 원본을 갱신합니다. 0원을 허용하며 적용 시점 규칙은 월급 수정과 같습니다.")
    @PatchMapping("/savings-goal")
    public ResponseEntity<Void> changeSavingsGoal(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SavingsGoalUpdateRequest request) {
        spendingGuideService.changeSavingsGoal(memberId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "오늘 거래 미리보기(S-01)",
            description = "오늘 거래 중 최신순 limit건(기본 5·최대 8)과 제한 전 전체 건수를 반환합니다. 고정지출·대체 수기 거래는 제외합니다.")
    @GetMapping("/transactions/today")
    public ResponseEntity<ApiResponse<TodayTransactionsResponse>> todayTransactions(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.success(spendingGuideService.getTodayTransactions(memberId, limit)));
    }

    @Operation(summary = "최근 N일 순사용액(S-01)",
            description = "최근 days일(기본·최대 7)의 날짜별 순사용액과 오늘 하루 권장 소비액을 반환합니다.")
    @GetMapping("/recent-days")
    public ResponseEntity<ApiResponse<RecentDaysResponse>> recentDays(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(ApiResponse.success(spendingGuideService.getRecentDays(memberId, days)));
    }

    @Operation(summary = "전체 소비내역(S-04)",
            description = "budgetId 생략 시 현재 급여 주기를 조회합니다. date·categoryId로 필터링할 수 있으며 고정지출·대체 수기 거래는 제외합니다.")
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<CardTransactionListResponse>> transactions(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Long budgetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long categoryId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        CardTransactionListResponse response =
                spendingGuideService.searchTransactions(memberId, budgetId, date, categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
