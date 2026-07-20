package com.weaone.themoa.domain.customerservice.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.customerservice.dto.request.FaqFeedbackRequest;
import com.weaone.themoa.domain.customerservice.dto.response.FaqListResponse;
import com.weaone.themoa.domain.customerservice.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** FAQ 조회·피드백(customerservice.md §4-1). 전부 로그인 사용자 전용이다. */
@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @Operation(summary = "FAQ 목록 조회",
            description = "활성 카테고리·활성 FAQ만 반환합니다. categoryCode·keyword는 선택이며 정렬은 priority DESC, id ASC입니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<FaqListResponse>> search(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(ApiResponse.success(faqService.search(memberId, categoryCode, keyword, page, size)));
    }

    @Operation(summary = "FAQ 피드백 등록·변경",
            description = "회원당 FAQ 1건에 1개만 저장되며 같은 API로 선택을 바꿀 수 있는 멱등 API입니다.")
    @PutMapping("/{faqId}/feedback")
    public ResponseEntity<Void> putFeedback(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long faqId,
            @Valid @RequestBody FaqFeedbackRequest request) {
        faqService.putFeedback(memberId, faqId, request);
        return ResponseEntity.ok().build();
    }
}
