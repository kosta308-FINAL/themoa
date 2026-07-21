package com.weaone.themoa.domain.merchant.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.merchant.dto.request.AdminMerchantQuickAliasRequest;
import com.weaone.themoa.domain.merchant.dto.request.MerchantAliasPromoteRequest;
import com.weaone.themoa.domain.merchant.dto.response.AdminMerchantPromotionCandidateResponse;
import com.weaone.themoa.domain.merchant.dto.response.AdminUnclassifiedMerchantResponse;
import com.weaone.themoa.domain.merchant.service.AdminMerchantService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 관리자 가맹점 & 서비스 마스터 관리(merchant.md §1·§2-1 확장). {@code SecurityConfig}가 ROLE_ADMIN만 통과시킨다. */
@RestController
@RequestMapping("/api/admin/merchants")
@RequiredArgsConstructor
public class AdminMerchantController {

    private final AdminMerchantService adminMerchantService;

    @Operation(summary = "전역 마스터 승격 대기목록",
            description = "회원이 개별 학습한 가맹점 표기 중 전역 시드로 승격 대기 중인 항목을 학습자 수 내림차순으로 반환합니다.")
    @GetMapping("/promotion-candidates")
    public ResponseEntity<ApiResponse<List<AdminMerchantPromotionCandidateResponse>>> promotionCandidates() {
        return ResponseEntity.ok(ApiResponse.success(adminMerchantService.listPromotionCandidates()));
    }

    @Operation(summary = "표기 전역 승격",
            description = "회원 학습 표기를 전역 시드(member_id=NULL)로 승격합니다. 이미 전역에 있으면 아무 일도 하지 않습니다.")
    @PostMapping("/promotion-candidates/promote")
    public ResponseEntity<ApiResponse<Void>> promote(@Valid @RequestBody MerchantAliasPromoteRequest request) {
        adminMerchantService.promote(request.aliasId(), request.aliasText());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "미식별 & 기타 가맹점 작업대",
            description = "최근 15일간 전역 alias가 없는 원본 가맹점을 발생 건수 상위로 반환합니다(결제대행사 제외).")
    @GetMapping("/unclassified")
    public ResponseEntity<ApiResponse<List<AdminUnclassifiedMerchantResponse>>> unclassified() {
        return ResponseEntity.ok(ApiResponse.success(adminMerchantService.listUnclassifiedMerchants()));
    }

    @Operation(summary = "빠른 마스터 매핑 등록", description = "미식별 가맹점을 전역 alias에 즉시 연결합니다.")
    @PostMapping("/{merchantId}/quick-alias")
    public ResponseEntity<ApiResponse<Void>> quickAlias(@PathVariable Long merchantId,
                                                          @Valid @RequestBody AdminMerchantQuickAliasRequest request) {
        adminMerchantService.quickRegisterGlobalAlias(merchantId, request.canonicalServiceName(), request.categoryId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
