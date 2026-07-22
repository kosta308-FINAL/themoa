package com.weaone.themoa.domain.recommend.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.recommend.dto.response.FinancialCollectResponse;
import com.weaone.themoa.domain.recommend.ingest.CollectionBatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 금융상품 데이터 관리자 도구.
 *
 * <p>평소 수집은 매일 새벽 4시 스케줄러가 돌리지만, 그 시각에 서버가 떠 있어야만 실행되고 그전까지는
 * 상품 테이블이 비어 있다. 관리자가 필요한 시점에 직접 수집할 수 있도록 수동 실행 통로를 둔다.
 *
 * <p>경로가 {@code /api/admin/**} 아래라 SecurityConfig의 기존 규칙으로 ADMIN 권한이 적용된다.
 *
 * <p>수집과 임베딩은 일부러 나눠 두었다. 한 번에 묶으면 수 분이 걸려 화면이 오래 멈추고, 어느 단계에서
 * 실패했는지도 구분되지 않는다. 임베딩 재구성은 기존 {@code POST /api/financial-products/embeddings/rebuild}를 쓴다.
 */
@RestController
@RequestMapping("/api/admin/financial-products")
public class FinancialProductAdminController {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private final CollectionBatchService collectionBatchService;

    public FinancialProductAdminController(CollectionBatchService collectionBatchService) {
        this.collectionBatchService = collectionBatchService;
    }

    /**
     * finlife 오픈API에서 예적금·대출 상품을 지금 수집한다(신규 저장 + 기존 갱신).
     * 응답에 신규 건수가 있으니, 신규가 있으면 이어서 임베딩 재구성을 실행하면 검색에 반영된다.
     */
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<FinancialCollectResponse>> collect() {
        CollectionBatchService.CollectionResult result = collectionBatchService.runCollection();
        return ResponseEntity.ok(ApiResponse.success(
                FinancialCollectResponse.from(result, LocalDateTime.now(ZONE_SEOUL))));
    }
}
