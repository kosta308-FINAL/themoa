package com.weaone.themoa.domain.financialsearch.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.financialsearch.dto.FinancialSearchRequest;
import com.weaone.themoa.domain.financialsearch.dto.FinancialSearchResponse;
import com.weaone.themoa.domain.financialsearch.dto.response.FinancialEmbeddingResponse;
import com.weaone.themoa.domain.financialsearch.service.FinancialEmbeddingService;
import com.weaone.themoa.domain.financialsearch.service.FinancialProductSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 금융상품 검색 JSON API.
 * - /search           : 인증 필요(SecurityConfig의 anyRequest().authenticated()). 검색(1단계 LIKE + 2단계 벡터
 *   하이브리드). app.financial.rag.enabled에 따라 자동 전환.
 * - /embeddings/rebuild: ADMIN 권한 필요(SecurityConfig). 전체 상품을 금융 전용 Qdrant 컬렉션에 다시
 *   임베딩(2단계 데이터 적재용, 수동 트리거).
 */
@RestController
@RequestMapping("/api/financial-products")
public class FinancialSearchController {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private final FinancialProductSearchService financialProductSearchService;
    private final FinancialEmbeddingService financialEmbeddingService;

    public FinancialSearchController(FinancialProductSearchService financialProductSearchService,
                                     FinancialEmbeddingService financialEmbeddingService) {
        this.financialProductSearchService = financialProductSearchService;
        this.financialEmbeddingService = financialEmbeddingService;
    }

    // 화면에서 검색어 입력하면 호출되는 API.
    @PostMapping("/search")
    public ApiResponse<FinancialSearchResponse> search(@Valid @RequestBody FinancialSearchRequest request) {
        return ApiResponse.success(financialProductSearchService.search(request));
    }

    // 상품 데이터가 바뀌었을 때(신규 배치 수집 등) 수동으로 금융 Qdrant 컬렉션을 다시 채우는 용도.
    // ADMIN 권한 필요(SecurityConfig에서 강제) — 일반 사용자가 트리거하면 재임베딩 비용·부하가 발생한다.
    @PostMapping("/embeddings/rebuild")
    public ApiResponse<FinancialEmbeddingResponse> rebuildEmbeddings() {
        int embeddedCount = financialEmbeddingService.embedAll();
        return ApiResponse.success(
                new FinancialEmbeddingResponse(embeddedCount, LocalDateTime.now(ZONE_SEOUL)));
    }
}
