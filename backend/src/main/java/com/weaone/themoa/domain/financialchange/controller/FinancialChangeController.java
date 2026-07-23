package com.weaone.themoa.domain.financialchange.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.financialchange.dto.response.FinancialChangeResponse;
import com.weaone.themoa.domain.financialchange.service.FinancialChangeQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 관심 상품 변경 내역 조회. 알림을 눌렀을 때 "이전 → 이후"를 팝업으로 보여주기 위한 API다.
 * 로그인 사용자 본인 것만 조회된다.
 */
@RestController
@RequestMapping("/api/financial-products/changes")
public class FinancialChangeController {

    private final FinancialChangeQueryService financialChangeQueryService;

    public FinancialChangeController(FinancialChangeQueryService financialChangeQueryService) {
        this.financialChangeQueryService = financialChangeQueryService;
    }

    /** 알림함에서 알림을 눌렀을 때 호출한다(알림 id로 해당 변경 내역을 찾는다). */
    @GetMapping("/by-notification/{notificationId}")
    public ResponseEntity<ApiResponse<FinancialChangeResponse>> findByNotification(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(ApiResponse.success(
                financialChangeQueryService.findByNotification(memberId, notificationId)));
    }

    /** 변경 내역 전체 목록(최근 순). */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FinancialChangeResponse>>> findAll(
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.success(financialChangeQueryService.findAll(memberId)));
    }
}
