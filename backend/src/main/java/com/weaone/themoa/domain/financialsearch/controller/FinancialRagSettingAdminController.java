package com.weaone.themoa.domain.financialsearch.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.financialsearch.config.FinancialRagProperties;
import com.weaone.themoa.domain.financialsearch.dto.request.FinancialRagSettingUpdateRequest;
import com.weaone.themoa.domain.financialsearch.dto.response.FinancialRagSettingResponse;
import com.weaone.themoa.domain.financialsearch.service.FinancialRagSettingService;
import com.weaone.themoa.domain.financialsearch.service.FinancialRagSettingValues;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 검색 튜닝값 조회·변경(관리자 전용). 검색 품질 점검 콘솔에서 확인한 결과를 바로 반영하는 데 쓴다.
 *
 * <p>예: 점검 화면에서 "벡터 히트 0건 / 임계값 0.45"를 확인했다면, 임계값을 0.3으로 낮춘 뒤 다시 점검해
 * 의미 검색이 걸리는지 확인할 수 있다. 재배포가 필요 없다.
 *
 * <p>경로가 {@code /api/admin/**} 아래라 SecurityConfig의 기존 규칙으로 ADMIN 권한이 적용된다.
 */
@RestController
@RequestMapping("/api/admin/financial-products/search/settings")
public class FinancialRagSettingAdminController {

    private final FinancialRagSettingService settingService;
    private final FinancialRagProperties ragProperties;

    public FinancialRagSettingAdminController(FinancialRagSettingService settingService,
                                              FinancialRagProperties ragProperties) {
        this.settingService = settingService;
        this.ragProperties = ragProperties;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<FinancialRagSettingResponse>> current() {
        return ResponseEntity.ok(ApiResponse.success(toResponse(settingService.current())));
    }

    /** 변경 후 실제로 적용된 값을 돌려준다(허용 범위를 벗어난 값은 범위 안으로 조정된다). */
    @PutMapping
    public ResponseEntity<ApiResponse<FinancialRagSettingResponse>> update(
            @AuthenticationPrincipal Long adminId,
            @Valid @RequestBody FinancialRagSettingUpdateRequest request) {
        FinancialRagSettingValues updated = settingService.update(
                adminId, request.topK(), request.retryTopK(), request.minimumSimilarity());
        return ResponseEntity.ok(ApiResponse.success(toResponse(updated)));
    }

    /** 초기값으로 되돌리기. 저장된 설정을 지워 application.yaml 기본값을 다시 쓰게 한다. */
    @DeleteMapping
    public ResponseEntity<ApiResponse<FinancialRagSettingResponse>> reset() {
        FinancialRagSettingValues defaults = settingService.resetToDefaults();
        return ResponseEntity.ok(ApiResponse.success(toResponse(defaults)));
    }

    private FinancialRagSettingResponse toResponse(FinancialRagSettingValues values) {
        Optional<LocalDateTime> updatedAt = settingService.findLastUpdatedAt();
        return new FinancialRagSettingResponse(
                values.topK(),
                values.retryTopK(),
                values.minimumSimilarity(),
                updatedAt.isEmpty(),
                updatedAt.orElse(null),
                ragProperties.isEnabled());
    }
}
