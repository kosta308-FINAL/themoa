package com.weaone.themoa.domain.financialsearch.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.financialsearch.dto.response.PopularProductResponse;
import com.weaone.themoa.domain.financialsearch.service.PopularFinancialProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 실시간 인기 금융상품 순위(북마크 많은 순). 로그인 사용자 기준이 아니라 전체 집계라, 추천 화면 등에서
 * "다른 사람들이 많이 담은 상품"으로 보여준다.
 */
@RestController
@RequestMapping("/api/financial-products/popular")
public class PopularFinancialProductController {

    private final PopularFinancialProductService popularService;

    public PopularFinancialProductController(PopularFinancialProductService popularService) {
        this.popularService = popularService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PopularProductResponse>>> popular(
            @RequestParam(required = false) Integer limit) {
        int size = limit == null ? popularService.defaultLimit() : limit;
        return ResponseEntity.ok(ApiResponse.success(popularService.findPopular(size)));
    }
}
