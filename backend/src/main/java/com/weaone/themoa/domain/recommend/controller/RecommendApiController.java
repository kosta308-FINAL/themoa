package com.weaone.themoa.domain.recommend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.recommend.dto.request.RecommendRequest;
import com.weaone.themoa.domain.recommend.dto.response.RecommendDefaultsResponse;
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
    public ApiResponse<RecommendResponse> recommend(@AuthenticationPrincipal Long memberId,
                                                    @Valid @RequestBody RecommendRequest request) {
        return ApiResponse.success(recommendQueryService.recommend(memberId, request));
    }

    /**
     * 추천 입력 폼 기본값. 화면 진입 시 호출해 월소득·월 납입가능금액을 미리 채운다.
     * 회원가입·소비내역 연동으로 이미 아는 값을 다시 묻지 않기 위한 용도다.
     */
    @GetMapping("/defaults")
    public ApiResponse<RecommendDefaultsResponse> defaults(@AuthenticationPrincipal Long memberId) {
        return ApiResponse.success(recommendQueryService.findDefaults(memberId));
    }
}
