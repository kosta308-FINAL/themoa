package com.weaone.themoa.domain.financialsearch.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.financialsearch.dto.response.FinancialStatusResponse;
import com.weaone.themoa.domain.financialsearch.service.FinancialStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 금융상품 데이터 현황(관리자 대시보드). 화면 진입 시 호출해 "지금 정상인가"를 한눈에 보여준다.
 *
 * <p>경로가 {@code /api/admin/**} 아래라 SecurityConfig의 기존 규칙으로 ADMIN 권한이 적용된다.
 */
@RestController
@RequestMapping("/api/admin/financial-products")
public class FinancialStatusAdminController {

    private final FinancialStatusService financialStatusService;

    public FinancialStatusAdminController(FinancialStatusService financialStatusService) {
        this.financialStatusService = financialStatusService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<FinancialStatusResponse>> status() {
        return ResponseEntity.ok(ApiResponse.success(financialStatusService.getStatus()));
    }
}
