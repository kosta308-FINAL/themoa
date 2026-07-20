package com.weaone.themoa.domain.recommend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.recommend.dto.request.RecommendRequest;
import com.weaone.themoa.domain.recommend.dto.response.RecommendResponse;
import com.weaone.themoa.domain.recommend.service.RecommendQueryService;

import jakarta.validation.Valid;

/**
 * 맞춤 금융상품 추천 JSON API. (기존 GET /recommend HTML 프로토타입과 별개로, 프론트 연동용)
 * 인증 필요(SecurityConfig의 anyRequest().authenticated()) — 로그인 사용자 기준 맞춤 추천.
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendApiController {

    private final RecommendQueryService recommendQueryService;

    public RecommendApiController(RecommendQueryService recommendQueryService) {
        this.recommendQueryService = recommendQueryService;
    }

    @PostMapping
    public ApiResponse<RecommendResponse> recommend(@Valid @RequestBody RecommendRequest request) {
        return ApiResponse.success(recommendQueryService.recommend(request));
    }
}
