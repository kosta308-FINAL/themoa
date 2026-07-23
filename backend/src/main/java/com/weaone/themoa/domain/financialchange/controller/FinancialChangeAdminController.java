package com.weaone.themoa.domain.financialchange.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.financialchange.service.FinancialChangeDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관심 상품 변경 감지 수동 실행(관리자 전용).
 *
 * <p>평소에는 매일 새벽 5시 스케줄러가 돌지만, 그 시각에 서버가 떠 있어야만 실행된다. 수집을 방금 돌린 뒤
 * 변경 알림이 제대로 나가는지 바로 확인할 수 있도록 통로를 둔다.
 *
 * <p>경로가 {@code /api/admin/**} 아래라 SecurityConfig의 기존 규칙으로 ADMIN 권한이 적용된다.
 */
@RestController
@RequestMapping("/api/admin/financial-products/changes")
public class FinancialChangeAdminController {

    private final FinancialChangeDetectionService detectionService;

    public FinancialChangeAdminController(FinancialChangeDetectionService detectionService) {
        this.detectionService = detectionService;
    }

    /** 지금 변경 감지를 실행하고, 만들어진 알림 건수를 돌려준다. */
    @PostMapping("/detect")
    public ResponseEntity<ApiResponse<Integer>> detect() {
        return ResponseEntity.ok(ApiResponse.success(detectionService.detectAll()));
    }
}
