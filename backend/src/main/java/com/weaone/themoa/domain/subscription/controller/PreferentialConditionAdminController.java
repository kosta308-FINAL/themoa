package com.weaone.themoa.domain.subscription.controller;

import com.weaone.themoa.common.exception.BusinessException;
import com.weaone.themoa.common.exception.ErrorCode;
import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.subscription.dto.request.PreferentialConditionUpdateRequest;
import com.weaone.themoa.domain.subscription.dto.response.PreferentialConditionCacheResponse;
import com.weaone.themoa.domain.subscription.dto.response.ProductConditionSummaryResponse;
import com.weaone.themoa.domain.subscription.entity.PreferentialConditionCache.ParsedItem;
import com.weaone.themoa.domain.subscription.service.PreferentialConditionCacheService;
import com.weaone.themoa.domain.subscription.support.PreferentialConditionCacheBatch;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 관리자용 우대조건 캐시 관리 API.
 *
 * <p>LLM 파싱이 어긋난 상품을 사람이 바로잡고, 원문 변경으로 재검토가 필요한(stale) 상품을 확인한다.
 * 경로가 {@code /api/admin/**}이라 기존 관리자 보안 설정을 그대로 따른다.
 */
@RestController
@RequestMapping("/api/admin/financial-products/conditions")
public class PreferentialConditionAdminController {

    private final PreferentialConditionCacheService cacheService;
    private final PreferentialConditionCacheBatch cacheBatch;

    public PreferentialConditionAdminController(PreferentialConditionCacheService cacheService,
                                                PreferentialConditionCacheBatch cacheBatch) {
        this.cacheService = cacheService;
        this.cacheBatch = cacheBatch;
    }

    /** 4시 배치를 기다리지 않고 지금 즉시 판매중 상품 캐시를 전부 최신화한다(관리자 수동 실행). */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<PreferentialConditionCacheBatch.RefreshResult>> refresh() {
        return ResponseEntity.ok(ApiResponse.success(cacheBatch.refreshAll()));
    }

    /** 상품 목록: 우대조건 원문이 있는 판매중 상품을 은행/상품명 키워드로 검색(캐시 상태 포함). */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductConditionSummaryResponse>>> products(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(cacheService.searchProducts(keyword)));
    }

    /** 원문이 바뀌어 재검토가 필요한(잠긴+stale) 상품 목록. */
    @GetMapping("/review")
    public ResponseEntity<ApiResponse<List<PreferentialConditionCacheResponse>>> reviewList() {
        List<PreferentialConditionCacheResponse> responses = cacheService.findStaleForReview().stream()
                .map(PreferentialConditionCacheResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /** 특정 상품의 현재 캐시. */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<PreferentialConditionCacheResponse>> detail(@PathVariable Long productId) {
        PreferentialConditionCacheResponse response = cacheService.find(productId)
                .map(PreferentialConditionCacheResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** 수동 수정: 전달된 항목으로 교체하고 잠근다(이후 배치가 덮지 않음). */
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long productId,
            @Valid @RequestBody PreferentialConditionUpdateRequest request) {
        List<ParsedItem> items = request.items().stream()
                .map(i -> new ParsedItem(
                        i.description().trim(),
                        i.rateBonus() == null ? BigDecimal.ZERO : i.rateBonus()))
                .toList();
        cacheService.updateManually(productId, items);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
