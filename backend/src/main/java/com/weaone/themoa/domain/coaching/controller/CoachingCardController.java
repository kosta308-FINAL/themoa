package com.weaone.themoa.domain.coaching.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.coaching.dto.request.CoachingCardDismissRequest;
import com.weaone.themoa.domain.coaching.dto.response.CoachingCardListResponse;
import com.weaone.themoa.domain.coaching.service.CoachingCardQueryService;
import com.weaone.themoa.domain.coaching.service.CoachingDismissService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 습관성 지출 코칭 카드(habitExpense.md §5, dayguide.md §8.1). 소비 가이드 화면 "이번 달 이렇게 아껴봐요" 섹션에 쓰인다. */
@RestController
@RequestMapping("/api/spending-guide/coaching-cards")
@RequiredArgsConstructor
public class CoachingCardController {

    private final CoachingCardQueryService coachingCardQueryService;
    private final CoachingDismissService coachingDismissService;

    @Operation(summary = "습관 코칭 카드 목록",
            description = "budgetId 생략 시 현재 급여 주기 동안 표시되는 카드(최대 3장)를 조회합니다. 정상적으로 비어 있으면 emptyReason으로 사유를 구분합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CoachingCardListResponse>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) Long budgetId) {
        return ResponseEntity.ok(ApiResponse.success(coachingCardQueryService.list(memberId, budgetId)));
    }

    @Operation(summary = "코칭 카드 넘기기",
            description = "이 카드는 즉시 목록에서 사라집니다. 추가로 '필요한 소비'(NOT_WASTE)는 다음 주기 톤다운, '그만 보기'(HIDE)는 다음 주기 후보 제외로 반영됩니다.")
    @PostMapping("/{cardId}/dismiss")
    public ResponseEntity<Void> dismiss(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long cardId,
            @Valid @RequestBody CoachingCardDismissRequest request) {
        coachingDismissService.dismiss(memberId, cardId, request.dismissType());
        return ResponseEntity.noContent().build();
    }
}
