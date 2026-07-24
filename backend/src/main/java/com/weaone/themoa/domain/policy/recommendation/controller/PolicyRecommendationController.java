package com.weaone.themoa.domain.policy.recommendation.controller;

import com.weaone.themoa.common.response.ApiResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.request.PolicyRecommendationProfileCreateRequest;
import com.weaone.themoa.domain.policy.recommendation.dto.request.PolicyRecommendationProfileUpdateRequest;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationListResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationProfileMutationResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationProfileResponse;
import com.weaone.themoa.domain.policy.recommendation.dto.response.PolicyRecommendationRegionOptionsResponse;
import com.weaone.themoa.domain.policy.recommendation.service.PolicyRecommendationProfileService;
import com.weaone.themoa.domain.policy.recommendation.service.PolicyRecommendationRegionService;
import com.weaone.themoa.domain.policy.recommendation.service.PolicyRecommendationService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policy-recommendations")
public class PolicyRecommendationController {
    private static final Logger log = LoggerFactory.getLogger(PolicyRecommendationController.class);
    private static final String RECOMMENDATION_REFRESH_FAILED_MESSAGE =
            "기본정보는 저장했지만 추천 정책을 계산하지 못했어요.";

    private final PolicyRecommendationProfileService profileService;
    private final PolicyRecommendationService recommendationService;
    private final PolicyRecommendationRegionService regionService;

    public PolicyRecommendationController(PolicyRecommendationProfileService profileService,
                                          PolicyRecommendationService recommendationService,
                                          PolicyRecommendationRegionService regionService) {
        this.profileService = profileService;
        this.recommendationService = recommendationService;
        this.regionService = regionService;
    }

    @GetMapping("/profile")
    public ApiResponse<PolicyRecommendationProfileResponse> profile(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        return ApiResponse.success(profileService.get(memberId));
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<PolicyRecommendationProfileMutationResponse>> createProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PolicyRecommendationProfileCreateRequest request) {
        PolicyRecommendationProfileResponse response = profileService.create(memberId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(refreshAfterProfileMutation(memberId, response)));
    }

    @PatchMapping("/profile")
    public ApiResponse<PolicyRecommendationProfileMutationResponse> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PolicyRecommendationProfileUpdateRequest request) {
        PolicyRecommendationProfileResponse response = profileService.update(memberId, request);
        return ApiResponse.success(refreshAfterProfileMutation(memberId, response));
    }

    @GetMapping
    public ApiResponse<PolicyRecommendationListResponse> recommendations(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        return ApiResponse.success(recommendationService.list(memberId));
    }

    @PostMapping("/refresh")
    public ApiResponse<PolicyRecommendationListResponse> refresh(
            @Parameter(hidden = true) @AuthenticationPrincipal Long memberId) {
        return ApiResponse.success(recommendationService.refreshForMember(memberId));
    }

    @GetMapping("/regions")
    public ApiResponse<PolicyRecommendationRegionOptionsResponse> regions() {
        return ApiResponse.success(regionService.options());
    }

    private PolicyRecommendationProfileMutationResponse refreshAfterProfileMutation(
            Long memberId,
            PolicyRecommendationProfileResponse profile) {
        try {
            PolicyRecommendationListResponse recommendations = recommendationService.refreshForMember(memberId);
            return new PolicyRecommendationProfileMutationResponse(
                    profile,
                    recommendations,
                    true,
                    null
            );
        } catch (RuntimeException ex) {
            log.warn("정책 추천 기본정보 저장 후 추천 재계산에 실패했습니다. memberId={}, errorType={}",
                    memberId, ex.getClass().getSimpleName());
            return new PolicyRecommendationProfileMutationResponse(
                    profile,
                    null,
                    false,
                    RECOMMENDATION_REFRESH_FAILED_MESSAGE
            );
        }
    }
}
