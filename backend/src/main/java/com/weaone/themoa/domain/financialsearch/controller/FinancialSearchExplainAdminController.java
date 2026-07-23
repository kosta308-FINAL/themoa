package com.weaone.themoa.domain.financialsearch.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.financialsearch.dto.request.FinancialSearchExplainRequest;
import com.weaone.themoa.domain.financialsearch.dto.response.FinancialSearchExplainResponse;
import com.weaone.themoa.domain.financialsearch.service.FinancialProductSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 검색 품질 점검 콘솔(관리자 전용).
 *
 * <p>검색어를 넣으면 실제 검색과 같은 채점을 돌린 뒤, 후보 상품별 유사도·키워드 점수와 제외 사유를
 * 돌려준다. 결과가 기대와 다를 때 유사도 임계값 때문인지, 인구집단·나이 필터 때문인지, 상품유형
 * 의도 감지 때문인지 구분할 수 있다.
 *
 * <p>경로가 {@code /api/admin/**} 아래라 SecurityConfig의 기존 규칙으로 ADMIN 권한이 적용된다.
 */
@RestController
@RequestMapping("/api/admin/financial-products/search")
public class FinancialSearchExplainAdminController {

    private final FinancialProductSearchService financialProductSearchService;

    public FinancialSearchExplainAdminController(FinancialProductSearchService financialProductSearchService) {
        this.financialProductSearchService = financialProductSearchService;
    }

    @PostMapping("/explain")
    public ResponseEntity<ApiResponse<FinancialSearchExplainResponse>> explain(
            @Valid @RequestBody FinancialSearchExplainRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                financialProductSearchService.explain(request.query())));
    }
}
