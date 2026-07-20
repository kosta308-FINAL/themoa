package com.weaone.themoa.domain.merchant.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.merchant.dto.response.MerchantAliasResponse;
import com.weaone.themoa.domain.merchant.service.MerchantAliasQueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 가맹점 별칭 검색(view/fixedExpense.md §3.3 F-03 "가맹점 검색하거나 고르세요"). */
@RestController
@RequestMapping("/api/merchant-aliases")
@RequiredArgsConstructor
public class MerchantAliasController {

    private final MerchantAliasQueryService merchantAliasQueryService;

    @Operation(summary = "가맹점 별칭 검색", description = "고정지출 등록 폼의 가맹점 선택용 전역 alias 목록을 검색합니다. q가 없으면 이름순 상위 20건을 줍니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MerchantAliasResponse>>> search(
            @RequestParam(required = false) String q) {
        List<MerchantAliasResponse> response = merchantAliasQueryService.search(q).stream()
                .map(MerchantAliasResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
