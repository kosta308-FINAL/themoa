package com.weaone.themoa.domain.fixedexpense.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.fixedexpense.dto.response.FixedExpenseCandidateResponse;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseCandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 반복결제 탐지 후보 응답(fixedExpense.md §3, F-02). "등록"은 /api/fixed-expenses 쪽 API가 담당한다. */
@RestController
@RequestMapping("/api/fixed-expense-candidates")
@RequiredArgsConstructor
public class FixedExpenseCandidateController {

    private final FixedExpenseCandidateService fixedExpenseCandidateService;

    @Operation(summary = "고정지출 추천 후보 목록", description = "탐지 배치가 만든 대기중인(PENDING) 추천 후보를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FixedExpenseCandidateResponse>>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        List<FixedExpenseCandidateResponse> response = fixedExpenseCandidateService.listPending(memberId).stream()
                .map(FixedExpenseCandidateResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "추천 후보 거절", description = "이 서비스는 앞으로 다시 추천하지 않습니다.")
    @PostMapping("/{candidateId}/reject")
    public ResponseEntity<Void> reject(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long candidateId) {
        fixedExpenseCandidateService.reject(memberId, candidateId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "추천 후보 나중에", description = "이번 급여 주기만 건너뛰고 다음 주기에 다시 추천합니다.")
    @PostMapping("/{candidateId}/snooze")
    public ResponseEntity<Void> snooze(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long candidateId) {
        fixedExpenseCandidateService.snooze(memberId, candidateId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "습관적 소비로 분류", description = "고정지출이 아닌 습관적 소비로 재분류합니다.")
    @PostMapping("/{candidateId}/reclassify-habit")
    public ResponseEntity<Void> reclassifyHabit(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long candidateId) {
        fixedExpenseCandidateService.reclassifyHabit(memberId, candidateId);
        return ResponseEntity.noContent().build();
    }
}
