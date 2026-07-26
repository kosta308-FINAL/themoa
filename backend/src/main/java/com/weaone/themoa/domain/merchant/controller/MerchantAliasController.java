package com.weaone.themoa.domain.merchant.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.merchant.dto.request.MerchantAliasLabelUpdateRequest;
import com.weaone.themoa.domain.merchant.dto.response.MerchantAliasResponse;
import com.weaone.themoa.domain.merchant.service.MemberMerchantLabelService;
import com.weaone.themoa.domain.merchant.service.MerchantAliasQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 가맹점 별칭 검색(view/fixedExpense.md §3.3 F-03 "가맹점 검색하거나 고르세요") + 회원 개인 표시명 설정. */
@RestController
@RequestMapping("/api/merchant-aliases")
@RequiredArgsConstructor
public class MerchantAliasController {

    private final MerchantAliasQueryService merchantAliasQueryService;
    private final MemberMerchantLabelService memberMerchantLabelService;

    @Operation(summary = "가맹점 별칭 검색", description = "고정지출 등록 폼의 가맹점 선택용 전역 alias 목록을 검색합니다. q가 없으면 이름순 상위 20건을 줍니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MerchantAliasResponse>>> search(
            @RequestParam(required = false) String q) {
        List<MerchantAliasResponse> response = merchantAliasQueryService.search(q).stream()
                .map(MerchantAliasResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "개인 표시명 설정",
            description = "이 서비스(alias)에 대해 로그인 회원 화면에서만 보일 이름을 저장합니다. 전역 이름이나 다른 회원의 설정과 무관하게 이 회원 화면에서는 항상 이 이름이 우선합니다.")
    @PutMapping("/{aliasId}/label")
    public ResponseEntity<Void> setLabel(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long aliasId,
            @Valid @RequestBody MerchantAliasLabelUpdateRequest request) {
        memberMerchantLabelService.setLabel(memberId, aliasId, request.label());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "개인 표시명 삭제", description = "설정해 둔 개인 표시명을 지우고 전역 이름으로 되돌립니다.")
    @DeleteMapping("/{aliasId}/label")
    public ResponseEntity<Void> clearLabel(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @PathVariable Long aliasId) {
        memberMerchantLabelService.clearLabel(memberId, aliasId);
        return ResponseEntity.noContent().build();
    }
}
