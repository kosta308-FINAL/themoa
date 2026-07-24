package com.weaone.themoa.domain.fixedexpense.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.fixedexpense.dto.response.FixedExpenseCoachingCardResponse;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseCoachingDismissService;
import com.weaone.themoa.domain.fixedexpense.service.FixedExpenseCoachingQueryService;
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

/** 고정지출 "연 환산" 코칭 카드. 고정지출 화면에 최대 3장 표시한다. */
@RestController
@RequestMapping("/api/fixed-expenses/coaching-cards")
@RequiredArgsConstructor
public class FixedExpenseCoachingCardController {

    private final FixedExpenseCoachingQueryService coachingQueryService;
    private final FixedExpenseCoachingDismissService coachingDismissService;

    @Operation(summary = "고정지출 코칭 카드 목록",
            description = "월세·관리비·보험처럼 필수 성격의 고정지출은 제외하고, 구독·여가성 등 재량 조정 가능한 항목만 최대 3장 " +
                    "\"연으로 보면 얼마\" 형태로 담담하게 보여줍니다. 절감을 종용하지 않습니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FixedExpenseCoachingCardResponse>>> list(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(coachingQueryService.list(memberId)));
    }

    @Operation(summary = "코칭 카드 다시 보지 않기",
            description = "이 카드는 즉시 목록에서 사라지고, 대상 고정지출은 다음 주기부터 후보에서 영구히 제외됩니다.")
    @PostMapping("/{cardId}/dismiss")
    public ResponseEntity<Void> dismiss(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long cardId) {
        coachingDismissService.dismiss(memberId, cardId);
        return ResponseEntity.noContent().build();
    }
}
