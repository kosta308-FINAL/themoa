package com.weaone.themoa.domain.budget.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.budget.dto.request.IncomeAdjustmentCreateRequest;
import com.weaone.themoa.domain.budget.dto.request.IncomeTypeUpdateRequest;
import com.weaone.themoa.domain.budget.dto.request.PaydayUpdateRequest;
import com.weaone.themoa.domain.budget.dto.request.SalaryUpdateRequest;
import com.weaone.themoa.domain.budget.dto.request.SavingsGoalUpdateRequest;
import com.weaone.themoa.domain.budget.dto.request.SpendingGuideSetupRequest;
import com.weaone.themoa.domain.budget.dto.request.WorkScheduleUpdateRequest;
import com.weaone.themoa.domain.budget.dto.response.IncomeAdjustmentResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

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

    @Operation(summary = "급여일 변경",
            description = "명목 급여일을 바꿉니다. 진행 중인 주기는 그대로 두고 다음 주기부터만 적용됩니다 — 적용 시점 선택지가 없습니다.")
    @PatchMapping("/payday")
    public ResponseEntity<Void> changePayday(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PaydayUpdateRequest request) {
        spendingGuideService.changePayday(memberId, request);
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

    @Operation(summary = "시급·근무스케줄 수정",
            description = "시급제(HOURLY) 전용. 시급과 요일별 근무시간을 전체 교체합니다. 적용 시점 규칙은 월급 수정과 같습니다.")
    @PatchMapping("/work-schedule")
    public ResponseEntity<Void> changeWorkSchedule(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody WorkScheduleUpdateRequest request) {
        spendingGuideService.changeWorkSchedule(memberId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "소득유형 전환(월급제↔알바 시급제)",
            description = "incomeType을 바꾸고 새 유형에 맞는 소득 정보로 갱신합니다. 반대 유형의 근무스케줄 등 잔존 데이터는 함께 정리됩니다. 적용 시점 규칙은 월급 수정과 같습니다.")
    @PatchMapping("/income-type")
    public ResponseEntity<Void> changeIncomeType(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody IncomeTypeUpdateRequest request) {
        spendingGuideService.changeIncomeType(memberId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "수입 직접 입력 생성",
            description = "용돈·정부지원금 등 비정기 수입이나 알바 근무 차액 보정을 이번 급여 주기에 더합니다. 0원은 허용하지 않습니다.")
    @PostMapping("/income-adjustments")
    public ResponseEntity<ApiResponse<IncomeAdjustmentResponse>> createIncomeAdjustment(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody IncomeAdjustmentCreateRequest request) {
        IncomeAdjustmentResponse response = spendingGuideService.createIncomeAdjustment(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "수입 직접 입력 목록",
            description = "이번 급여 주기에 등록된 수입 직접 입력을 최신순으로 반환합니다.")
    @GetMapping("/income-adjustments")
    public ResponseEntity<ApiResponse<List<IncomeAdjustmentResponse>>> incomeAdjustments(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(spendingGuideService.listIncomeAdjustments(memberId)));
    }

    @Operation(summary = "수입 직접 입력 삭제",
            description = "본인 소유가 아니거나 존재하지 않으면 404를 반환합니다.")
    @DeleteMapping("/income-adjustments/{id}")
    public ResponseEntity<Void> deleteIncomeAdjustment(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long id) {
        spendingGuideService.deleteIncomeAdjustment(memberId, id);
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
